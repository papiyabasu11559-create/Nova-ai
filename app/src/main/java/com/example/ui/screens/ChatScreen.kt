package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.ui.NovaAssistantViewModel
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
import com.example.util.FileTextExtractor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingText by viewModel.currentStreamingText.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val rmsDb by viewModel.rmsDb.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val recognizedText by viewModel.recognizedSpeechText.collectAsStateWithLifecycle()
    val isHandsFree by viewModel.isHandsFreeMode.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val attachedFile by viewModel.attachedFile.collectAsStateWithLifecycle()
    val activeThemeProposal by viewModel.activeThemeProposal.collectAsStateWithLifecycle()
    val isImeVisible = WindowInsets.isImeVisible

    var inputText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showAttachMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Stream partial speech recognition results live into the input field
    LaunchedEffect(recognizedText) {
        if (isListening && recognizedText.isNotBlank()) {
            inputText = recognizedText
        }
    }

    // Auto-scroll on new message, stream chunk, or when keyboard opens
    LaunchedEffect(messages.size, streamingText, isImeVisible) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            val targetIndex = (messages.size + (if (streamingText.isNotEmpty()) 1 else 0)) - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Photo picker launcher (Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
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

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
        }
    }

    // Document file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val res = FileTextExtractor.processUri(context, uri)
            res.fold(
                onSuccess = { file ->
                    viewModel.attachFile(file)
                    Toast.makeText(context, "Attached: ${file.name}", Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    Toast.makeText(context, "File Error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Status indicator banner if high server load (503 / 429)
        if (!connectionStatus.isNullOrBlank()) {
            Surface(
                color = VividViolet.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VividViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = VividViolet, strokeWidth = 2.dp)
                    Text(text = connectionStatus!!, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Active Theme Proposal Confirmation Card
        if (activeThemeProposal != null) {
            val proposal = activeThemeProposal!!
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(android.graphics.Color.parseColor(proposal.themeConfig.primaryColorHex))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color(android.graphics.Color.parseColor(proposal.themeConfig.primaryColorHex)))
                        Text(text = "Theme Proposal: ${proposal.title}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(text = proposal.description, fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.applyThemeProposal(proposal) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(proposal.themeConfig.primaryColorHex))),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Apply Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        OutlinedButton(
                            onClick = { viewModel.dismissThemeProposal() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Cancel", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            if (messages.isEmpty() && !isStreaming) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 32.sp)
                        }
                        Text(
                            text = "NOVA AI Online & Listening",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Ultra-low latency device execution & intelligence\nTap the microphone or type any command below",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        // Quick prompt suggestion chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            listOf("Turn on flashlight", "Check battery", "Summarize document", "Make a timer").forEach { chip ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable { viewModel.sendMessage(chip) }
                                ) {
                                    Text(
                                        text = chip,
                                        fontSize = 11.sp,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }

            // Live streaming bubble
            if (isStreaming) {
                item {
                    StreamingMessageBubble(streamingText = streamingText)
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Attached File Chip Preview
        if (attachedFile != null) {
            val file = attachedFile!!
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = ObsidianSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                        Column {
                            Text(text = file.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "${file.readableSize} • Document context loaded", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.removeAttachedFile() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove file", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Image Attachment Thumbnail Preview
        if (selectedBitmap != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = ObsidianSurfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected image preview",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Text(
                        text = "Image attached for multimodal analysis",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedBitmap = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove image", tint = TextMuted)
                    }
                }
            }
        }

        // Real-time audio waveform when mic is listening
        if (isListening) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = VividViolet.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VividViolet.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveWaveform(rmsDb = rmsDb)
                        Text(text = "Listening live...", fontSize = 12.sp, color = VividViolet, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { viewModel.stopVoiceInput() }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Stop Listening", tint = VividViolet)
                    }
                }
            }
        }

        // Bottom Input Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ObsidianSurface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Attach Button (+)
                Box {
                    IconButton(
                        onClick = { showAttachMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceVariant)
                            .border(1.dp, ObsidianBorder, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Attach file, photo or document", tint = ElectricCyan)
                    }

                    DropdownMenu(
                        expanded = showAttachMenu,
                        onDismissRequest = { showAttachMenu = false },
                        modifier = Modifier.background(ObsidianSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("📄 Document (PDF, Word, Code, CSV)", fontSize = 12.sp, color = TextPrimary) },
                            onClick = {
                                showAttachMenu = false
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "text/*",
                                        "application/json",
                                        "text/csv",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    )
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📷 Camera Capture", fontSize = 12.sp, color = TextPrimary) },
                            onClick = {
                                showAttachMenu = false
                                cameraLauncher.launch(null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🖼️ Photo Gallery", fontSize = 12.sp, color = TextPrimary) },
                            onClick = {
                                showAttachMenu = false
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )
                    }
                }

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening to voice..." else "Speak or type any command...",
                            fontSize = 13.sp,
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
                            if (inputText.isNotBlank() || selectedBitmap != null || attachedFile != null) {
                                val textToSend = inputText
                                val bitmapToSend = selectedBitmap
                                inputText = ""
                                selectedBitmap = null
                                viewModel.sendMessage(textToSend, bitmapToSend)
                            }
                        }
                    )
                )

                // Push-to-Talk Microphone Button
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isListening) VividViolet else ObsidianSurfaceVariant)
                        .border(1.dp, if (isListening) VividViolet else ObsidianBorder, CircleShape)
                        .testTag("push_to_talk_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Push to Talk Voice Assistant",
                        tint = if (isListening) TextPrimary else ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || selectedBitmap != null || attachedFile != null) {
                            val textToSend = inputText
                            val bitmapToSend = selectedBitmap
                            inputText = ""
                            selectedBitmap = null
                            viewModel.sendMessage(textToSend, bitmapToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() || selectedBitmap != null || attachedFile != null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() || selectedBitmap != null || attachedFile != null) ElectricCyan else ObsidianSurfaceVariant)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank() || selectedBitmap != null || attachedFile != null) ObsidianBackground else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) ElectricCyan.copy(alpha = 0.20f) else ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(
                width = 0.8.dp,
                color = if (isUser) ElectricCyan.copy(alpha = 0.5f) else ObsidianBorder
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.88f)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "⚡ NOVA AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                        if (message.toolStatus != null) {
                            StatusBadge(text = message.toolStatus, isSuccess = true)
                        }
                    }
                }

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun StreamingMessageBubble(streamingText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(0.90f)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = ElectricCyan,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "⚡ NOVA AI Live Thinking...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }

                Text(
                    text = if (streamingText.isEmpty()) "Processing tokens..." else streamingText,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
