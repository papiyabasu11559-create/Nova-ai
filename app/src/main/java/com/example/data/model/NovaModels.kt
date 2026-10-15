package com.example.data.model

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sessionId: String = "default_session",
    val role: String, // "user", "model", "system"
    val content: String,
    val toolCallName: String? = null,
    val toolCallArgs: String? = null,
    val toolResult: String? = null,
    val toolStatus: String? = null, // "SUCCESS", "FAILED", "DISPATCHED", "PENDING"
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

data class ActionConfirmation(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val details: String,
    val targetAction: String,
    val payload: Map<String, String>,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

enum class ModelChoice(val id: String, val displayName: String, val description: String) {
    FLASH_35("gemini-3.5-flash", "Gemini 3.5 Flash", "High availability, ultra-fast response & native device actions"),
    FLASH("gemini-3.8-flash", "Gemini 3.8 Flash", "Preview model with real-time reasoning"),
    PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Advanced multi-step reasoning, coding & research"),
    ARGON("gemini-4-argon", "Gemini 4 Argon", "Deep thinking, multimodal vision, high complexity")
}

data class AssistantSettings(
    val customApiKey: String = "",
    val selectedModel: ModelChoice = ModelChoice.FLASH_35,
    val autoDetectedModelName: String = "",
    val availableModels: List<String> = emptyList(),
    val isKeyValidated: Boolean = false,
    val keyValidationMessage: String = "",
    val personaTone: String = "Helpful, concise, and proactive personal assistant",
    val autoSpeakResponses: Boolean = false,
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val floatingOverlayEnabled: Boolean = false,
    val customApiEndpoint: String = "",
    val bubbleSizeDp: Int = 60,
    val bubbleOpacity: Float = 0.95f,
    val bubbleGlowEnabled: Boolean = true,
    val bubbleThemeColor: String = "CYAN"
)

data class AutomationLogItem(
    val id: Long,
    val actionType: String,
    val description: String,
    val parameters: String,
    val result: String,
    val status: String,
    val timestamp: Long
)
