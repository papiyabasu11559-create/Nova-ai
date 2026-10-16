package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ThemeConfig
import com.example.ui.theme.DarkBgEnd
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FintechDestructive
import com.example.ui.theme.FintechMicroLabel
import com.example.ui.theme.FintechOffline
import com.example.ui.theme.FintechSuccess
import com.example.ui.theme.FintechWarning
import com.example.ui.theme.LightBgEnd
import com.example.ui.theme.LightBgStart
import com.example.ui.theme.LightGlassCard
import com.example.ui.theme.LightGlassCardBorder
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MintAccentGlow
import com.example.ui.theme.MintGradient
import com.example.ui.theme.MintPrimary

// =========================================================================
// 1. Full-screen Atmosphere & Wallpaper Container
// =========================================================================

@Composable
fun FintechBackground(
    themeConfig: ThemeConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = !themeConfig.isLightTheme

    val baseBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(DarkBgStart, DarkBgEnd)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(LightBgStart, LightBgEnd)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBrush)
    ) {
        // Custom wallpaper if user set one
        if (!themeConfig.wallpaperUri.isNullOrBlank()) {
            AsyncImage(
                model = themeConfig.wallpaperUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = themeConfig.darkOverlayAlpha.coerceIn(0.2f, 0.9f))
                    )
            )
        }

        // Ambient faint mint / teal glow orb in top-right corner (as seen in fintech mockup)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDark) MintPrimary.copy(alpha = 0.08f) else MintPrimary.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        content()
    }
}

// =========================================================================
// 2. Layered Glassmorphism Card (24dp radius, hairline border, subtle glow)
// =========================================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    paddingValues: PaddingValues = PaddingValues(20.dp),
    borderTint: Color? = null,
    isGlowActive: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cardFill = if (isDark) DarkGlassCard else LightGlassCard
    val defaultBorder = if (isDark) DarkGlassCardBorder else LightGlassCardBorder
    val finalBorder = borderTint ?: defaultBorder

    val cardModifier = if (onClick != null) {
        modifier
            .shadow(
                elevation = if (isGlowActive) 16.dp else 8.dp,
                shape = shape,
                spotColor = if (isGlowActive) MintAccentGlow else (if (isDark) Color(0x59000000) else Color(0x1A103C2D))
            )
            .clip(shape)
            .background(cardFill)
            .border(width = 1.dp, color = finalBorder, shape = shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MintPrimary),
                onClick = onClick
            )
            .padding(paddingValues)
    } else {
        modifier
            .shadow(
                elevation = if (isGlowActive) 16.dp else 8.dp,
                shape = shape,
                spotColor = if (isGlowActive) MintAccentGlow else (if (isDark) Color(0x59000000) else Color(0x1A103C2D))
            )
            .clip(shape)
            .background(cardFill)
            .border(width = 1.dp, color = finalBorder, shape = shape)
            .padding(paddingValues)
    }

    Column(
        modifier = cardModifier,
        content = content
    )
}

// =========================================================================
// 3. Pill Chip / Category Tab (Fully rounded, 36dp tall, Mint Gradient when active)
// =========================================================================

@Composable
fun PillChip(
    label: String,
    selected: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    val containerModifier = if (selected) {
        Modifier
            .shadow(elevation = 10.dp, shape = CircleShape, spotColor = MintAccentGlow)
            .clip(CircleShape)
            .background(MintGradient)
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
    } else {
        Modifier
            .clip(CircleShape)
            .background(if (isDark) DarkGlassCard else LightGlassCard)
            .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, CircleShape)
    }

    Row(
        modifier = modifier
            .then(containerModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MintPrimary),
                onClick = onClick
            )
            .height(36.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) DarkBgStart else (if (isDark) DarkTextSecondary else LightTextSecondary),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) DarkBgStart else (if (isDark) DarkTextSecondary else LightTextSecondary)
        )
    }
}

// =========================================================================
// 4. Primary Button (Mint Gradient Pill, Dark Text, Soft Glow, 52dp tall)
// =========================================================================

