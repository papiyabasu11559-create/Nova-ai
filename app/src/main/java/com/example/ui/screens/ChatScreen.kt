package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.ui.NovaAssistantViewModel
import com.example.ui.NovaTab
import com.example.ui.components.LiveWaveform
import com.example.ui.components.PulsingAIOld
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ElectricCyan
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingText by viewModel.currentStreamingText.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val rmsDb by viewModel.rmsDb.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isHandsFree by viewModel.isHandsFreeMode.collectAsStateWithLifecycle()
    val isImeVisible = WindowInsets.isImeVisible

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val listState = rememberLazyListState()

    // Auto-scroll on new message, stream chunk, or when keyboard opens
    LaunchedEffect(messages.size, streamingText, isImeVisible) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            val targetIndex = (messages.size + (if (streamingText.isNotEmpty()) 1 else 0)) - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Photo picker launcher (zero-permission standard Android photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Audio permission launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startVoiceInput(onError = {})
        }
    }

    val activeModelDisplayName = settings.autoDetectedModelName.ifBlank {
        settings.selectedModel.displayName
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ObsidianSurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulsingAIOld(isListening = isListening, isStreaming = isStreaming)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NOVA AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeModelDisplayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricCyan
                                )
                            }
                        }
                        Text(
                            text = if (isListening) "Listening to your voice..." else if (isStreaming) "AI Reasoning & Executing..." else "AI Ready • Awaiting human input",
                            fontSize = 11.sp,
                            color = if (isListening) ElectricCyan else TextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isHandsFree) SuccessTeal.copy(alpha = 0.2f) else ObsidianSurfaceVariant)
                            .border(1.dp, if (isHandsFree) SuccessTeal else ObsidianBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.toggleHandsFreeMode(!isHandsFree, onPermissionRequired = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                })
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("hands_free_mode_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = if (isHandsFree) "🟢" else "🎙️", fontSize = 11.sp)
                            Text(
                                text = if (isHandsFree) "Hands-Free" else "Hands-Free",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHandsFree) SuccessTeal else TextSecondary
                            )
                        }
                    }

                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier.testTag("stop_speaking_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speaking - Tap to stop",
                                tint = VividViolet
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Optional API Key Setup Banner if key is missing
        if (settings.customApiKey.isBlank() && !settings.isKeyValidated) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = ObsidianSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, VividViolet.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = VividViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Configure Google API Key",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Auto-detects compatible Gemini model for your account",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.selectTab(NovaTab.SETTINGS) },
                        colors = ButtonDefaults.buttonColors(containerColor = VividViolet),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Setup", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Chat Messages Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && streamingText.isEmpty()) {
                item {
                    EmptyChatGreeting()
                }
            }

            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }

            if (streamingText.isNotEmpty()) {
                item {
                    StreamingMessageBubble(text = streamingText)
                }
            }
        }

        // Voice Waveform Banner (Active for Push-to-Talk or Continuous Hands-Free)
        AnimatedVisibility(
            visible = isListening || isHandsFree,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isHandsFree) SuccessTeal.copy(alpha = 0.7f) else ElectricCyan.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isHandsFree) {
                                if (isListening) "Listening..." else "🎙️ Hands-Free • Speak anytime"
                            } else {
                                "Listening to you..."
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isHandsFree) SuccessTeal else ElectricCyan
                        )
                        LiveWaveform(rmsDb = rmsDb)
                    }

                    IconButton(
                        onClick = {
                            if (isHandsFree) {
                                viewModel.toggleHandsFreeMode(false, {})
                            } else {
                                viewModel.stopVoiceInput()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Listening",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Image Attachment Preview
        if (selectedBitmap != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected image preview",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Text(
                    text = "Image attached for AI analysis",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        selectedBitmap = null
                        selectedImageUri = null
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attached image",
                        tint = TextMuted
                    )
                }
            }
        }

        // Bottom Input Row: Human Input Interface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ObsidianSurface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Photo Attachment Button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.testTag("attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach image for AI analysis",
                        tint = if (selectedBitmap != null) ElectricCyan else TextSecondary
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Speak or type any command...",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurfaceVariant,
                        unfocusedContainerColor = ObsidianSurfaceVariant,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() || selectedBitmap != null) {
                                val textToSend = inputText
                                val bitmapToSend = selectedBitmap
                                inputText = ""
                                selectedBitmap = null
                                selectedImageUri = null
                                viewModel.sendMessage(textToSend, bitmapToSend)
                            }
                        }
                    )
                )

                // Push-to-Talk Voice Button
                IconButton(
                    onClick = {
                        if (isListening) {
                            viewModel.stopVoiceInput()
                        } else {
                            if (hasMicPermission) {
                                viewModel.startVoiceInput(onError = {})
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) VividViolet else ObsidianSurfaceVariant)
                        .border(1.dp, if (isListening) VividViolet else ObsidianBorder, CircleShape)
                        .testTag("push_to_talk_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Push to Talk Voice Assistant",
                        tint = if (isListening) TextPrimary else ElectricCyan
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || selectedBitmap != null) {
                            val textToSend = inputText
                            val bitmapToSend = selectedBitmap
                            inputText = ""
                            selectedBitmap = null
                            selectedImageUri = null
                            viewModel.sendMessage(textToSend, bitmapToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() || selectedBitmap != null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() || selectedBitmap != null) ElectricCyan else ObsidianSurfaceVariant)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank() || selectedBitmap != null) ObsidianBackground else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) ObsidianSurfaceElevated else ObsidianSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isUser) VividViolet.copy(alpha = 0.4f) else ObsidianBorder
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag(if (isUser) "user_message_bubble" else "model_message_bubble")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else "NOVA AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUser) VividViolet else ElectricCyan
                    )
                    Text(
                        text = formatTimestamp(message.timestamp),
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Content Text
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary
                )

                // Tool Execution Block (if tool was invoked by AI)
                if (message.toolCallName != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ToolExecutionCard(
                        toolName = message.toolCallName,
                        toolArgs = message.toolCallArgs ?: "",
                        toolResult = message.toolResult ?: "",
                        toolStatus = message.toolStatus ?: "SUCCESS"
                    )
                }
            }
        }
    }
}

@Composable
fun ToolExecutionCard(
    toolName: String,
    toolArgs: String,
    toolResult: String,
    toolStatus: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ObsidianSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "⚡", fontSize = 12.sp)
                    Text(
                        text = "Native Action: $toolName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
                StatusBadge(
                    text = toolStatus,
                    isSuccess = toolStatus == "SUCCESS" || toolStatus == "DISPATCHED"
                )
            }

            if (toolResult.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = toolResult,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (toolArgs.isNotBlank() && toolArgs != "{}") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Args: $toolArgs",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun StreamingMessageBubble(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulsingAIOld(isStreaming = true)
                    Text(
                        text = "NOVA AI • Reasoning",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$text ▋",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun EmptyChatGreeting() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(ObsidianSurfaceElevated)
                .border(2.dp, ElectricCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⚡", fontSize = 38.sp)
        }

        Text(
            text = "NOVA AI",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Direct human-to-AI interaction.\nSpeak or type any command naturally — NOVA AI will reason, call native device tools (timers, alarms, calendar, apps, calls, SMS, battery), and execute the task automatically.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return sdf.format(date)
}
