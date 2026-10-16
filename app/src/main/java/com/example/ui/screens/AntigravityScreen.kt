package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AntigravityReasoningDepth
import com.example.ui.NovaAssistantViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.MintSwitch
import com.example.ui.components.PillChip
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.SecondaryPillButton
import com.example.ui.components.SegmentedPillControl
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusPillType
import com.example.ui.theme.DarkBgEnd
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.FintechBody
import com.example.ui.theme.FintechDestructive
import com.example.ui.theme.LightBgEnd
import com.example.ui.theme.LightBgStart
import com.example.ui.theme.FintechMicroLabel
import com.example.ui.theme.FintechSectionTitle
import com.example.ui.theme.FintechSuccess
import com.example.ui.theme.LightGlassCard
import com.example.ui.theme.LightGlassCardBorder
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MintAccentGlow
import com.example.ui.theme.MintGradient
import com.example.ui.theme.MintPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val antigravityState by viewModel.antigravityState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var codeInput by remember { mutableStateOf("") }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("v2.4.2 (Latest agent)") }

    // Auto-detect code in clipboard on entry
    LaunchedEffect(Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                if (clipText.startsWith("ag_") || (clipText.length in 20..120 && !clipText.contains(" "))) {
                    if (codeInput.isBlank()) {
                        codeInput = clipText.trim()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    val availableModelsList = listOf(
        Pair("gemini-3.8-flash", "Gemini 3.8 Flash (High Speed)"),
        Pair("gemini-3.5-flash", "Gemini 3.5 Flash (Ultra-Low Latency)"),
        Pair("gemini-2.5-flash", "Gemini 2.5 Flash Lite (Fastest)"),
        Pair("gemini-3.1-pro-preview", "Gemini 3.1 Pro (Deep Reasoning)"),
        Pair("gemini-4-argon", "Gemini 4 Argon (Multimodal)")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Hero Card with Status Pill (Active Mint / Offline Grey)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            isGlowActive = true,
            borderTint = MintPrimary
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = DarkBgStart, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Google Antigravity",
                            style = FintechSectionTitle,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        Text(
                            text = "CLOUD AGENT WORKSPACE",
                            style = FintechMicroLabel,
                            color = MintPrimary
                        )
                    }
                }

                StatusPill(
                    text = if (antigravityState.isConnected) "ACTIVE" else "OFFLINE",
                    type = if (antigravityState.isConnected) StatusPillType.SUCCESS else StatusPillType.OFFLINE
                )
            }
        }

        // Section 1: Google Account Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (antigravityState.isConnected) {
                // Connected state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MintPrimary.copy(alpha = 0.2f))
                                .border(1.dp, MintPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MintPrimary)
                        }
                        Column {
                            Text(
                                text = antigravityState.userEmail,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                            )
                            Text(
                                text = "Authenticated with Google",
                                fontSize = 11.sp,
                                color = MintPrimary
                            )
                        }
                    }

                    TextButton(onClick = { showDisconnectDialog = true }) {
                        Text(text = "Disconnect", fontSize = 12.sp, color = FintechDestructive, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Signed-out state
                Text(
                    text = "GOOGLE WORKSPACE AUTHENTICATION",
                    style = FintechMicroLabel,
                    color = if (isDark) DarkTextMuted else LightTextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in to access your private Antigravity Linux workspace, higher reasoning budgets, and cloud agents.",
                    style = FintechBody,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Primary Mint Gradient Pill Button
                PrimaryPillButton(
                    text = "Sign in with Antigravity",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://antigravity.google/docs/install/#authenticate")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignored
                        }
                    },
                    height = 50.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Code Input Field
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    placeholder = {
                        Text("Paste auth code here...", fontSize = 12.sp, color = if (isDark) DarkTextMuted else LightTextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) codeInput = text.trim()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = MintPrimary, modifier = Modifier.size(18.dp))
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                SecondaryPillButton(
                    text = "Verify & Connect Agent",
                    onClick = { viewModel.connectAntigravityWithCode(codeInput) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 2: Model Dropdown Glass Tile with Chevron
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showModelBottomSheet = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE INTELLIGENCE MODEL",
                        style = FintechMicroLabel,
                        color = if (isDark) DarkTextMuted else LightTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = availableModelsList.find { it.first == antigravityState.selectedModel }?.second ?: antigravityState.selectedModel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Model",
                    tint = MintPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Section 3: Reasoning Depth Segmented Control (Low / Medium / High)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "REASONING BUDGET DEPTH",
                style = FintechMicroLabel,
                color = if (isDark) DarkTextMuted else LightTextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            val depthLabel = when (antigravityState.reasoningDepth) {
                AntigravityReasoningDepth.LOW -> "Instant execution (0 tokens)"
                AntigravityReasoningDepth.MEDIUM -> "Balanced reasoning (2,048 tokens)"
                AntigravityReasoningDepth.HIGH -> "Maximum reasoning depth (8,192 tokens)"
            }
            Text(text = depthLabel, style = FintechBody, color = MintPrimary)

            Spacer(modifier = Modifier.height(12.dp))

            val depths = listOf("Low", "Medium", "High")
            val currentIdx = when (antigravityState.reasoningDepth) {
                AntigravityReasoningDepth.LOW -> 0
                AntigravityReasoningDepth.MEDIUM -> 1
                AntigravityReasoningDepth.HIGH -> 2
            }

            SegmentedPillControl(
                options = depths,
                selectedIndex = currentIdx,
                onSelect = { idx ->
                    val chosen = when (idx) {
                        0 -> AntigravityReasoningDepth.LOW
                        1 -> AntigravityReasoningDepth.MEDIUM
                        else -> AntigravityReasoningDepth.HIGH
                    }
                    viewModel.setAntigravityReasoningDepth(chosen)
                }
            )
        }

        // Section 4: Secondary Glass Button for "Test Connection"
        SecondaryPillButton(
            text = if (antigravityState.isTestingConnection) "Testing Connection..." else "Test Antigravity Connection",
            icon = Icons.Default.Sync,
            onClick = { viewModel.testAntigravityConnection() },
            modifier = Modifier.fillMaxWidth(),
            height = 50.dp
        )

        // Test result chip if available
        if (antigravityState.testResult != null) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderTint = if (antigravityState.isSuccess) MintPrimary else FintechDestructive
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = if (antigravityState.isSuccess) "CONNECTED" else "ERROR",
                        type = if (antigravityState.isSuccess) StatusPillType.SUCCESS else StatusPillType.ERROR
                    )
                    Text(
                        text = antigravityState.testResult!!,
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                    )
                }
            }
        }

        // Section 5: Automatic Tool Approval Switch
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Tool Approval",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                    )
                    Text(
                        text = "Executes within isolated Linux container",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary
                    )
                }
                MintSwitch(
                    checked = antigravityState.autoToolApproval,
                    onCheckedChange = { viewModel.toggleAntigravityAutoApproval(it) }
                )
            }
        }

        // Section 6: Runtime Updates Row
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Runtime Updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                    )
                    Text(
                        text = updateStatusText,
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary
                    )
                }

                SecondaryPillButton(
                    text = if (isCheckingUpdates) "Checking..." else "Check Updates",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        isCheckingUpdates = true
                        updateStatusText = "Querying Antigravity registry..."
                        coroutineScope.launch {
                            delay(1200)
                            isCheckingUpdates = false
                            updateStatusText = "v2.4.2 (Up to date)"
                        }
                    },
                    height = 38.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Model selection bottom sheet
    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false },
            containerColor = if (isDark) DarkBgEnd else LightBgEnd,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SELECT REASONING MODEL",
                    style = FintechMicroLabel,
                    color = MintPrimary
                )

                availableModelsList.forEach { (modelId, modelName) ->
                    val isSelected = antigravityState.selectedModel == modelId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) MintPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) MintPrimary else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.setAntigravityModel(modelId)
                                showModelBottomSheet = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = modelName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Disconnect confirmation dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text(text = "Disconnect Antigravity?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Disconnecting will terminate your active cloud agent and reset your reasoning session tokens.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.disconnectAntigravity()
                    showDisconnectDialog = false
                }) {
                    Text(text = "Disconnect", color = FintechDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(text = "Cancel", color = MintPrimary)
                }
            },
            containerColor = if (isDark) DarkBgEnd else LightBgEnd,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
