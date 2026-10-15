package com.example.overlay

import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

enum class BubbleThemeColor(
    val key: String,
    val displayName: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val glowColor: Int
) {
    CYAN(
        key = "CYAN",
        displayName = "Cyan",
        primaryColor = Color.parseColor("#22D3EE"),   // Neon Cyan
        secondaryColor = Color.parseColor("#7C3AED"), // Deep Purple
        glowColor = Color.parseColor("#22D3EE")
    ),
    PURPLE(
        key = "PURPLE",
        displayName = "Purple",
        primaryColor = Color.parseColor("#A855F7"),   // Electric Purple
        secondaryColor = Color.parseColor("#3B82F6"), // Royal Blue
        glowColor = Color.parseColor("#7C3AED")
    ),
    PINK(
        key = "PINK",
        displayName = "Pink",
        primaryColor = Color.parseColor("#EC4899"),   // Hot Pink
        secondaryColor = Color.parseColor("#8B5CF6"), // Violet
        glowColor = Color.parseColor("#EC4899")
    ),
    GREEN(
        key = "GREEN",
        displayName = "Green",
        primaryColor = Color.parseColor("#10B981"),   // Emerald
        secondaryColor = Color.parseColor("#06B6D4"), // Cyan/Aqua
        glowColor = Color.parseColor("#10B981")
    );

    companion object {
        fun fromKey(key: String): BubbleThemeColor {
            return values().firstOrNull { it.key.equals(key, ignoreCase = true) } ?: CYAN
        }
    }
}

enum class BubbleAssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class BubbleConfig(
    val sizeDp: Int = 60,
    val opacity: Float = 0.95f,
    val glowEnabled: Boolean = true,
    val theme: BubbleThemeColor = BubbleThemeColor.CYAN
)

object BubblePreferencesManager {
    const val PREF_BUBBLE_SIZE = "bubble_size_dp"
    const val PREF_BUBBLE_OPACITY = "bubble_opacity"
    const val PREF_BUBBLE_GLOW = "bubble_glow_enabled"
    const val PREF_BUBBLE_THEME = "bubble_theme_color"

    val currentConfig = MutableStateFlow(BubbleConfig())
    val currentState = MutableStateFlow(BubbleAssistantState.IDLE)
}
