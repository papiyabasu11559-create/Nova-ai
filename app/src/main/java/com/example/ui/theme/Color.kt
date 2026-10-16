package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// NOVA AI - Fintech Glassmorphism Design Tokens (Authoritative Specification)
// =========================================================================

// Dark Theme Base (Teal-Black Depth)
val DarkBgStart = Color(0xFF08150F)
val DarkBgEnd = Color(0xFF0B1F17)
val DarkGlassCard = Color(0x0FFFFFFF)          // rgba(255, 255, 255, 0.06)
val DarkGlassCardBorder = Color(0x1AFFFFFF)    // rgba(255, 255, 255, 0.10)
val DarkGlassInnerHighlight = Color(0x0FFFFFFF) // rgba(255, 255, 255, 0.06)

// Light Theme Base (Near-white with faint mint wash)
val LightBgStart = Color(0xFFF5F8F6)
val LightBgEnd = Color(0xFFFFFFFF)
val LightGlassCard = Color(0xB3FFFFFF)         // rgba(255, 255, 255, 0.70)
val LightGlassCardBorder = Color(0x14103C2D)   // rgba(16, 60, 45, 0.08)
val LightGlassInnerHighlight = Color(0x40FFFFFF)

// Primary Accent - Emerald / Mint Fintech Palette
val MintPrimary = Color(0xFF34D399)
val EmeraldPrimary = Color(0xFF10B981)
val MintAccentGlow = Color(0x5934D399)        // rgba(52, 211, 153, 0.35)

// Secondary Accent - NOVA Identity preserved
val ElectricCyan = Color(0xFF22D3EE)
val CyanBright = Color(0xFF38BDF8)
val CyanContainer = Color(0xFF00384E)
val OnCyanContainer = Color(0xFFB8F3FF)

val VividViolet = Color(0xFFA855F7)
val VioletBright = Color(0xFFC084FC)

// Dark Theme Typography & Contrast
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF9FB3AC)
val DarkTextMuted = Color(0xFF5F7A70)

// Light Theme Typography & Contrast
val LightTextPrimary = Color(0xFF0B1F17)
val LightTextSecondary = Color(0xFF4A5F58)
val LightTextMuted = Color(0xFF6B827A)

// Status & Semantic Colors
val FintechSuccess = Color(0xFF34D399)
val FintechWarning = Color(0xFFFBBF24)
val FintechDestructive = Color(0xFFFF5A5F)
val FintechOffline = Color(0xFF718096)
val SuccessTeal = Color(0xFF10B981)

// Backward Compatibility Aliases for existing components
val ObsidianBackground = DarkBgStart
val ObsidianSurface = Color(0xFF0D221A)
val ObsidianSurfaceVariant = Color(0xFF122C22)
val ObsidianSurfaceElevated = Color(0xFF17382B)
val ObsidianBorder = DarkGlassCardBorder
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val TextMuted = DarkTextMuted

// Gradients
val MintGradient = Brush.horizontalGradient(
    colors = listOf(MintPrimary, EmeraldPrimary)
)

val MintVerticalGradient = Brush.verticalGradient(
    colors = listOf(MintPrimary, EmeraldPrimary)
)

val DarkBackgroundGradient = Brush.linearGradient(
    colors = listOf(DarkBgStart, DarkBgEnd)
)

val LightBackgroundGradient = Brush.linearGradient(
    colors = listOf(LightBgStart, LightBgEnd)
)
