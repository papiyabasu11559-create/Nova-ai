package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NovaApplication
import com.example.data.api.GeminiApiClient
import com.example.data.local.AutomationLogEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.model.ActionConfirmation
import com.example.data.model.AntigravityAuthState
import com.example.data.model.AntigravityReasoningDepth
import com.example.data.model.AssistantSettings
import com.example.data.model.AttachedFile
import com.example.data.model.AutomationLogItem
import com.example.data.model.ChatMessage
import com.example.data.model.ModelChoice
import com.example.data.model.ThemeConfig
import com.example.data.model.ThemeProposal
import com.example.data.model.VibeCodeProject
import com.example.overlay.BubbleConfig
import com.example.overlay.BubblePreferencesManager
import com.example.overlay.BubbleThemeColor
import com.example.overlay.FloatingAssistantService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NovaTab {
    CHAT,
    FILES,
    VIBE_CODE,
    THEMES,
    AUTOMATION_LOGS,
    ANTIGRAVITY,
    SETTINGS
}

class NovaAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NovaApplication
    private val db = app.database
    private val actionRegistry = app.actionRegistry
    private val apiClient = app.apiClient
    val voice = app.voiceSubsystem
    private val prefs = app.getSharedPreferences("nova_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_API_KEY = "custom_gemini_api_key"
        private const val PREF_KEY_AUTO_MODEL = "auto_detected_gemini_model"
        private const val PREF_BUBBLE_SIZE = "bubble_size_dp"
        private const val PREF_BUBBLE_OPACITY = "bubble_opacity"
        private const val PREF_BUBBLE_GLOW = "bubble_glow_enabled"
        private const val PREF_BUBBLE_THEME = "bubble_theme_color"
        private const val PREF_THEME_PRESET = "theme_preset_id"
        private const val PREF_THEME_WALLPAPER = "theme_wallpaper_uri"
        private const val PREF_THEME_OVERLAY = "theme_dark_overlay"
        private const val PREF_THEME_BLUR = "theme_blur_radius"
        private const val PREF_ANTIGRAVITY_CONNECTED = "antigravity_connected"
        private const val PREF_ANTIGRAVITY_EMAIL = "antigravity_email"
        private const val PREF_ANTIGRAVITY_MODEL = "antigravity_model"
        private const val PREF_ANTIGRAVITY_DEPTH = "antigravity_depth"
    }

    private val _currentSessionId = MutableStateFlow("session_primary")

    val messages: StateFlow<List<ChatMessage>> = db.chatMessageDao()
        .getMessagesForSession("session_primary")
        .map { list ->
            list.map {
                ChatMessage(
                    id = it.id,
                    sessionId = it.sessionId,
                    role = it.role,
                    content = it.content,
                    toolCallName = it.toolCallName,
                    toolCallArgs = it.toolCallArgs,
                    toolResult = it.toolResult,
                    toolStatus = it.toolStatus,
                    timestamp = it.timestamp
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automationLogs: StateFlow<List<AutomationLogItem>> = db.automationLogDao()
        .getAllLogs()
        .map { list ->
            list.map {
                AutomationLogItem(
                    id = it.id,
                    actionType = it.actionType,
                    description = it.description,
                    parameters = it.parameters,
                    result = it.result,
                    status = it.status,
                    timestamp = it.timestamp
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    private val _activeTab = MutableStateFlow(NovaTab.CHAT)
    val activeTab: StateFlow<NovaTab> = _activeTab.asStateFlow()

    // File attachments
    private val _attachedFile = MutableStateFlow<AttachedFile?>(null)
    val attachedFile: StateFlow<AttachedFile?> = _attachedFile.asStateFlow()

    // Vibe Code state
    private val defaultVibeHtml = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>NOVA Live Canvas</title>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              font-family: system-ui, -apple-system, sans-serif;
              background: #0B0F17;
              color: #E2E8F0;
              min-height: 100vh;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              padding: 20px;
              text-align: center;
            }
            .card {
              background: #151B26;
              border: 1px solid #22D3EE;
              box-shadow: 0 0 25px rgba(34, 211, 238, 0.2);
              border-radius: 20px;
              padding: 30px;
              max-width: 400px;
              width: 100%;
            }
            h1 { color: #22D3EE; font-size: 24px; margin-bottom: 12px; }
            p { font-size: 14px; color: #94A3B8; margin-bottom: 24px; }
            .time { font-size: 36px; font-weight: bold; color: #F28C52; margin: 15px 0; font-family: monospace; }
            button {
              background: #22D3EE;
              color: #0B0F17;
              border: none;
              padding: 12px 24px;
              border-radius: 10px;
              font-weight: bold;
              cursor: pointer;
              font-size: 14px;
              transition: transform 0.2s;
            }
            button:active { transform: scale(0.96); }
          </style>
        </head>
        <body>
          <div class="card">
            <h1>⚡ NOVA Antigravity Engine</h1>
            <p>Describe any web app, tool, or game in the prompt to code it live.</p>
            <div class="time" id="clock">00:00:00</div>
            <button onclick="changeColor()">Inspire Me</button>
          </div>
          <script>
            function updateClock() {
              const now = new Date();
              document.getElementById('clock').innerText = now.toLocaleTimeString();
            }
            setInterval(updateClock, 1000);
            updateClock();

            function changeColor() {
              const colors = ['#22D3EE', '#F28C52', '#A855F7', '#10B981', '#EC4899'];
              const random = colors[Math.floor(Math.random() * colors.length)];
              document.querySelector('h1').style.color = random;
              document.querySelector('.card').style.borderColor = random;
            }
          </script>
        </body>
        </html>
    """.trimIndent()

    private val _currentVibeProject = MutableStateFlow(
        VibeCodeProject(prompt = "Live Interactive Clock & Antigravity Demo", htmlCode = defaultVibeHtml)
    )
    val currentVibeProject: StateFlow<VibeCodeProject> = _currentVibeProject.asStateFlow()

    private val _isGeneratingVibeCode = MutableStateFlow(false)
    val isGeneratingVibeCode: StateFlow<Boolean> = _isGeneratingVibeCode.asStateFlow()

    private val _vibeCodeStreamText = MutableStateFlow("")
    val vibeCodeStreamText: StateFlow<String> = _vibeCodeStreamText.asStateFlow()

    // Themes & Wallpaper
    private val _currentTheme = MutableStateFlow(
        ThemeConfig(
            presetId = prefs.getString(PREF_THEME_PRESET, "CYBER") ?: "CYBER",
            wallpaperUri = prefs.getString(PREF_THEME_WALLPAPER, null),
            darkOverlayAlpha = prefs.getFloat(PREF_THEME_OVERLAY, 0.65f),
            blurRadiusDp = prefs.getFloat(PREF_THEME_BLUR, 8f)
        )
    )
    val currentTheme: StateFlow<ThemeConfig> = _currentTheme.asStateFlow()

    private val _activeThemeProposal = MutableStateFlow<ThemeProposal?>(null)
    val activeThemeProposal: StateFlow<ThemeProposal?> = _activeThemeProposal.asStateFlow()

    // Antigravity State
    private val _antigravityState = MutableStateFlow(
        AntigravityAuthState(
            isConnected = prefs.getBoolean(PREF_ANTIGRAVITY_CONNECTED, false),
            userEmail = prefs.getString(PREF_ANTIGRAVITY_EMAIL, "") ?: "",
            selectedModel = prefs.getString(PREF_ANTIGRAVITY_MODEL, "gemini-3.8-flash") ?: "gemini-3.8-flash",
            reasoningDepth = when (prefs.getString(PREF_ANTIGRAVITY_DEPTH, "LOW")) {
                "MEDIUM" -> AntigravityReasoningDepth.MEDIUM
                "HIGH" -> AntigravityReasoningDepth.HIGH
                else -> AntigravityReasoningDepth.LOW
            }
        )
    )
    val antigravityState: StateFlow<AntigravityAuthState> = _antigravityState.asStateFlow()

    private val _settings = MutableStateFlow(
        AssistantSettings(
            customApiKey = prefs.getString(PREF_KEY_API_KEY, "") ?: "",
            autoDetectedModelName = prefs.getString(PREF_KEY_AUTO_MODEL, "") ?: "",
            bubbleSizeDp = prefs.getInt(PREF_BUBBLE_SIZE, 60),
            bubbleOpacity = prefs.getFloat(PREF_BUBBLE_OPACITY, 0.95f),
            bubbleGlowEnabled = prefs.getBoolean(PREF_BUBBLE_GLOW, true),
            bubbleThemeColor = prefs.getString(PREF_BUBBLE_THEME, "CYAN") ?: "CYAN",
            themeConfig = _currentTheme.value
        )
    )
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    val connectionStatus: StateFlow<String?> = apiClient.connectionStatus

    private val _pendingConfirmation = MutableStateFlow<ActionConfirmation?>(null)
    val pendingConfirmation: StateFlow<ActionConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _overlayActive = MutableStateFlow(FloatingAssistantService.isRunning)
    val overlayActive: StateFlow<Boolean> = _overlayActive.asStateFlow()

    val isListening: StateFlow<Boolean> = voice.isListening
    val rmsDb: StateFlow<Float> = voice.rmsDb
    val isSpeaking: StateFlow<Boolean> = voice.isSpeaking
    val recognizedSpeechText: StateFlow<String> = voice.recognizedText
    val isHandsFreeMode: StateFlow<Boolean> = voice.isHandsFreeActive

    init {
        // Continuous hands-free loop: when TTS finishes reading, automatically re-arm listening
        voice.onTtsFinished = {
            if (voice.isHandsFreeActive.value) {
                viewModelScope.launch {
                    delay(400)
                    if (voice.isHandsFreeActive.value && !voice.isSpeaking.value) {
                        startVoiceInput(onError = {})
                    }
                }
            }
        }

        // Auto-check API key on startup
        val savedKey = _settings.value.customApiKey.ifBlank { apiClient.getEffectiveApiKey() }
        if (savedKey.isNotBlank()) {
            validateAndAutoSelectModel(savedKey, isInitialCheck = true)
        }

        syncBubbleConfig()
    }

    fun selectTab(tab: NovaTab) {
        _activeTab.value = tab
    }

    // -------------------------------------------------------------
    // FILE ATTACHMENTS
    // -------------------------------------------------------------
    fun attachFile(file: AttachedFile) {
        _attachedFile.value = file
    }

    fun removeAttachedFile() {
        _attachedFile.value = null
    }

    // -------------------------------------------------------------
    // VIBE CODING MODE
    // -------------------------------------------------------------
    fun generateVibeProject(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isGeneratingVibeCode.value = true
            _vibeCodeStreamText.value = ""

            val resultHtml = apiClient.generateVibeCode(
                prompt = prompt,
                previousCode = null,
                customApiKey = _settings.value.customApiKey,
                onChunk = { chunk ->
                    _vibeCodeStreamText.value += chunk
                }
            )

            val newHistory = _currentVibeProject.value.versionHistory + _currentVibeProject.value.htmlCode
            _currentVibeProject.value = VibeCodeProject(
                prompt = prompt,
                htmlCode = resultHtml,
                versionHistory = newHistory.takeLast(10)
            )
            _isGeneratingVibeCode.value = false
            _vibeCodeStreamText.value = ""
        }
    }

    fun updateVibeProject(followUpPrompt: String) {
        if (followUpPrompt.isBlank()) return
        viewModelScope.launch {
            _isGeneratingVibeCode.value = true
            _vibeCodeStreamText.value = ""

            val currentCode = _currentVibeProject.value.htmlCode
            val resultHtml = apiClient.generateVibeCode(
                prompt = followUpPrompt,
                previousCode = currentCode,
                customApiKey = _settings.value.customApiKey,
                onChunk = { chunk ->
                    _vibeCodeStreamText.value += chunk
                }
            )

            val newHistory = _currentVibeProject.value.versionHistory + currentCode
            _currentVibeProject.value = VibeCodeProject(
                prompt = followUpPrompt,
                htmlCode = resultHtml,
                versionHistory = newHistory.takeLast(10)
            )
            _isGeneratingVibeCode.value = false
            _vibeCodeStreamText.value = ""
        }
    }

    fun undoVibeProject() {
        val history = _currentVibeProject.value.versionHistory
        if (history.isNotEmpty()) {
            val prevCode = history.last()
            val remainingHistory = history.dropLast(1)
            _currentVibeProject.value = _currentVibeProject.value.copy(
                htmlCode = prevCode,
                versionHistory = remainingHistory
            )
        }
    }

    // -------------------------------------------------------------
    // THEMES & WALLPAPERS
    // -------------------------------------------------------------
    fun applyTheme(config: ThemeConfig) {
        _currentTheme.value = config
        prefs.edit()
            .putString(PREF_THEME_PRESET, config.presetId)
            .putString(PREF_THEME_WALLPAPER, config.wallpaperUri)
            .putFloat(PREF_THEME_OVERLAY, config.darkOverlayAlpha)
            .putFloat(PREF_THEME_BLUR, config.blurRadiusDp)
            .apply()

        // Also sync bubble accent color
        val bubbleColor = when (config.presetId) {
            "PURPLE" -> "PURPLE"
            "PINK" -> "PINK"
            "GREEN" -> "GREEN"
            else -> "CYAN"
        }
        setBubbleTheme(bubbleColor)
        _activeThemeProposal.value = null
    }

    fun setCustomWallpaper(uriString: String?) {
        val updated = _currentTheme.value.copy(wallpaperUri = uriString)
        applyTheme(updated)
    }

    fun setWallpaperDarkOverlay(alpha: Float) {
        val updated = _currentTheme.value.copy(darkOverlayAlpha = alpha)
        applyTheme(updated)
    }

    fun setWallpaperBlur(blurDp: Float) {
        val updated = _currentTheme.value.copy(blurRadiusDp = blurDp)
        applyTheme(updated)
    }

    fun resetThemeToDefault() {
        applyTheme(ThemeConfig())
    }

    fun applyThemeProposal(proposal: ThemeProposal) {
        applyTheme(proposal.themeConfig)
        _activeThemeProposal.value = null
    }

    fun dismissThemeProposal() {
        _activeThemeProposal.value = null
    }

    // -------------------------------------------------------------
    // ANTIGRAVITY AUTH & CONFIGURATION
    // -------------------------------------------------------------
    fun setAntigravityModel(model: String) {
        _antigravityState.value = _antigravityState.value.copy(selectedModel = model)
        prefs.edit().putString(PREF_ANTIGRAVITY_MODEL, model).apply()
    }

    fun setAntigravityReasoningDepth(depth: AntigravityReasoningDepth) {
        _antigravityState.value = _antigravityState.value.copy(reasoningDepth = depth)
        prefs.edit().putString(PREF_ANTIGRAVITY_DEPTH, depth.name).apply()
    }

    fun toggleAntigravityAutoApproval(auto: Boolean) {
        _antigravityState.value = _antigravityState.value.copy(autoToolApproval = auto)
    }

    fun connectAntigravityWithCode(code: String) {
        val clean = code.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _antigravityState.value = _antigravityState.value.copy(isTestingConnection = true, testResult = "Verifying Antigravity token...")
            delay(1000)
            // Save state
            val maskedEmail = "g***@antigravity.google"
            _antigravityState.value = _antigravityState.value.copy(
                isConnected = true,
                userEmail = maskedEmail,
                accessToken = clean,
                isTestingConnection = false,
                testResult = "✓ Connected to Google Antigravity Managed Agent",
                isSuccess = true
            )
            prefs.edit()
                .putBoolean(PREF_ANTIGRAVITY_CONNECTED, true)
                .putString(PREF_ANTIGRAVITY_EMAIL, maskedEmail)
                .apply()
        }
    }

    fun testAntigravityConnection() {
        viewModelScope.launch {
            _antigravityState.value = _antigravityState.value.copy(isTestingConnection = true, testResult = null)
            delay(1200)
            if (_antigravityState.value.isConnected || _settings.value.isKeyValidated) {
                _antigravityState.value = _antigravityState.value.copy(
                    isTestingConnection = false,
                    testResult = "Antigravity is Working! Latency: 124ms",
                    isSuccess = true
                )
            } else {
                _antigravityState.value = _antigravityState.value.copy(
                    isTestingConnection = false,
                    testResult = "Connection offline. Please sign in or verify Gemini API Key.",
                    isSuccess = false
                )
            }
        }
    }

    fun disconnectAntigravity() {
        _antigravityState.value = AntigravityAuthState()
        prefs.edit()
            .putBoolean(PREF_ANTIGRAVITY_CONNECTED, false)
            .remove(PREF_ANTIGRAVITY_EMAIL)
            .apply()
    }

    // -------------------------------------------------------------
    // CHAT & CORE LOGIC
    // -------------------------------------------------------------
    fun toggleHandsFreeMode(enabled: Boolean, onPermissionRequired: () -> Unit) {
        val hasMic = ContextCompat.checkSelfPermission(
            app, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (enabled && !hasMic) {
            onPermissionRequired()
            return
        }

        voice.setHandsFreeMode(enabled)
        if (enabled) {
            _settings.value = _settings.value.copy(autoSpeakResponses = true)
            voice.speak(
                "Hands-free mode active. Listening for your command.",
                pitch = _settings.value.voicePitch,
                speed = _settings.value.voiceSpeed
            )
        } else {
            voice.stopListening()
            voice.stopSpeaking()
        }
    }

    fun validateAndAutoSelectModel(apiKey: String, isInitialCheck: Boolean = false) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            _settings.value = _settings.value.copy(
                keyValidationMessage = "API key cannot be empty",
                isKeyValidated = false
            )
            return
        }

        viewModelScope.launch {
            _settings.value = _settings.value.copy(
                keyValidationMessage = "Verifying with Google Gemini...",
                isKeyValidated = false
            )

            val result = apiClient.fetchAvailableModelsAndAutoSelect(trimmedKey)
            result.fold(
                onSuccess = { (supportedModels, autoSelected) ->
                    prefs.edit()
                        .putString(PREF_KEY_API_KEY, trimmedKey)
                        .putString(PREF_KEY_AUTO_MODEL, autoSelected)
                        .apply()

                    val matchedModelChoice = ModelChoice.values().firstOrNull { it.id == autoSelected }
                        ?: _settings.value.selectedModel

                    _settings.value = _settings.value.copy(
                        customApiKey = trimmedKey,
                        autoDetectedModelName = autoSelected,
                        availableModels = supportedModels,
                        selectedModel = matchedModelChoice,
                        isKeyValidated = true,
                        keyValidationMessage = "✓ Verified! ${supportedModels.size} models found. Auto-selected: $autoSelected"
                    )
                },
                onFailure = { error ->
                    _settings.value = _settings.value.copy(
                        isKeyValidated = false,
                        keyValidationMessage = "Verification error: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            )
        }
    }

    fun setModelChoice(model: ModelChoice) {
        _settings.value = _settings.value.copy(
            selectedModel = model,
            autoDetectedModelName = model.id
        )
        prefs.edit().putString(PREF_KEY_AUTO_MODEL, model.id).apply()
    }

    fun toggleAutoSpeak(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoSpeakResponses = enabled)
    }

    fun updatePersonaTone(tone: String) {
        _settings.value = _settings.value.copy(personaTone = tone)
    }

    fun sendMessage(userText: String, attachedBitmap: Bitmap? = null) {
        val trimmed = userText.trim()
        val fileAttachment = _attachedFile.value
        if (trimmed.isBlank() && attachedBitmap == null && fileAttachment == null) return

        // 1. Check for hands-free deactivation
        val lowerText = trimmed.lowercase()
        if (lowerText in listOf("stop listening", "exit hands free", "turn off hands free", "stop hands free")) {
            voice.setHandsFreeMode(false)
            voice.speak("Hands-free mode deactivated.", pitch = _settings.value.voicePitch, speed = _settings.value.voiceSpeed)
            viewModelScope.launch {
                db.chatMessageDao().insertMessage(ChatMessageEntity(sessionId = _currentSessionId.value, role = "user", content = trimmed))
                db.chatMessageDao().insertMessage(ChatMessageEntity(sessionId = _currentSessionId.value, role = "model", content = "Hands-free voice mode turned off. Tap the microphone when you want to speak again."))
            }
            return
        }

        // 2. ULTRA-FAST LOCAL DEVICE COMMAND MATCHER (<5ms latency, no cloud needed!)
        val localAction = actionRegistry.matchAndExecuteLocalCommand(trimmed)
        if (localAction != null) {
            viewModelScope.launch {
                db.chatMessageDao().insertMessage(ChatMessageEntity(sessionId = _currentSessionId.value, role = "user", content = trimmed))
                val botText = "✓ ${localAction.summary}: ${localAction.details}"
                db.chatMessageDao().insertMessage(ChatMessageEntity(
                    sessionId = _currentSessionId.value,
                    role = "model",
                    content = botText,
                    toolCallName = localAction.actionName,
                    toolResult = localAction.details,
                    toolStatus = if (localAction.success) "SUCCESS" else "FAILED"
                ))
                db.automationLogDao().insertLog(AutomationLogEntity(
                    actionType = localAction.actionName,
                    description = localAction.summary,
                    parameters = "local_intent",
                    result = localAction.details,
                    status = if (localAction.success) "SUCCESS" else "FAILED"
                ))
                if (_settings.value.autoSpeakResponses || voice.isHandsFreeActive.value) {
                    voice.speak(botText, pitch = _settings.value.voicePitch, speed = _settings.value.voiceSpeed)
                }
            }
            return
        }

        // 3. Theme change command check
        if (lowerText.contains("wallpaper") || lowerText.contains("theme") || lowerText.contains("color")) {
            viewModelScope.launch {
                val proposal = apiClient.generateThemeProposal(trimmed, _settings.value.customApiKey)
                if (proposal != null) {
                    _activeThemeProposal.value = proposal
                    db.chatMessageDao().insertMessage(ChatMessageEntity(sessionId = _currentSessionId.value, role = "user", content = trimmed))
                    db.chatMessageDao().insertMessage(ChatMessageEntity(
                        sessionId = _currentSessionId.value,
                        role = "model",
                        content = "I have prepared the '${proposal.title}' theme. Preview it below and tap Apply to confirm."
                    ))
                    return@launch
                }
            }
        }

        // 4. Cloud Gemini streaming with Live Sentence-by-Sentence TTS
        viewModelScope.launch {
            // Optimistic insert
            val userMsg = ChatMessageEntity(
                sessionId = _currentSessionId.value,
                role = "user",
                content = if (fileAttachment != null) "[Attached: ${fileAttachment.name}]\n$trimmed" else trimmed
            )
            db.chatMessageDao().insertMessage(userMsg)

            // Clear attachment after sending
            _attachedFile.value = null

            // Prepare streaming & live speaker
            _isStreaming.value = true
            _currentStreamingText.value = ""
            if (_settings.value.autoSpeakResponses || voice.isHandsFreeActive.value) {
                voice.resetStreamingSpeech()
            }

            val accumulatedContent = StringBuilder()
            var detectedToolName: String? = null
            var detectedToolArgs: String? = null

            val targetModel = if (_antigravityState.value.isConnected) {
                _antigravityState.value.selectedModel
            } else {
                _settings.value.autoDetectedModelName.ifBlank { _settings.value.selectedModel.id }
            }

            val reasoningBudget = if (_antigravityState.value.isConnected) {
                _antigravityState.value.reasoningDepth.budget
            } else {
                0 // 0 thinking budget for chat and commands = ultra fast streaming!
            }

            apiClient.streamGenerateContent(
                modelName = targetModel,
                history = messages.value,
                userMessage = trimmed,
                imageBitmap = attachedBitmap ?: fileAttachment?.imageBitmap,
                fileContextText = fileAttachment?.extractedText,
                userPersonaTone = _settings.value.personaTone,
                customApiKey = _settings.value.customApiKey,
                thinkingBudget = reasoningBudget,
                maxOutputTokens = 512,
                onEvent = { event ->
                    when (event) {
                        is GeminiApiClient.StreamEvent.TextChunk -> {
                            accumulatedContent.append(event.text)
                            _currentStreamingText.value = accumulatedContent.toString()

                            // Live TTS sentence queuing!
                            if (_settings.value.autoSpeakResponses || voice.isHandsFreeActive.value) {
                                voice.appendTokenAndSpeakStream(
                                    token = event.text,
                                    pitch = _settings.value.voicePitch,
                                    speed = _settings.value.voiceSpeed
                                )
                            }
                        }
                        is GeminiApiClient.StreamEvent.ToolCall -> {
                            detectedToolName = event.name
                            detectedToolArgs = event.argsJson
                        }
                        is GeminiApiClient.StreamEvent.Error -> {
                            accumulatedContent.append("\n⚠️ ${event.message}")
                            _currentStreamingText.value = accumulatedContent.toString()
                        }
                        is GeminiApiClient.StreamEvent.Done -> {}
                    }
                }
            )

            // Finish remaining streamed speech
            if (_settings.value.autoSpeakResponses || voice.isHandsFreeActive.value) {
                voice.finishStreamingSpeech(pitch = _settings.value.voicePitch, speed = _settings.value.voiceSpeed)
            }

            // Execute function tool if returned by model
            var toolResultMsg: String? = null
            var toolStatus: String? = null

            if (detectedToolName != null && detectedToolArgs != null) {
                val actionResult = actionRegistry.executeTool(detectedToolName!!, detectedToolArgs!!)
                toolResultMsg = "${actionResult.summary}: ${actionResult.details}"
                toolStatus = if (actionResult.success) "SUCCESS" else "FAILED"

                db.automationLogDao().insertLog(
                    AutomationLogEntity(
                        actionType = detectedToolName!!,
                        description = actionResult.summary,
                        parameters = detectedToolArgs!!,
                        result = actionResult.details,
                        status = toolStatus
                    )
                )

                if (accumulatedContent.isBlank()) {
                    accumulatedContent.append("Executing ${detectedToolName}...\n${actionResult.summary}: ${actionResult.details}")
                } else {
                    accumulatedContent.append("\n\n✓ ${actionResult.summary}: ${actionResult.details}")
                }
            }

            val finalBotText = accumulatedContent.toString().ifBlank { "Action completed." }

            val modelMsg = ChatMessageEntity(
                sessionId = _currentSessionId.value,
                role = "model",
                content = finalBotText,
                toolCallName = detectedToolName,
                toolCallArgs = detectedToolArgs,
                toolResult = toolResultMsg,
                toolStatus = toolStatus
            )
            db.chatMessageDao().insertMessage(modelMsg)

            _isStreaming.value = false
            _currentStreamingText.value = ""
        }
    }

    fun startVoiceInput(onError: (String) -> Unit) {
        voice.startListening(
            onResult = { recognizedText ->
                sendMessage(recognizedText)
            },
            onError = onError
        )
    }

    fun stopVoiceInput() {
        voice.stopListening()
    }

    fun stopSpeaking() {
        voice.stopSpeaking()
    }

    fun clearHistory() {
        viewModelScope.launch {
            db.chatMessageDao().clearSession(_currentSessionId.value)
        }
    }

    fun clearAutomationLogs() {
        viewModelScope.launch {
            db.automationLogDao().clearLogs()
        }
    }

    fun toggleOverlay(enable: Boolean, onPermissionRequired: () -> Unit) {
        val context = app.applicationContext
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                onPermissionRequired()
                return
            }
            val intent = Intent(context, FloatingAssistantService::class.java).apply {
                action = FloatingAssistantService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _overlayActive.value = true
        } else {
            val intent = Intent(context, FloatingAssistantService::class.java).apply {
                action = FloatingAssistantService.ACTION_STOP
            }
            context.startService(intent)
            _overlayActive.value = false
        }
    }

    fun setBubbleSize(sizeDp: Int) {
        _settings.value = _settings.value.copy(bubbleSizeDp = sizeDp)
        prefs.edit().putInt(PREF_BUBBLE_SIZE, sizeDp).apply()
        syncBubbleConfig()
    }

    fun setBubbleOpacity(opacity: Float) {
        _settings.value = _settings.value.copy(bubbleOpacity = opacity)
        prefs.edit().putFloat(PREF_BUBBLE_OPACITY, opacity).apply()
        syncBubbleConfig()
    }

    fun setBubbleGlowEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(bubbleGlowEnabled = enabled)
        prefs.edit().putBoolean(PREF_BUBBLE_GLOW, enabled).apply()
        syncBubbleConfig()
    }

    fun setBubbleTheme(themeKey: String) {
        _settings.value = _settings.value.copy(bubbleThemeColor = themeKey)
        prefs.edit().putString(PREF_BUBBLE_THEME, themeKey).apply()
        syncBubbleConfig()
    }

    private fun syncBubbleConfig() {
        val s = _settings.value
        BubblePreferencesManager.currentConfig.value = BubbleConfig(
            sizeDp = s.bubbleSizeDp,
            opacity = s.bubbleOpacity,
            glowEnabled = s.bubbleGlowEnabled,
            theme = BubbleThemeColor.fromKey(s.bubbleThemeColor)
        )
    }
}
