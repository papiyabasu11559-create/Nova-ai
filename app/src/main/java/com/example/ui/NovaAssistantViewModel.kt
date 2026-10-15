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
import com.example.data.model.AssistantSettings
import com.example.data.model.AutomationLogItem
import com.example.data.model.ChatMessage
import com.example.data.model.ModelChoice
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
    AUTOMATION_LOGS,
    SETTINGS
}

class NovaAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NovaApplication
    private val db = app.database
    private val actionRegistry = app.actionRegistry
    private val apiClient = app.apiClient
    private val voice = app.voiceSubsystem
    private val prefs = app.getSharedPreferences("nova_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_API_KEY = "custom_gemini_api_key"
        private const val PREF_KEY_AUTO_MODEL = "auto_detected_gemini_model"
        private const val PREF_BUBBLE_SIZE = "bubble_size_dp"
        private const val PREF_BUBBLE_OPACITY = "bubble_opacity"
        private const val PREF_BUBBLE_GLOW = "bubble_glow_enabled"
        private const val PREF_BUBBLE_THEME = "bubble_theme_color"
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

    private val _settings = MutableStateFlow(
        AssistantSettings(
            customApiKey = prefs.getString(PREF_KEY_API_KEY, "") ?: "",
            autoDetectedModelName = prefs.getString(PREF_KEY_AUTO_MODEL, "") ?: "",
            bubbleSizeDp = prefs.getInt(PREF_BUBBLE_SIZE, 60),
            bubbleOpacity = prefs.getFloat(PREF_BUBBLE_OPACITY, 0.95f),
            bubbleGlowEnabled = prefs.getBoolean(PREF_BUBBLE_GLOW, true),
            bubbleThemeColor = prefs.getString(PREF_BUBBLE_THEME, "CYAN") ?: "CYAN"
        )
    )
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

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
        // When TTS finishes reading the response, if hands-free mode is on, automatically re-arm listening!
        voice.onTtsFinished = {
            if (voice.isHandsFreeActive.value) {
                viewModelScope.launch {
                    delay(500)
                    if (voice.isHandsFreeActive.value && !voice.isSpeaking.value) {
                        startVoiceInput(onError = {})
                    }
                }
            }
        }

        // Auto-check key and model on initialization if a key exists
        val savedKey = _settings.value.customApiKey.ifBlank { apiClient.getEffectiveApiKey() }
        if (savedKey.isNotBlank()) {
            validateAndAutoSelectModel(savedKey, isInitialCheck = true)
        }

        syncBubbleConfig()
    }

    fun selectTab(tab: NovaTab) {
        _activeTab.value = tab
    }

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
            // Enable auto speak so AI converses hands-free
            _settings.value = _settings.value.copy(autoSpeakResponses = true)
            voice.speak(
                "Hands-free mode active. Say any command and I will do the work.",
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
                    // Persist key and model
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
        if (trimmed.isBlank() && attachedBitmap == null) return

        // Check for hands-free stop command
        val lowerText = trimmed.lowercase()
        if (lowerText == "stop listening" || lowerText == "exit hands free" || lowerText == "turn off hands free" || lowerText == "stop hands free") {
            voice.setHandsFreeMode(false)
            voice.speak("Hands-free mode turned off.", pitch = _settings.value.voicePitch, speed = _settings.value.voiceSpeed)
            viewModelScope.launch {
                val userMsg = ChatMessageEntity(
                    sessionId = _currentSessionId.value,
                    role = "user",
                    content = trimmed
                )
                db.chatMessageDao().insertMessage(userMsg)
                val modelMsg = ChatMessageEntity(
                    sessionId = _currentSessionId.value,
                    role = "model",
                    content = "Hands-free voice mode deactivated. Tap the microphone when you want to speak again."
                )
                db.chatMessageDao().insertMessage(modelMsg)
            }
            return
        }

        viewModelScope.launch {
            // Save user message to database
            val userMsg = ChatMessageEntity(
                sessionId = _currentSessionId.value,
                role = "user",
                content = trimmed
            )
            db.chatMessageDao().insertMessage(userMsg)

            // Prepare streaming state
            _isStreaming.value = true
            _currentStreamingText.value = ""

            val accumulatedContent = StringBuilder()
            var detectedToolName: String? = null
            var detectedToolArgs: String? = null

            val targetModel = _settings.value.autoDetectedModelName.ifBlank {
                _settings.value.selectedModel.id
            }

            apiClient.streamGenerateContent(
                modelName = targetModel,
                history = messages.value,
                userMessage = trimmed,
                imageBitmap = attachedBitmap,
                userPersonaTone = _settings.value.personaTone,
                customApiKey = _settings.value.customApiKey,
                onEvent = { event ->
                    when (event) {
                        is GeminiApiClient.StreamEvent.TextChunk -> {
                            accumulatedContent.append(event.text)
                            _currentStreamingText.value = accumulatedContent.toString()
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

            // If a tool was requested by the model, execute it natively
            var toolResultMsg: String? = null
            var toolStatus: String? = null

            if (detectedToolName != null && detectedToolArgs != null) {
                val actionResult = actionRegistry.executeTool(detectedToolName!!, detectedToolArgs!!)
                toolResultMsg = "${actionResult.summary}: ${actionResult.details}"
                toolStatus = if (actionResult.success) "SUCCESS" else "FAILED"

                // Log to Automation Log
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

            // Save assistant reply to database
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

            // Speak response if auto-speak is enabled or hands-free is active
            if (_settings.value.autoSpeakResponses || voice.isHandsFreeActive.value) {
                voice.speak(
                    text = finalBotText.replace(Regex("[#*`_-]"), ""),
                    pitch = _settings.value.voicePitch,
                    speed = _settings.value.voiceSpeed
                )
            }
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
