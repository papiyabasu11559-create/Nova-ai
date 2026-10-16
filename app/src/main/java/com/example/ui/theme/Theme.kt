package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { true }

private val NovaDarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = DarkBgStart,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = DarkBgStart,
    secondaryContainer = Color(0xFF00384E),
    onSecondaryContainer = Color(0xFFB8F3FF),
    tertiary = MintPrimary,
    onTertiary = DarkBgStart,
    background = DarkBgStart,
    onBackground = DarkTextPrimary,
    surface = DarkGlassCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0x14FFFFFF),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkGlassCardBorder,
    outlineVariant = Color(0x0FFFFFFF),
    error = FintechDestructive,
    onError = Color.White
)

private val NovaLightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = LightTextPrimary,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = LightTextPrimary,
    tertiary = EmeraldPrimary,
    onTertiary = Color.White,
    background = LightBgStart,
    onBackground = LightTextPrimary,
    surface = LightGlassCard,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0x0A103C2D),
    onSurfaceVariant = LightTextSecondary,
    outline = LightGlassCardBorder,
    outlineVariant = Color(0x0A103C2D),
    error = FintechDestructive,
    onError = Color.White
)

@Composable
fun NovaAiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NovaDarkColorScheme else NovaLightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward compatibility alias for template
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NovaAiTheme(darkTheme = darkTheme, content = content)
}
