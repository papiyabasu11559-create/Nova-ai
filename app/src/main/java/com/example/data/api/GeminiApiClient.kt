package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ThemeConfig
import com.example.data.model.ThemeProposal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val context: Context) {

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com"
    }

    init {
        warmUpConnection()
    }

    fun warmUpConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = getEffectiveApiKey()
                if (apiKey.isNotBlank()) {
                    val testUrl = "$BASE_URL/v1beta/models?key=$apiKey"
                    val request = Request.Builder().url(testUrl).head().build()
                    client.newCall(request).execute().close()
                }
            } catch (e: Exception) {
                // Ignore silent warm-up exceptions
            }
        }
    }

    fun getEffectiveApiKey(customKey: String? = null): String {
        val trimmedCustom = customKey?.trim() ?: ""
        if (trimmedCustom.isNotBlank()) {
            return trimmedCustom
        }
        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                buildConfigKey
            } else {
                ""
            }
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Queries Google's Gemini models API using the provided key,
     * verifies key authenticity, returns the list of available models,
     * and automatically selects the optimal Gemini model for the key.
     */
    suspend fun fetchAvailableModelsAndAutoSelect(apiKey: String): Result<Pair<List<String>, String>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("API key is blank"))
        }

        val url = "$BASE_URL/v1beta/models?key=$cleanKey"
        val request = Request.Builder().url(url).get().build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val json = JSONObject(body)
                    json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $body"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(body)
            val modelsArray = json.optJSONArray("models") ?: JSONArray()
            val supportedModels = mutableListOf<String>()

            for (i in 0 until modelsArray.length()) {
                val m = modelsArray.getJSONObject(i)
                val rawName = m.optString("name", "")
                val cleanName = rawName.removePrefix("models/")
                val supportedMethods = m.optJSONArray("supportedGenerationMethods")
                var canGenerate = false
                if (supportedMethods != null) {
                    for (j in 0 until supportedMethods.length()) {
                        if (supportedMethods.getString(j) == "generateContent") {
                            canGenerate = true
                            break
                        }
                    }
                }
                if (canGenerate) {
                    supportedModels.add(cleanName)
                }
            }

            if (supportedModels.isEmpty()) {
                Result.failure(Exception("No models with generateContent capability found for this key."))
            } else {
                val priorityOrder = listOf(
                    "gemini-3.5-flash",
                    "gemini-2.5-flash",
                    "gemini-3.8-flash",
                    "gemini-3.1-pro-preview"
                )

                var autoSelected: String? = null
                for (p in priorityOrder) {
                    if (supportedModels.contains(p)) {
                        autoSelected = p
                        break
                    }
                }

                if (autoSelected == null) {
                    autoSelected = supportedModels.firstOrNull { it.contains("flash", ignoreCase = true) }
                        ?: supportedModels.first()
                }

                Result.success(Pair(supportedModels, autoSelected))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating API key", e)
            Result.failure(Exception("Network error validating key: ${e.localizedMessage ?: "Connection failure"}"))
        }
    }

    private fun buildSystemInstruction(userPersonaTone: String): JSONObject {
        val instructionText = """
            You are NOVA AI, a ultra-fast native Android personal assistant.
            Persona: $userPersonaTone.
            Keep all answers concise, direct, and actionable.
            Available native device tools:
            - open_app(app_name): Launch installed app.
            - set_timer(duration_seconds, label): Set timer.
            - set_alarm(hour, minute, message): Set alarm.
            - create_calendar_event(title, minutes_from_now, duration_minutes, description).
            - draft_sms(phone_number, message).
            - dial_phone(phone_number).
            - get_battery_info().
            - launch_navigation(destination).
            - web_search(query).
            - toggle_flashlight(enabled).
            - adjust_volume(direction, stream).
            - open_settings_page(setting_type).
            - send_email(to, subject, body).
            - play_music(query).
            - get_device_specs().
            - copy_to_clipboard(text).
            - take_photo().

            Rule: When an action is requested, trigger the function tool immediately. Speak concisely without fluff.
        """.trimIndent()

        return JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", instructionText) })
            })
        }
    }

    private fun buildToolsDeclaration(): JSONArray {
        val functionDeclarations = JSONArray()

        functionDeclarations.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Launch any installed application on the user's phone")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app (e.g. YouTube, WhatsApp, Camera, Settings, Chrome)")
                    })
                })
                put("required", JSONArray().apply { put("app_name") })
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "set_timer")
            put("description", "Set a countdown timer on the user's phone")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("duration_seconds", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Duration in seconds (e.g. 300 for 5 minutes)")
                    })
                    put("label", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Label for the timer")
                    })
                })
                put("required", JSONArray().apply { put("duration_seconds") })
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "set_alarm")
            put("description", "Set an alarm for a specific time")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Hour of day (0-23)")
                    })
                    put("minute", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minute of hour (0-59)")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Label or message for the alarm")
                    })
                })
                put("required", JSONArray().apply {
                    put("hour")
                    put("minute")
                })
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "get_battery_info")
            put("description", "Get the phone's battery level and charging state")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "toggle_flashlight")
            put("description", "Turn the phone's rear torch / flashlight on or off")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("enabled", JSONObject().apply {
                        put("type", "BOOLEAN")
                        put("description", "true to turn on, false to turn off")
                    })
                })
                put("required", JSONArray().apply { put("enabled") })
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "adjust_volume")
            put("description", "Adjust volume or mute device sound")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "raise, lower, mute, max")
                    })
                })
                put("required", JSONArray().apply { put("direction") })
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "open_settings_page")
            put("description", "Open Android system settings")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("setting_type", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "wifi, bluetooth, sound, display, battery, main")
                    })
                })
                put("required", JSONArray().apply { put("setting_type") })
            })
        })

        functionDeclarations.put(JSONObject().apply {
            put("name", "web_search")
            put("description", "Search online query")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        return JSONArray().apply {
            put(JSONObject().apply { put("functionDeclarations", functionDeclarations) })
        }
    }

    sealed class StreamEvent {
        data class TextChunk(val text: String) : StreamEvent()
        data class ToolCall(val name: String, val argsJson: String) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
        data object Done : StreamEvent()
    }

    suspend fun streamGenerateContent(
        modelName: String,
        history: List<ChatMessage>,
        userMessage: String,
        imageBitmap: Bitmap? = null,
        fileContextText: String? = null,
        userPersonaTone: String = "Helpful and concise",
        customApiKey: String? = null,
        thinkingBudget: Int = 0,
        maxOutputTokens: Int = 512,
        onEvent: (StreamEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        _isGenerating.value = true
        _connectionStatus.value = null
        try {
            val apiKey = getEffectiveApiKey(customApiKey)
            if (apiKey.isBlank()) {
                onEvent(StreamEvent.Error(
                    "Google Gemini API key is not configured. Please paste your API key in Settings -> Google API Key, or configure it via the Secrets panel in AI Studio."
                ))
                return@withContext
            }

            val requestJson = JSONObject()

            // Contents: send only last 10 messages for low latency & fast token transfer
            val contentsArray = JSONArray()
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                if (msg.role == "user" || msg.role == "model") {
                    val turnObj = JSONObject()
                    turnObj.put("role", msg.role)
                    val partsArray = JSONArray()
                    val partObj = JSONObject()
                    partObj.put("text", msg.content)
                    partsArray.put(partObj)
                    turnObj.put("parts", partsArray)
                    contentsArray.put(turnObj)
                }
            }

            // Current turn (including file content if provided)
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            val combinedUserText = if (!fileContextText.isNullOrBlank()) {
                "--- ATTACHED FILE CONTEXT ---\n$fileContextText\n--- USER QUERY ---\n$userMessage"
            } else {
                userMessage
            }
            currentParts.put(JSONObject().apply { put("text", combinedUserText) })

            if (imageBitmap != null) {
                try {
                    val outputStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    currentParts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Data)
                        })
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error encoding image", e)
                }
            }
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            requestJson.put("contents", contentsArray)
            requestJson.put("systemInstruction", buildSystemInstruction(userPersonaTone))
            requestJson.put("tools", buildToolsDeclaration())

            // Generation config with thinkingBudget (0 for zero latency on commands/chat)
            val generationConfig = JSONObject().apply {
                put("temperature", 0.6)
                put("topP", 0.95)
                put("maxOutputTokens", maxOutputTokens)
                if (thinkingBudget >= 0) {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingBudget", thinkingBudget)
                    })
                }
            }
            requestJson.put("generationConfig", generationConfig)

            // Resilient candidate list with fastest flash models prioritized
            val candidateModels = when (modelName) {
                "gemini-3.5-flash" -> listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-3.8-flash")
                "gemini-2.5-flash" -> listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.8-flash")
                "gemini-3.8-flash" -> listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-2.5-flash")
                else -> listOf(modelName, "gemini-3.5-flash", "gemini-2.5-flash")
            }

            var lastError: String? = null

            for (candidate in candidateModels) {
                val isFallback = candidate != modelName
                if (isFallback) {
                    _connectionStatus.value = "Switching to fast fallback: $candidate"
                }

                val targetUrl = "$BASE_URL/v1beta/models/$candidate:streamGenerateContent?alt=sse&key=$apiKey"
                val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(targetUrl)
                    .post(requestBody)
                    .build()

                var attempt = 0
                val maxAttempts = if (isFallback) 1 else 3

                while (attempt < maxAttempts) {
                    attempt++
                    try {
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) {
                            val statusCode = response.code
                            val errorBody = response.body?.string() ?: ""
                            response.close()

                            val parsedError = try {
                                val errObj = JSONObject(errorBody)
                                errObj.optJSONObject("error")?.optString("message") ?: errorBody
                            } catch (e: Exception) {
                                errorBody
                            }

                            // 503 or 429: exponential backoff (0.5s, 1s, 2s)
                            if (statusCode == 503 || statusCode == 429) {
                                val backoffMs = when (attempt) {
                                    1 -> 500L
                                    2 -> 1000L
                                    else -> 2000L
                                }
                                _connectionStatus.value = "High server demand on $candidate (503). Retrying in ${backoffMs}ms..."
                                delay(backoffMs)
                                continue
                            } else {
                                onEvent(StreamEvent.Error("API Error ($statusCode): $parsedError"))
                                return@withContext
                            }
                        }

                        _connectionStatus.value = null

                        val body = response.body ?: run {
                            response.close()
                            onEvent(StreamEvent.Error("Empty server response"))
                            return@withContext
                        }

                        val reader = BufferedReader(InputStreamReader(body.byteStream()))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line?.trim() ?: continue
                            if (!currentLine.startsWith("data:")) continue
                            val payload = currentLine.removePrefix("data:").trim()
                            if (payload.isEmpty() || payload == "[DONE]") continue

                            try {
                                val chunkObj = JSONObject(payload)
                                val candidates = chunkObj.optJSONArray("candidates") ?: continue
                                if (candidates.length() == 0) continue
                                val firstCand = candidates.getJSONObject(0)
                                val contentObj = firstCand.optJSONObject("content") ?: continue
                                val parts = contentObj.optJSONArray("parts") ?: continue

                                for (p in 0 until parts.length()) {
                                    val part = parts.getJSONObject(p)
                                    if (part.has("text")) {
                                        val text = part.getString("text")
                                        if (text.isNotEmpty()) {
                                            onEvent(StreamEvent.TextChunk(text))
                                        }
                                    }
                                    if (part.has("functionCall")) {
                                        val functionCall = part.getJSONObject("functionCall")
                                        val toolName = functionCall.optString("name")
                                        val argsObj = functionCall.optJSONObject("args") ?: JSONObject()
                                        onEvent(StreamEvent.ToolCall(toolName, argsObj.toString()))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing SSE chunk: ${e.message}")
                            }
                        }
                        response.close()
                        onEvent(StreamEvent.Done)
                        return@withContext

                    } catch (e: Exception) {
                        Log.e(TAG, "Connection exception for model $candidate", e)
                        lastError = "Connection error: ${e.localizedMessage ?: "Network failure"}"
                        if (attempt < maxAttempts) {
                            delay(500L * attempt)
                        }
                    }
                }
            }

            onEvent(StreamEvent.Error(
                lastError ?: "Google Gemini servers are currently experiencing global demand. Please try again."
            ))
        } finally {
            _isGenerating.value = false
            _connectionStatus.value = null
        }
    }

    /**
     * Vibe Code Generation: Produces clean, self-contained, responsive HTML/CSS/JS code
     * that runs immediately in an Android WebView sandbox.
     */
    suspend fun generateVibeCode(
        prompt: String,
        previousCode: String? = null,
        customApiKey: String? = null,
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext "<!-- Error: Please configure your Gemini API Key in Settings -->"
        }

        val systemInstruction = """
            You are Antigravity Vibe Coder, an expert frontend engineer.
            Your task is to generate complete, single-file, beautifully styled, responsive HTML/CSS/JavaScript applications.
            RULES:
            1. Output ONLY the raw HTML code starting with <!DOCTYPE html> and ending with </html>.
            2. Do NOT wrap the output in markdown codeblocks (no ```html, no ```).
            3. Include all CSS inside <style> tags and all JavaScript inside <script> tags.
            4. Make the design modern, interactive, vibrant, responsive, and mobile-friendly.
            5. If previous code is provided, update it seamlessly according to the user's instructions.
        """.trimIndent()

        val contentsArray = JSONArray()
        if (!previousCode.isNullOrBlank()) {
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", "Base template:\n$previousCode") }) })
            })
            contentsArray.put(JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", previousCode) }) })
            })
        }

        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply { put(JSONObject().apply { put("text", prompt) }) })
        })

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemInstruction) }) })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 8192)
            })
        }

        val targetUrl = "$BASE_URL/v1beta/models/gemini-3.5-flash:streamGenerateContent?alt=sse&key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(targetUrl).post(requestBody).build()

        val fullCode = StringBuilder()
        try {
            val response = client.newCall(request).execute()
            val body = response.body ?: return@withContext "<!-- No response from server -->"
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (!currentLine.startsWith("data:")) continue
                val payload = currentLine.removePrefix("data:").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue

                val chunkObj = JSONObject(payload)
                val candidates = chunkObj.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue
                val contentObj = candidates.getJSONObject(0).optJSONObject("content") ?: continue
                val parts = contentObj.optJSONArray("parts") ?: continue
                for (p in 0 until parts.length()) {
                    val part = parts.getJSONObject(p)
                    if (part.has("text")) {
                        val text = part.getString("text")
                        fullCode.append(text)
                        onChunk(text)
                    }
                }
            }
            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in Vibe Code generation", e)
        }

        var result = fullCode.toString().trim()
        if (result.startsWith("```html")) result = result.removePrefix("```html").trim()
        if (result.startsWith("```")) result = result.removePrefix("```").trim()
        if (result.endsWith("```")) result = result.removeSuffix("```").trim()
        return@withContext result
    }

    /**
     * Theme generation helper from natural language descriptions
     */
    suspend fun generateThemeProposal(prompt: String, customApiKey: String? = null): ThemeProposal? = withContext(Dispatchers.IO) {
        val lower = prompt.lowercase()
        return@withContext when {
            lower.contains("purple") || lower.contains("violet") -> {
                ThemeProposal(
                    title = "Galactic Violet",
                    description = "Deep violet glass with electric purple glow",
                    themeConfig = ThemeConfig(
                        presetId = "PURPLE",
                        name = "Galactic Violet",
                        primaryColorHex = "#A855F7",
                        secondaryColorHex = "#3B82F6",
                        accentColorHex = "#7C3AED",
                        darkOverlayAlpha = 0.60f
                    )
                )
            }
            lower.contains("orange") || lower.contains("sunset") || lower.contains("antigravity") || lower.contains("fire") -> {
                ThemeProposal(
                    title = "Antigravity Flame",
                    description = "Official Antigravity Orange and Solar Crimson",
                    themeConfig = ThemeConfig(
                        presetId = "ORANGE",
                        name = "Antigravity Flame",
                        primaryColorHex = "#F28C52",
                        secondaryColorHex = "#EF4444",
                        accentColorHex = "#F97316",
                        darkOverlayAlpha = 0.65f
                    )
                )
            }
            lower.contains("green") || lower.contains("matrix") || lower.contains("emerald") -> {
                ThemeProposal(
                    title = "Emerald Matrix",
                    description = "Terminal green and neon cyan accents",
                    themeConfig = ThemeConfig(
                        presetId = "GREEN",
                        name = "Emerald Matrix",
                        primaryColorHex = "#10B981",
                        secondaryColorHex = "#06B6D4",
                        accentColorHex = "#10B981",
                        darkOverlayAlpha = 0.70f
                    )
                )
            }
            lower.contains("pink") || lower.contains("rose") || lower.contains("cyberpunk") -> {
                ThemeProposal(
                    title = "Neon Pink",
                    description = "Vibrant magenta and violet neon glow",
                    themeConfig = ThemeConfig(
                        presetId = "PINK",
                        name = "Neon Pink",
                        primaryColorHex = "#EC4899",
                        secondaryColorHex = "#8B5CF6",
                        accentColorHex = "#F43F5E",
                        darkOverlayAlpha = 0.65f
                    )
                )
            }
            else -> {
                ThemeProposal(
                    title = "Cyber Neon",
                    description = "Electric cyan with obsidian contrast",
                    themeConfig = ThemeConfig(
                        presetId = "CYBER",
                        name = "Cyber Neon",
                        primaryColorHex = "#22D3EE",
                        secondaryColorHex = "#7C3AED",
                        accentColorHex = "#00F2FE",
                        darkOverlayAlpha = 0.65f
                    )
                )
            }
        }
    }
}
