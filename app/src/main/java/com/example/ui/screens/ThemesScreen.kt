package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ThemeConfig
import com.example.ui.NovaAssistantViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.MintSlider
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.SecondaryPillButton
import com.example.ui.components.SegmentedPillControl
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusPillType
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.FintechBody
import com.example.ui.theme.FintechMicroLabel
import com.example.ui.theme.FintechSectionTitle
import com.example.ui.theme.LightGlassCard
import com.example.ui.theme.LightGlassCardBorder
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MintAccentGlow
import com.example.ui.theme.MintGradient
import com.example.ui.theme.MintPrimary

@Composable
fun ThemesScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomWallpaper(uri.toString())
        }
    }

    val presetThemes = listOf(
        ThemeConfig(
            presetId = "MINT",
            name = "Emerald Mint Glass",
            primaryColorHex = "#34D399",
            secondaryColorHex = "#10B981",
            accentColorHex = "#22D3EE",
            darkOverlayAlpha = 0.65f,
            isLightTheme = currentTheme.isLightTheme
        ),
        ThemeConfig(
            presetId = "CYBER",
            name = "Electric Cyber",
            primaryColorHex = "#22D3EE",
            secondaryColorHex = "#7C3AED",
            accentColorHex = "#00F2FE",
            darkOverlayAlpha = 0.65f,
            isLightTheme = currentTheme.isLightTheme
        ),
        ThemeConfig(
            presetId = "ORANGE",
            name = "Solar Flame",
            primaryColorHex = "#F28C52",
            secondaryColorHex = "#EF4444",
            accentColorHex = "#F97316",
            darkOverlayAlpha = 0.65f,
            isLightTheme = currentTheme.isLightTheme
        ),
        ThemeConfig(
            presetId = "PURPLE",
            name = "Galactic Purple",
            primaryColorHex = "#A855F7",
            secondaryColorHex = "#3B82F6",
            accentColorHex = "#7C3AED",
            darkOverlayAlpha = 0.60f,
            isLightTheme = currentTheme.isLightTheme
        ),
        ThemeConfig(
            presetId = "GREEN",
            name = "Emerald Matrix",
            primaryColorHex = "#10B981",
            secondaryColorHex = "#06B6D4",
            accentColorHex = "#10B981",
            darkOverlayAlpha = 0.70f,
            isLightTheme = currentTheme.isLightTheme
        ),
        ThemeConfig(
            presetId = "PINK",
            name = "Neon Synth",
            primaryColorHex = "#EC4899",
            secondaryColorHex = "#8B5CF6",
            accentColorHex = "#F43F5E",
            darkOverlayAlpha = 0.65f,
            isLightTheme = currentTheme.isLightTheme
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Theme & Visual Customization with preview
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            isGlowActive = true,
            borderTint = MintPrimary
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(8.dp, CircleShape, spotColor = MintAccentGlow)
                            .clip(CircleShape)
                            .background(MintGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = DarkBgStart,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Visual Theme Suite",
                            style = FintechSectionTitle,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        Text(
                            text = "FINTECH GLASS ENGINE",
                            style = FintechMicroLabel,
                            color = MintPrimary
                        )
                    }
                }

                StatusPill(text = "ACTIVE", type = StatusPillType.SUCCESS)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Gradient Preview Panel
            val primaryColor = Color(android.graphics.Color.parseColor(currentTheme.primaryColorHex))
            val secondaryColor = Color(android.graphics.Color.parseColor(currentTheme.secondaryColorHex))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.7f), secondaryColor.copy(alpha = 0.5f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = currentTheme.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentTheme.isLightTheme) "Light Palette • Frosted Mint Wash" else "Dark Palette • Deep Emerald Obsidian",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Section 1: Dark / Light Mode Switch
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "LIGHT & DARK PALETTES",
                style = FintechMicroLabel,
                color = if (isDark) DarkTextMuted else LightTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            SegmentedPillControl(
                options = listOf("🌙 Dark Obsidian", "☀️ Light Mint"),
                selectedIndex = if (currentTheme.isLightTheme) 1 else 0,
                onSelect = { idx ->
                    viewModel.setThemeLightDark(isLight = idx == 1)
                }
            )
        }

        // Section 2: Preset Themes List
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PRESET THEMES",
                    style = FintechMicroLabel,
                    color = if (isDark) DarkTextMuted else LightTextMuted
                )
                Text(
                    text = "${presetThemes.size} PALETTES",
                    style = FintechMicroLabel,
                    color = MintPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presetThemes.forEach { preset ->
                    val isSelected = currentTheme.presetId == preset.presetId
                    val presetPrimary = Color(android.graphics.Color.parseColor(preset.primaryColorHex))
                    val presetSecondary = Color(android.graphics.Color.parseColor(preset.secondaryColorHex))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) {
                                    if (isDark) Color(0x1F34D399) else Color(0x1410B981)
                                } else {
                                    if (isDark) DarkGlassCard else LightGlassCard
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MintPrimary else (if (isDark) DarkGlassCardBorder else LightGlassCardBorder),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { viewModel.applyTheme(preset) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Gradient Color Orb
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(6.dp, CircleShape, spotColor = presetPrimary)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(presetPrimary, presetSecondary))
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )

                            Column {
                                Text(
                                    text = preset.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                                Text(
                                    text = "${preset.primaryColorHex} • ${preset.secondaryColorHex}",
                                    fontSize = 11.sp,
                                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .shadow(6.dp, CircleShape, spotColor = MintAccentGlow)
                                    .clip(CircleShape)
                                    .background(MintGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = DarkBgStart,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Custom Wallpaper & Transparency
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CUSTOM WALLPAPER",
                    style = FintechMicroLabel,
                    color = if (isDark) DarkTextMuted else LightTextMuted
                )
                if (currentTheme.wallpaperUri != null) {
                    IconButton(
                        onClick = { viewModel.setCustomWallpaper(null) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Wallpaper",
                            tint = Color(0xFFFF5A5F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryPillButton(
                text = if (currentTheme.wallpaperUri != null) "Change Photo Wallpaper" else "Choose Wallpaper from Gallery",
                icon = Icons.Default.AddPhotoAlternate,
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                height = 48.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dark overlay opacity slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wallpaper Dark Wash",
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(currentTheme.darkOverlayAlpha * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary
                )
            }

            MintSlider(
                value = currentTheme.darkOverlayAlpha,
                onValueChange = { viewModel.setWallpaperDarkOverlay(it) },
                valueRange = 0.2f..0.95f
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Blur radius slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Frosted Blur Strength",
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${currentTheme.blurRadiusDp.toInt()} dp",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary
                )
            }

            MintSlider(
                value = currentTheme.blurRadiusDp,
                onValueChange = { viewModel.setWallpaperBlur(it) },
                valueRange = 0f..25f
            )
        }

        // Section 4: Reset Theme Button
        SecondaryPillButton(
            text = "Reset to Default Theme",
            icon = Icons.Default.Refresh,
            onClick = { viewModel.resetThemeToDefault() },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
