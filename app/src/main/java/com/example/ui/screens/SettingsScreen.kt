package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ModelChoice
import com.example.overlay.BubbleThemeColor
import com.example.ui.NovaAssistantViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.MintSlider
import com.example.ui.components.MintSwitch
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.SecondaryPillButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusPillType
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FintechBody
import com.example.ui.theme.FintechDestructive
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
fun SettingsScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = LocalIsDarkTheme.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val overlayActive by viewModel.overlayActive.collectAsStateWithLifecycle()

    var apiKeyInput by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
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
                        Text(text = "⚡", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "NOVA AI Configuration",
                            style = FintechSectionTitle,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        Text(
                            text = "PREFERENCES & REASONING",
                            style = FintechMicroLabel,
                            color = MintPrimary
                        )
                    }
                }
                StatusPill(text = "OPTIMIZED", type = StatusPillType.SUCCESS)
            }
        }

        // Section 1: Google Gemini API Key Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "GOOGLE GEMINI API KEY",
                        style = FintechMicroLabel,
                        color = if (isDark) DarkTextMuted else LightTextMuted
                    )
                }

                if (settings.isKeyValidated) {
                    StatusPill(text = "VERIFIED", type = StatusPillType.SUCCESS)
                } else if (settings.customApiKey.isNotBlank()) {
                    StatusPill(text = "CUSTOM KEY", type = StatusPillType.INFO)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter your Google Gemini API key to unlock full autonomous capabilities, multimodal image recognition, and Antigravity execution.",
                style = FintechBody,
                color = if (isDark) DarkTextSecondary else LightTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // API Key Input Pill
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                placeholder = {
                    Text("AIzaSy...", fontSize = 13.sp, color = if (isDark) DarkTextMuted else LightTextMuted)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input_field"),
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = if (isDark) DarkTextSecondary else LightTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val pasted = clipData.getItemAt(0).text?.toString() ?: ""
                                if (pasted.isNotBlank()) {
                                    apiKeyInput = pasted.trim()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = MintPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) DarkGlassCard else LightGlassCard,
                    unfocusedContainerColor = if (isDark) DarkGlassCard else LightGlassCard,
                    focusedBorderColor = MintPrimary,
                    unfocusedBorderColor = if (isDark) DarkGlassCardBorder else LightGlassCardBorder,
                    focusedTextColor = if (isDark) DarkTextPrimary else LightTextPrimary,
                    unfocusedTextColor = if (isDark) DarkTextPrimary else LightTextPrimary,
                    cursorColor = MintPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    viewModel.validateAndAutoSelectModel(apiKeyInput)
                })
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryPillButton(
                text = "Validate & Detect Models",
                onClick = {
                    keyboardController?.hide()
                    viewModel.validateAndAutoSelectModel(apiKeyInput)
                },
                height = 46.dp
            )
        }

        // Section 2: Model Override Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "MODEL OVERRIDE",
                        style = FintechMicroLabel,
                        color = if (isDark) DarkTextMuted else LightTextMuted
                    )
                }

                Text(
                    text = settings.selectedModel.displayName,
                    style = FintechMicroLabel,
                    color = MintPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModelChoice.values().forEach { model ->
                    val isSelected = settings.selectedModel == model

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
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
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.setModelChoice(model) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                            )
                            Text(
                                text = model.description,
                                fontSize = 11.sp,
                                color = if (isDark) DarkTextSecondary else LightTextSecondary
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setModelChoice(model) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MintPrimary,
                                unselectedColor = if (isDark) DarkTextMuted else LightTextMuted
                            )
                        )
                    }
                }
            }
        }

        // Section 3: Floating Assistant Bubble (⚡ Overlay) Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "FLOATING ASSISTANT BUBBLE (⚡)",
                        style = FintechMicroLabel,
                        color = if (isDark) DarkTextMuted else LightTextMuted
                    )
                }

                MintSwitch(
                    checked = overlayActive,
                    onCheckedChange = { enable ->
                        viewModel.toggleOverlay(enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Premium glass bubble overlay with physics edge-docking, long-press radial menu, and Android 12+ background blur.",
                style = FintechBody,
                color = if (isDark) DarkTextSecondary else LightTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bubble Diameter Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bubble Diameter",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
                Text(
                    text = "${settings.bubbleSizeDp} dp",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary
                )
            }

            MintSlider(
                value = settings.bubbleSizeDp.toFloat(),
                onValueChange = { viewModel.setBubbleSize(it.toInt()) },
                valueRange = 40f..80f
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bubble Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Glass Transparency",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
                Text(
                    text = "${(settings.bubbleOpacity * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary
                )
            }

            MintSlider(
                value = settings.bubbleOpacity,
                onValueChange = { viewModel.setBubbleOpacity(it) },
                valueRange = 0.3f..1.0f
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Neon Outer Glow Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Neon Ambient Outer Glow",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
                MintSwitch(
                    checked = settings.bubbleGlowEnabled,
                    onCheckedChange = { viewModel.setBubbleGlowEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Theme Color Picker Row
            Text(
                text = "BUBBLE ACCENT PALETTE",
                style = FintechMicroLabel,
                color = if (isDark) DarkTextMuted else LightTextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BubbleThemeColor.values().forEach { themeItem ->
                    val isSelected = settings.bubbleThemeColor == themeItem.key
                    val itemColor = Color(themeItem.primaryColor)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .shadow(if (isSelected) 8.dp else 0.dp, CircleShape, spotColor = itemColor)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.background(Brush.horizontalGradient(listOf(itemColor, Color(themeItem.secondaryColor))))
                                } else {
                                    Modifier.background(if (isDark) DarkGlassCard else LightGlassCard)
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.6f) else (if (isDark) DarkGlassCardBorder else LightGlassCardBorder),
                                shape = CircleShape
                            )
                            .clickable { viewModel.setBubbleTheme(themeItem.key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = themeItem.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else (if (isDark) DarkTextSecondary else LightTextSecondary)
                        )
                    }
                }
            }
        }

        // Section 4: Voice & Audio Synthesizer Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "VOICE & AUDIO SYNTHESIZER",
                        style = FintechMicroLabel,
                        color = if (isDark) DarkTextMuted else LightTextMuted
                    )
                }

                MintSwitch(
                    checked = settings.autoSpeakResponses,
                    onCheckedChange = { viewModel.toggleAutoSpeak(it) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speech Rate Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Speaking Speed",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
                Text(
                    text = String.format("%.2fx", settings.voiceSpeed),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary
                )
            }

            MintSlider(
                value = settings.voiceSpeed,
                onValueChange = { viewModel.setVoiceSpeed(it) },
                valueRange = 0.8f..1.5f
            )
        }

        // Section 5: Destructive Actions / Privacy Glass Card (Red text, faint red border)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderTint = FintechDestructive.copy(alpha = 0.35f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = FintechDestructive, modifier = Modifier.size(18.dp))
                Text(
                    text = "PRIVACY & LOCAL DEVICE SECURITY",
                    style = FintechMicroLabel,
                    color = FintechDestructive
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NOVA AI runs device actions locally. You can wipe all chat logs, automation history, and cached context at any time.",
                style = FintechBody,
                color = if (isDark) DarkTextSecondary else LightTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Clear Chat History Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(FintechDestructive.copy(alpha = 0.12f))
                        .border(1.dp, FintechDestructive.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            viewModel.clearHistory()
                            Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clear History",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FintechDestructive
                    )
                }

                // Wipe All Logs Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(FintechDestructive.copy(alpha = 0.12f))
                        .border(1.dp, FintechDestructive.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            viewModel.clearAutomationLogs()
                            Toast.makeText(context, "All logs wiped", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Wipe Audit Logs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FintechDestructive
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
