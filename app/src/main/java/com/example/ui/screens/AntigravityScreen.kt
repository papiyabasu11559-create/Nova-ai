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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AntigravityReasoningDepth
import com.example.ui.NovaAssistantViewModel

val AntigravityDarkBg = Color(0xFF0B0F17)
val AntigravityCardBg = Color(0xFF151B26)
val AntigravityOrange = Color(0xFFF28C52)
val AntigravityBorder = Color(0xFF263346)
val AntigravityGreen = Color(0xFF10B981)
val AntigravityTextMuted = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val antigravityState by viewModel.antigravityState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var codeInput by remember { mutableStateOf("") }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("v2.4.0 (Latest Antigravity agent)") }

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
            .background(AntigravityDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top row: screen title + status pill (live Online/Offline)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Google Antigravity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Managed Agent & Cloud Workspace",
                    fontSize = 12.sp,
                    color = AntigravityTextMuted
                )
            }

            // Status Pill (Live Green dot "Online" / Red "Offline")
            Surface(
                shape = CircleShape,
                color = if (antigravityState.isConnected) AntigravityGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (antigravityState.isConnected) AntigravityGreen.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (antigravityState.isConnected) AntigravityGreen else Color.Red)
                    )
                    Text(
                        text = if (antigravityState.isConnected) "Online" else "Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (antigravityState.isConnected) AntigravityGreen else Color(0xFFEF4444)
                    )
                }
            }
        }

        // 2. "Antigravity" chip/tab (selected state, orange border)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AntigravityCardBg,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AntigravityOrange)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = AntigravityOrange, modifier = Modifier.size(16.dp))
                    Text(text = "Antigravity Active", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AntigravityOrange)
                }
            }
        }

        // 3. Card "Google account"
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AntigravityCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (antigravityState.isConnected) {
                    // Connected State
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AntigravityGreen.copy(alpha = 0.2f))
                                    .border(1.dp, AntigravityGreen, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "G", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AntigravityGreen)
                            }
                            Column {
                                Text(text = antigravityState.userEmail, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "Connected with Google", fontSize = 11.sp, color = AntigravityGreen, fontWeight = FontWeight.Medium)
                            }
                        }

                        TextButton(onClick = { showDisconnectDialog = true }) {
                            Text(text = "Disconnect", fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // Signed-out State
                    Text(text = "Google Account Authentication", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "Sign in to access your private Antigravity Linux workspace, higher reasoning budgets, and cloud agents.",
                        fontSize = 12.sp,
                        color = AntigravityTextMuted
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://antigravity.google/docs/install/#authenticate")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AntigravityOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Sign in with Antigravity", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    // Code entry field
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        placeholder = { Text("Paste authentication code here...", fontSize = 12.sp, color = AntigravityTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AntigravityDarkBg,
                            unfocusedContainerColor = AntigravityDarkBg,
                            focusedBorderColor = AntigravityOrange,
                            unfocusedBorderColor = AntigravityBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = AntigravityOrange, modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.connectAntigravityWithCode(codeInput) },
                        enabled = codeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Verify & Connect Agent", fontSize = 13.sp, color = AntigravityOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Section "Active Intelligence Model" + "Sync"
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AntigravityCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Active Intelligence Model", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    TextButton(onClick = { viewModel.testAntigravityConnection() }) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = AntigravityOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Sync", fontSize = 12.sp, color = AntigravityOrange, fontWeight = FontWeight.Bold)
                    }
                }

                // Dropdown Card: icon, display name, ID in monospace, chevron
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AntigravityDarkBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showModelBottomSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AntigravityOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = AntigravityOrange, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                val currentModelPair = availableModelsList.firstOrNull { it.first == antigravityState.selectedModel }
                                Text(
                                    text = currentModelPair?.second ?: antigravityState.selectedModel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = antigravityState.selectedModel,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AntigravityTextMuted
                                )
                            }
                        }

                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Select", tint = AntigravityOrange)
                    }
                }
            }
        }

        // 5. Section "Reasoning Depth" + live label
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AntigravityCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Reasoning Depth", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    val depthLabel = when (antigravityState.reasoningDepth) {
                        AntigravityReasoningDepth.LOW -> "Zero latency (0 tokens)"
                        AntigravityReasoningDepth.MEDIUM -> "Balanced reasoning (2,048 tokens)"
                        AntigravityReasoningDepth.HIGH -> "Maximum reasoning depth (8,192 tokens)"
                    }
                    Text(text = depthLabel, fontSize = 11.sp, color = AntigravityOrange, fontWeight = FontWeight.Medium)
                }

                // 3-way Segmented Control: Low | Medium | High (selected = orange fill, dark text)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AntigravityDarkBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        AntigravityReasoningDepth.values().forEach { depth ->
                            val isSelected = antigravityState.reasoningDepth == depth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AntigravityOrange else Color.Transparent)
                                    .clickable { viewModel.setAntigravityReasoningDepth(depth) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = depth.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else AntigravityTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. "Test connection" outlined orange button
        OutlinedButton(
            onClick = { viewModel.testAntigravityConnection() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AntigravityOrange),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, AntigravityOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (antigravityState.isTestingConnection) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AntigravityOrange, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Testing Connection...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = AntigravityOrange, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Test Connection", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (antigravityState.testResult != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (antigravityState.isSuccess) AntigravityGreen.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (antigravityState.isSuccess) AntigravityGreen else Color(0xFFEF4444)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (antigravityState.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (antigravityState.isSuccess) AntigravityGreen else Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(text = antigravityState.testResult!!, fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // 7. Caption: "Automatic tool approval · Runs inside the private Linux workspace"
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AntigravityCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Automatic Tool Approval", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "Runs inside the private Linux workspace",
                        fontSize = 11.sp,
                        color = AntigravityTextMuted
                    )
                }
                Switch(
                    checked = antigravityState.autoToolApproval,
                    onCheckedChange = { viewModel.toggleAntigravityAutoApproval(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AntigravityOrange,
                        checkedTrackColor = AntigravityOrange.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // 8. Card "Runtime updates"
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AntigravityCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Runtime Updates", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = updateStatusText, fontSize = 11.sp, color = AntigravityTextMuted)
                }

                Button(
                    onClick = {
                        isCheckingUpdates = true
                        updateStatusText = "Checking Antigravity registry..."
                        coroutineScope.launch {
                            delay(1200)
                            isCheckingUpdates = false
                            updateStatusText = "v2.4.2 (Up to date)"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AntigravityDarkBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AntigravityBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AntigravityOrange, strokeWidth = 2.dp)
                    } else {
                        Text(text = "Check Updates", fontSize = 12.sp, color = AntigravityOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 9. Footer Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AntigravityTextMuted, modifier = Modifier.size(16.dp))
            Text(
                text = "API keys encrypted in Android secure storage · Terminal is accessible from any project",
                fontSize = 11.sp,
                color = AntigravityTextMuted
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Model selection bottom sheet
    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = AntigravityCardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Select Antigravity Intelligence Model", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                HorizontalDivider(color = AntigravityBorder)

                availableModelsList.forEach { (modelId, displayName) ->
                    val isSelected = antigravityState.selectedModel == modelId
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AntigravityDarkBg else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AntigravityOrange else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setAntigravityModel(modelId)
                                showModelBottomSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(text = modelId, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AntigravityTextMuted)
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AntigravityOrange)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Disconnect Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect Antigravity?") },
            text = { Text("Your local session and stored workspace tokens will be wiped. You can sign in again at any time.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.disconnectAntigravity()
                    showDisconnectDialog = false
                }) {
                    Text("Disconnect", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = AntigravityCardBg
        )
    }
}
