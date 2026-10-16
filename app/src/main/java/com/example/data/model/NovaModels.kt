package com.example.data.model

import android.graphics.Bitmap
import java.util.UUID

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
    val isStreaming: Boolean = false,
    val attachedFileName: String? = null,
    val themeProposal: ThemeProposal? = null
)

data class AttachedFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val extractedText: String? = null,
    val imageBitmap: Bitmap? = null
) {
    val readableSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format("%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
        }
}

data class ThemeConfig(
    val presetId: String = "CYBER",
    val name: String = "Electric Cyber",
    val primaryColorHex: String = "#22D3EE",
    val secondaryColorHex: String = "#7C3AED",
    val accentColorHex: String = "#00F2FE",
    val wallpaperUri: String? = null,
    val darkOverlayAlpha: Float = 0.65f,
    val blurRadiusDp: Float = 8f
)

data class ThemeProposal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val themeConfig: ThemeConfig
)

data class VibeCodeProject(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val htmlCode: String,
    val versionHistory: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class AntigravityReasoningDepth(val displayName: String, val budget: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 2048),
    HIGH("High", 8192)
}

data class AntigravityAuthState(
    val isConnected: Boolean = false,
    val userEmail: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val selectedModel: String = "gemini-3.8-flash",
    val reasoningDepth: AntigravityReasoningDepth = AntigravityReasoningDepth.LOW,
    val autoToolApproval: Boolean = false,
    val isTestingConnection: Boolean = false,
    val testResult: String? = null,
    val isSuccess: Boolean = false
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
    FLASH_35("gemini-3.5-flash", "Gemini 3.5 Flash", "Ultra-fast response, lowest latency & native device actions"),
    FLASH_LITE("gemini-2.5-flash", "Gemini 2.5 Flash", "Lightweight, instant token streaming"),
    FLASH("gemini-3.8-flash", "Gemini 3.8 Flash", "Preview model with real-time reasoning"),
    PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep multi-step research & code synthesis"),
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
    val voiceSpeed: Float = 1.1f,
    val voicePitch: Float = 1.0f,
    val floatingOverlayEnabled: Boolean = false,
    val customApiEndpoint: String = "",
    val bubbleSizeDp: Int = 60,
    val bubbleOpacity: Float = 0.95f,
    val bubbleGlowEnabled: Boolean = true,
    val bubbleThemeColor: String = "CYAN",
    val themeConfig: ThemeConfig = ThemeConfig()
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