@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    height: Dp = 52.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = CircleShape,
                spotColor = MintAccentGlow
            )
            .clip(CircleShape)
            .background(
                if (enabled) MintGradient else Brush.horizontalGradient(listOf(Color(0xFF2E483D), Color(0xFF1E352B)))
            )
            .border(
                1.2.dp,
                if (enabled) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                CircleShape
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = DarkBgStart,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DarkBgStart,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBgStart
                )
            }
        }
    }
}

// =========================================================================
// 5. Secondary Button (Glass Pill with Mint Border)
// =========================================================================

@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 48.dp
) {
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(if (isDark) DarkGlassCard else LightGlassCard)
            .border(1.dp, MintPrimary.copy(alpha = 0.5f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MintPrimary),
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MintPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MintPrimary
            )
        }
    }
}

// =========================================================================
// 6. Status Pill (Micro-labels with status dots)
// =========================================================================

enum class StatusPillType {
    SUCCESS, WARNING, ERROR, OFFLINE, INFO
}

@Composable
fun StatusPill(
    text: String,
    type: StatusPillType = StatusPillType.SUCCESS,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgColor, textColor) = when (type) {
        StatusPillType.SUCCESS -> Triple(FintechSuccess, FintechSuccess.copy(alpha = 0.15f), FintechSuccess)
        StatusPillType.WARNING -> Triple(FintechWarning, FintechWarning.copy(alpha = 0.15f), FintechWarning)
        StatusPillType.ERROR -> Triple(FintechDestructive, FintechDestructive.copy(alpha = 0.15f), FintechDestructive)
        StatusPillType.OFFLINE -> Triple(FintechOffline, FintechOffline.copy(alpha = 0.15f), FintechOffline)
        StatusPillType.INFO -> Triple(ElectricCyan, ElectricCyan.copy(alpha = 0.15f), ElectricCyan)
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(0.8.dp, dotColor.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text.uppercase(),
            style = FintechMicroLabel,
            color = textColor
        )
    }
}

// =========================================================================
// 7. Segmented Control (Pill container, active segment = mint gradient fill)
// =========================================================================

@Composable
fun SegmentedPillControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(if (isDark) DarkGlassCard else LightGlassCard)
            .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MintGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = MintPrimary),
                        onClick = { onSelect(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) DarkBgStart else (if (isDark) DarkTextSecondary else LightTextSecondary)
                )
            }
        }
    }
}

// =========================================================================
// 8. Mint Slider & Switch Controls
// =========================================================================

@Composable
fun MintSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    val isDark = LocalIsDarkTheme.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MintPrimary,
            activeTrackColor = MintPrimary,
            inactiveTrackColor = if (isDark) Color(0x3334D399) else Color(0x20103C2D)
        )
    )
}

@Composable
fun MintSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = DarkBgStart,
            checkedTrackColor = MintPrimary,
            uncheckedThumbColor = if (isDark) DarkTextSecondary else LightTextSecondary,
            uncheckedTrackColor = if (isDark) DarkGlassCard else LightGlassCard,
            uncheckedBorderColor = if (isDark) DarkGlassCardBorder else LightGlassCardBorder
        )
    )
}

// =========================================================================
// Backward Compatibility Components (Restyled to Emerald Mint Fintech)
// =========================================================================

@Composable
fun StatusBadge(
    text: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    StatusPill(
        text = text,
        type = if (isSuccess) StatusPillType.SUCCESS else StatusPillType.ERROR,
        modifier = modifier
    )
}

@Composable
fun QuickActionChip(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PillChip(
        label = "$icon $label",
        selected = false,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun LiveWaveform(
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    val heights = listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.8f, 1.0f, 0.7f, 0.5f, 0.9f, 0.6f, 0.3f)
    Row(
        modifier = modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { baseMultiplier ->
            val dynamicHeight = (8 + (rmsDb * 3f * baseMultiplier)).coerceIn(4f, 32f)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(dynamicHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MintGradient)
            )
        }
    }
}

@Composable
fun PulsingAIOld(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isStreaming: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowColor = if (isListening) ElectricCyan else MintPrimary

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.4f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((22 * (if (isListening || isStreaming) scale else 1f)).dp)
                .clip(CircleShape)
                .background(MintGradient)
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
        )
    }
}
