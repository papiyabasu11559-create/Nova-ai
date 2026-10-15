package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.example.overlay.BubbleThemeColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.NovaApplication
import com.example.data.model.ModelChoice
import com.example.ui.NovaAssistantViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SuccessTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet

@Composable
fun SettingsScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val app = context.applicationContext as NovaApplication
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val overlayActive by viewModel.overlayActive.collectAsStateWithLifecycle()

    var apiKeyInput by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var personaText by remember(settings.personaTone) { mutableStateOf(settings.personaTone) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ObsidianSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "NOVA AI Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Google API Key, Auto-Model Selection, Voice & Device Control",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Section 1: Google API Key & Automatic Model Detection
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Google Gemini API Key",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-detects & selects compatible Gemini model",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "Paste your Google Gemini API key below. NOVA AI queries Google's models endpoint to verify the key and automatically selects the best available Gemini model for full device actions.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextSecondary
                )

                // Key Input Field
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = {
                        Text("AIzaSy...", fontSize = 13.sp, color = TextMuted)
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
                                    contentDescription = "Toggle key visibility",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
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
                                    contentDescription = "Paste from clipboard",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurfaceVariant,
                        unfocusedContainerColor = ObsidianSurfaceVariant,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        viewModel.validateAndAutoSelectModel(apiKeyInput)
                    })
                )

                // Validation Status / Message
                if (settings.keyValidationMessage.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (settings.isKeyValidated) SuccessTeal.copy(alpha = 0.12f) else ObsidianSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (settings.isKeyValidated) SuccessTeal.copy(alpha = 0.5f) else ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (settings.isKeyValidated) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (settings.isKeyValidated) SuccessTeal else VividViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = settings.keyValidationMessage,
                                fontSize = 12.sp,
                                color = if (settings.isKeyValidated) SuccessTeal else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.validateAndAutoSelectModel(apiKeyInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("validate_key_button")
                ) {
                    Text(
                        text = "Verify Key & Auto-Select Model",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBackground
                    )
                }

                // Currently active model badge
                val activeModel = settings.autoDetectedModelName.ifBlank { settings.selectedModel.displayName }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active Brain Model:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VividViolet.copy(alpha = 0.2f))
                            .border(0.8.dp, VividViolet.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = activeModel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VividViolet
                        )
                    }
                }
            }
        }

        // Section 2: Manual Model Override (Optional)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Assistant, contentDescription = null, tint = ElectricCyan)
                    Text(
                        text = "Model Override / Selection",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ModelChoice.values().forEach { model ->
                    val isSelected = (settings.autoDetectedModelName.isBlank() && settings.selectedModel == model) ||
                            settings.autoDetectedModelName == model.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setModelChoice(model) }
                            .background(if (isSelected) ObsidianSurfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setModelChoice(model) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ElectricCyan,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = model.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) ElectricCyan else TextPrimary
                            )
                            Text(
                                text = model.description,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Floating Assistant Overlay Customization
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, VividViolet.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VividViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = VividViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Floating Assistant Bubble (⚡)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Glass HUD overlay with radial quick menu",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = overlayActive,
                        onCheckedChange = { enabled ->
                            viewModel.toggleOverlay(
                                enable = enabled,
                                onPermissionRequired = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricCyan,
                            checkedTrackColor = VividViolet.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurfaceVariant
                        ),
                        modifier = Modifier.testTag("floating_overlay_switch")
                    )
                }

                val currentTheme = BubbleThemeColor.fromKey(settings.bubbleThemeColor)
                val primaryColorCompose = androidx.compose.ui.graphics.Color(currentTheme.primaryColor)
                val secondaryColorCompose = androidx.compose.ui.graphics.Color(currentTheme.secondaryColor)

                // Interactive Mini-Preview of the Glass Bubble
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Glass Orb Preview
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            primaryColorCompose.copy(alpha = 0.45f * settings.bubbleOpacity),
                                            secondaryColorCompose.copy(alpha = 0.55f * settings.bubbleOpacity)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                        listOf(
                                            primaryColorCompose,
                                            secondaryColorCompose,
                                            primaryColorCompose
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 24.sp,
                                color = primaryColorCompose
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${currentTheme.displayName} Holographic Orb",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (overlayActive) "● Active on screen • Tap to open • Long-press menu" else "○ Disabled (Toggle switch above to launch)",
                                fontSize = 11.sp,
                                color = if (overlayActive) ElectricCyan else TextMuted
                            )
                        }
                    }
                }

                HorizontalDivider(color = ObsidianBorder)

                // 1. Size Slider (40dp - 80dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Text(text = "Bubble Diameter", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = "${settings.bubbleSizeDp} dp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }

                    Slider(
                        value = settings.bubbleSizeDp.toFloat(),
                        onValueChange = { viewModel.setBubbleSize(it.toInt()) },
                        valueRange = 40f..80f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = ObsidianBorder
                        ),
                        modifier = Modifier.testTag("bubble_size_slider")
                    )
                }

                // 2. Opacity Slider (30% - 100%)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Glass Translucency & Opacity", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${(settings.bubbleOpacity * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VividViolet
                        )
                    }

                    Slider(
                        value = settings.bubbleOpacity,
                        onValueChange = { viewModel.setBubbleOpacity(it) },
                        valueRange = 0.30f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = VividViolet,
                            activeTrackColor = VividViolet,
                            inactiveTrackColor = ObsidianBorder
                        ),
                        modifier = Modifier.testTag("bubble_opacity_slider")
                    )
                }

                // 3. Glow Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Column {
                            Text(text = "Neon Glow Aura", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(text = "16dp soft outer glow with breathing pulse", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Switch(
                        checked = settings.bubbleGlowEnabled,
                        onCheckedChange = { viewModel.setBubbleGlowEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricCyan,
                            checkedTrackColor = ElectricCyan.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("bubble_glow_switch")
                    )
                }

                // 4. Theme Color Picker (Cyan, Purple, Pink, Green)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = VividViolet, modifier = Modifier.size(16.dp))
                        Text(text = "Theme Accent Color", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BubbleThemeColor.values().forEach { themeItem ->
                            val isSelected = currentTheme == themeItem
                            val itemColor = androidx.compose.ui.graphics.Color(themeItem.primaryColor)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) itemColor.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) itemColor else ObsidianBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setBubbleTheme(themeItem.key) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(itemColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = androidx.compose.ui.graphics.Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = themeItem.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) itemColor else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = ObsidianBorder)

                // Feature details & Gesture guide
                Text(
                    text = "• Android 12+ Real-time Background Blur (FLAG_BLUR_BEHIND)\n• Auto-snaps to nearest screen edge with a spring\n• Dims to 60% alpha & half-docks after 3s idle (instant wake on touch)\n• Long-press reveals 4-action radial menu (Voice, Chat, Camera, Close)\n• Dynamic states: Breathing Idle ⚡, Audio-Wave Listening 🎙️, Rotating Arc Thinking 🌀, Pulsing Speaking 🔊",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = TextMuted
                )
            }
        }

        // Section 4: Assistant Personality Tone
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Assistant Persona Tone",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Defines how NOVA AI speaks, reasons, and executes commands.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = personaText,
                    onValueChange = {
                        personaText = it
                        viewModel.updatePersonaTone(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurfaceVariant,
                        unfocusedContainerColor = ObsidianSurfaceVariant,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )
            }
        }

        // Section 5: Voice & Speech Output
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricCyan)
                        Column {
                            Text(
                                text = "Auto-Speak AI Responses",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Read assistant responses aloud via Android TTS",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = settings.autoSpeakResponses,
                        onCheckedChange = { viewModel.toggleAutoSpeak(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricCyan,
                            checkedTrackColor = ElectricCyan.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        app.voiceSubsystem.speak(
                            "NOVA AI voice synthesis online. Ready to execute your native device commands.",
                            pitch = settings.voicePitch,
                            speed = settings.voiceSpeed
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "🔊 Test Speech Synthesis", fontSize = 12.sp, color = ElectricCyan)
                }
            }
        }

        // Section 6: Privacy, Safety & Data
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ElectricCyan)
                    Text(
                        text = "Privacy & Local Device Security",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "• All conversation memory is stored in an encrypted local SQLite database.\n• No background audio is recorded without explicit Push-to-Talk touch activation.\n• Sensitive actions (SMS dispatch, phone calls) require Android user consent before executing.\n• API keys are stored in secure local preferences on your device.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = TextSecondary
                )

                HorizontalDivider(color = ObsidianBorder)

                Button(
                    onClick = {
                        viewModel.clearHistory()
                        viewModel.clearAutomationLogs()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Wipe All Local Data & Logs",
                        fontSize = 12.sp,
                        color = ErrorCrimson,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
