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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ThemeConfig
import com.example.ui.NovaAssistantViewModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet

@Composable
fun ThemesScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomWallpaper(uri.toString())
        }
    }

    val presetThemes = listOf(
        ThemeConfig(
            presetId = "CYBER",
            name = "Cyber Neon",
            primaryColorHex = "#22D3EE",
            secondaryColorHex = "#7C3AED",
            accentColorHex = "#00F2FE",
            darkOverlayAlpha = 0.65f
        ),
        ThemeConfig(
            presetId = "ORANGE",
            name = "Solar Flame",
            primaryColorHex = "#F28C52",
            secondaryColorHex = "#EF4444",
            accentColorHex = "#F97316",
            darkOverlayAlpha = 0.65f
        ),
        ThemeConfig(
            presetId = "PURPLE",
            name = "Galactic Purple",
            primaryColorHex = "#A855F7",
            secondaryColorHex = "#3B82F6",
            accentColorHex = "#7C3AED",
            darkOverlayAlpha = 0.60f
        ),
        ThemeConfig(
            presetId = "GREEN",
            name = "Emerald Matrix",
            primaryColorHex = "#10B981",
            secondaryColorHex = "#06B6D4",
            accentColorHex = "#10B981",
            darkOverlayAlpha = 0.70f
        ),
        ThemeConfig(
            presetId = "PINK",
            name = "Neon Synth",
            primaryColorHex = "#EC4899",
            secondaryColorHex = "#8B5CF6",
            accentColorHex = "#F43F5E",
            darkOverlayAlpha = 0.65f
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(text = "Theme & Visual Customization", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Wallpapers, neon palettes, dark overlays & blur", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Live Preview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(android.graphics.Color.parseColor(currentTheme.primaryColorHex))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Background Wallpaper if set
                if (currentTheme.wallpaperUri != null) {
                    AsyncImage(
                        model = currentTheme.wallpaperUri,
                        contentDescription = "Theme wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(
                                        Color(android.graphics.Color.parseColor(currentTheme.primaryColorHex)).copy(alpha = 0.25f),
                                        Color(android.graphics.Color.parseColor(currentTheme.secondaryColorHex)).copy(alpha = 0.35f),
                                        ObsidianBackground
                                    )
                                )
                            )
                    )
                }

                // Dark Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = currentTheme.darkOverlayAlpha))
                )

                // Foreground mock preview elements
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentTheme.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(android.graphics.Color.parseColor(currentTheme.primaryColorHex))
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Active Theme", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // Mini mock chat bubbles
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(android.graphics.Color.parseColor(currentTheme.primaryColorHex)).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(android.graphics.Color.parseColor(currentTheme.primaryColorHex)))
                        ) {
                            Text(
                                text = "Hello NOVA AI! Show me today's plan.",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceVariant.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "✓ Battery 85% • Flashlight ready • Voice online.",
                                fontSize = 11.sp,
                                color = Color(android.graphics.Color.parseColor(currentTheme.accentColorHex)),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Preset Themes
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Preset Color Themes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                presetThemes.forEach { preset ->
                    val isSelected = currentTheme.presetId == preset.presetId
                    val primary = Color(android.graphics.Color.parseColor(preset.primaryColorHex))
                    val secondary = Color(android.graphics.Color.parseColor(preset.secondaryColorHex))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ObsidianSurfaceVariant else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) primary else ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.applyTheme(preset.copy(wallpaperUri = currentTheme.wallpaperUri))
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(primary, secondary)
                                            )
                                        )
                                )
                                Text(
                                    text = preset.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) primary else TextPrimary
                                )
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Custom Wallpaper Photo Picker
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Chat Background Wallpaper", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (currentTheme.wallpaperUri != null) {
                        IconButton(onClick = { viewModel.setCustomWallpaper(null) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Wallpaper", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentTheme.wallpaperUri != null) "Choose Different Photo" else "Pick Photo from Gallery",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }

                // Dark Overlay Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Dark Readability Overlay", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "${(currentTheme.darkOverlayAlpha * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                    }
                    Slider(
                        value = currentTheme.darkOverlayAlpha,
                        onValueChange = { viewModel.setWallpaperDarkOverlay(it) },
                        valueRange = 0.20f..0.90f,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = ObsidianBorder
                        )
                    )
                }

                // Wallpaper Blur Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Wallpaper Blur", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "${currentTheme.blurRadiusDp.toInt()} dp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VividViolet)
                    }
                    Slider(
                        value = currentTheme.blurRadiusDp,
                        onValueChange = { viewModel.setWallpaperBlur(it) },
                        valueRange = 0f..25f,
                        colors = SliderDefaults.colors(
                            thumbColor = VividViolet,
                            activeTrackColor = VividViolet,
                            inactiveTrackColor = ObsidianBorder
                        )
                    )
                }
            }
        }

        // Section 3: Reset
        OutlinedButton(
            onClick = { viewModel.resetThemeToDefault() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Reset Theme to Default Cyber Neon", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
