package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com"
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
            return@withContext Result.failure(Exception("API Key cannot be empty."))
        }

        val url = "$BASE_URL/v1beta/models?key=$cleanKey"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val json = JSONObject(bodyString)
                        json.optJSONObject("error")?.optString("message") ?: bodyString
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $bodyString"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val json = JSONObject(bodyString)
                val modelsArray = json.optJSONArray("models") ?: JSONArray()
                val supportedModels = mutableListOf<String>()

                for (i in 0 until modelsArray.length()) {
                    val m = modelsArray.getJSONObject(i)
                    val name = m.optString("name", "") // e.g. "models/gemini-2.5-flash"
                    val cleanName = name.removePrefix("models/")
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    var supportsGenerateContent = false
                    if (methods != null) {
                        for (j in 0 until methods.length()) {
                            if (methods.getString(j) == "generateContent") {
                                supportsGenerateContent = true
                                break
                            }
                        }
                    }
                    if (supportsGenerateContent) {
                        supportedModels.add(cleanName)
                    }
                }

                if (supportedModels.isEmpty()) {
                    return@withContext Result.failure(Exception("Key verified, but no generateContent models are enabled on this project."))
                }

                // Automatic smart model selection:
                // Prioritize best flash models with high availability and speed
                val priorityOrder = listOf(
                    "gemini-3.5-flash",
                    "gemini-3.8-flash",
                    "gemini-flash-latest",
                    "gemini-2.5-flash",
                    "gemini-3.1-flash-lite-preview",
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
            You are NOVA AI, a premier native Android 16 personal assistant running on the user's phone.
            Persona: $userPersonaTone.
            You have real native tools for full hands-free device control:
            - open_app: Launch any installed app by name (e.g. YouTube, WhatsApp, Camera, Settings, Chrome, Clock).
            - set_timer: Set a timer with duration in seconds and label.
            - set_alarm: Set an alarm with hour (0-23) and minute (0-59).
            - create_calendar_event: Add an event to Google Calendar with title and time.
            - draft_sms: Compose an SMS to a phone number.
            - dial_phone: Open dialer for a phone number.
            - get_battery_info: Query current phone battery level and charging state.
            - launch_navigation: Open Google Maps directions for a destination.
            - web_search: Search Google or open an online URL.
            - share_text: Share text via Android system share sheet.
            - toggle_flashlight: Turn phone flashlight/torch on or off (enabled: boolean).
            - adjust_volume: Raise, lower, or mute audio volume (direction: raise/lower/mute/unmute, stream: music/ring/alarm).
            - open_settings_page: Open Android system settings (setting_type: wifi, bluetooth, display, sound, battery, apps, location).
            - send_email: Compose an email with recipient, subject, and body.
            - play_music: Search and play music/videos on media players or YouTube.
            - get_device_specs: Query phone hardware specs, available RAM, free storage, and OS version.
            - copy_to_clipboard: Copy text to system clipboard.
            - take_photo: Open camera to capture a photo.

            Rule: When the user asks you to perform an action, invoke the appropriate function call tool immediately without hesitation.
            Speak naturally, concisely, and confirm the execution.
        """.trimIndent()

        return JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", instructionText) })
            })
        }
    }

    private fun buildToolsDeclaration(): JSONArray {
        val functionDeclarations = JSONArray()

        // 1. open_app
        functionDeclarations.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Open an installed application on the user's Android phone by name or package")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the application, e.g. Camera, YouTube, Settings, WhatsApp, Clock, Calculator")
                    })
                })
                put("required", JSONArray().apply { put("app_name") })
            })
        })

        // 2. set_timer
        functionDeclarations.put(JSONObject().apply {
            put("name", "set_timer")
            put("description", "Set a countdown timer on the Android device")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("duration_seconds", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Duration of timer in seconds")
                    })
                    put("label", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Timer label or reason")
                    })
                })
                put("required", JSONArray().apply { put("duration_seconds") })
            })
        })

        // 3. set_alarm
        functionDeclarations.put(JSONObject().apply {
            put("name", "set_alarm")
            put("description", "Set an alarm for a specific hour and minute")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Hour in 24-hour format (0-23)")
                    })
                    put("minute", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minute (0-59)")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Alarm message or label")
                    })
                })
                put("required", JSONArray().apply {
                    put("hour")
                    put("minute")
                })
            })
        })

        // 4. create_calendar_event
        functionDeclarations.put(JSONObject().apply {
            put("name", "create_calendar_event")
            put("description", "Schedule a calendar event in the user's Android calendar")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title of the calendar event")
                    })
                    put("minutes_from_now", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "When the event starts, expressed in minutes from now")
                    })
                    put("duration_minutes", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Duration of the event in minutes")
                    })
                    put("description", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Description or details of event")
                    })
                })
                put("required", JSONArray().apply { put("title") })
            })
        })

        // 5. draft_sms
        functionDeclarations.put(JSONObject().apply {
            put("name", "draft_sms")
            put("description", "Prepare an SMS message in the default messaging app for user review")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phone_number", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Recipient phone number")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Body text of the message")
                    })
                })
                put("required", JSONArray().apply {
                    put("phone_number")
                    put("message")
                })
            })
        })

        // 6. dial_phone
        functionDeclarations.put(JSONObject().apply {
            put("name", "dial_phone")
            put("description", "Open phone dialer with specified number for call initiation")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phone_number", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Phone number to dial")
                    })
                })
                put("required", JSONArray().apply { put("phone_number") })
            })
        })

        // 7. get_battery_info
        functionDeclarations.put(JSONObject().apply {
            put("name", "get_battery_info")
            put("description", "Query current device battery percentage and charging status")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 8. launch_navigation
        functionDeclarations.put(JSONObject().apply {
            put("name", "launch_navigation")
            put("description", "Open turn-by-turn navigation in Google Maps or default maps app")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("destination", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Destination address or place name")
                    })
                })
                put("required", JSONArray().apply { put("destination") })
            })
        })

        // 9. web_search
        functionDeclarations.put(JSONObject().apply {
            put("name", "web_search")
            put("description", "Search Google or navigate to a web URL")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search keywords or URL")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 10. share_text
        functionDeclarations.put(JSONObject().apply {
            put("name", "share_text")
            put("description", "Share text content through Android system share sheet")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("text", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Content to share")
                    })
                })
                put("required", JSONArray().apply { put("text") })
            })
        })

        // 11. toggle_flashlight
        functionDeclarations.put(JSONObject().apply {
            put("name", "toggle_flashlight")
            put("description", "Turn the device flashlight / torch on or off")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("enabled", JSONObject().apply {
                        put("type", "BOOLEAN")
                        put("description", "True to turn on flashlight, False to turn off")
                    })
                })
                put("required", JSONArray().apply { put("enabled") })
            })
        })

        // 12. adjust_volume
        functionDeclarations.put(JSONObject().apply {
            put("name", "adjust_volume")
            put("description", "Adjust device volume or mute/unmute")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'raise', 'lower', 'mute', or 'unmute'")
                    })
                    put("stream", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'music', 'ring', 'alarm', or 'notification'")
                    })
                })
            })
        })

        // 13. open_settings_page
        functionDeclarations.put(JSONObject().apply {
            put("name", "open_settings_page")
            put("description", "Navigate to Android settings page")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("setting_type", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Type of setting: 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'apps', or 'location'")
                    })
                })
            })
        })

        // 14. send_email
        functionDeclarations.put(JSONObject().apply {
            put("name", "send_email")
            put("description", "Compose an email with recipient, subject, and message")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("recipient", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Email address of recipient")
                    })
                    put("subject", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Subject line")
                    })
                    put("body", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Email body content")
                    })
                })
                put("required", JSONArray().apply {
                    put("recipient")
                })
            })
        })

        // 15. play_music
        functionDeclarations.put(JSONObject().apply {
            put("name", "play_music")
            put("description", "Search and play music, songs, or video")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Song title, artist, playlist, or video query")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 16. get_device_specs
        functionDeclarations.put(JSONObject().apply {
            put("name", "get_device_specs")
            put("description", "Query device hardware specs, available RAM, free storage, and OS version")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 17. copy_to_clipboard
        functionDeclarations.put(JSONObject().apply {
            put("name", "copy_to_clipboard")
            put("description", "Copy text to Android system clipboard")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("text", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Text to copy")
                    })
                })
                put("required", JSONArray().apply { put("text") })
            })
        })

        // 18. take_photo
        functionDeclarations.put(JSONObject().apply {
            put("name", "take_photo")
            put("description", "Open camera to capture a photo")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        return JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", functionDeclarations)
            })
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
        userPersonaTone: String = "Helpful and concise",
        customApiKey: String? = null,
        onEvent: (StreamEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        _isGenerating.value = true
        try {
            val apiKey = getEffectiveApiKey(customApiKey)
            if (apiKey.isBlank()) {
                onEvent(StreamEvent.Error(
                    "Google Gemini API key is not configured. Please paste your API key in Settings -> Google API Key, or configure it via the Secrets panel in AI Studio."
                ))
                return@withContext
            }

        val requestJson = JSONObject()

        // Contents (Multi-turn history)
        val contentsArray = JSONArray()
        val recentHistory = history.takeLast(12)
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

        // Current turn
        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        val currentParts = JSONArray()
        currentParts.put(JSONObject().apply { put("text", userMessage) })

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

        val generationConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("topP", 0.95)
        }
        requestJson.put("generationConfig", generationConfig)

        // Resilient candidate list for automatic failover when Google server experiences 503 high demand
        val candidateModels = when (modelName) {
            "gemini-3.8-flash" -> listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-flash-latest")
            "gemini-3.5-flash" -> listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.8-flash")
            else -> listOf(modelName, "gemini-3.5-flash")
        }

        var lastError: String? = null

        for (candidate in candidateModels) {
            val isFallback = candidate != modelName
            val targetUrl = "$BASE_URL/v1beta/models/$candidate:streamGenerateContent?alt=sse&key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(targetUrl)
                .post(requestBody)
                .build()

            var attempt = 0
            val maxAttempts = if (isFallback) 1 else 2

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

                        // Check for temporary high demand (503) or rate limits (429)
                        if (statusCode == 503 || statusCode == 429) {
                            Log.w(TAG, "Model $candidate returned $statusCode (attempt $attempt): $parsedError")
                            lastError = "Google AI Servers are experiencing high temporary traffic on $candidate (503). Retrying..."
                            if (attempt < maxAttempts) {
                                delay(1200)
                                continue
                            } else {
                                break
                            }
                        } else {
                            onEvent(StreamEvent.Error("API Error ($statusCode): $parsedError"))
                            return@withContext
                        }
                    }

                    // Successful stream connection!
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

                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                if (part.has("text")) {
                                    val text = part.optString("text", "")
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
                        delay(1000)
                    }
                }
            }
        }

        // If all candidates failed:
        onEvent(StreamEvent.Error(
            lastError ?: "Google Gemini servers are currently experiencing global high demand (503). Please tap retry in a moment."
        ))
        } finally {
            _isGenerating.value = false
        }
    }
}
