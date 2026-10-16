package com.example.ui.screens

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.ui.NovaAssistantViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveWaveform
import com.example.ui.components.PillChip
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.SecondaryPillButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusPillType
import com.example.ui.theme.DarkBgEnd
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightBgEnd
import com.example.ui.theme.LightBgStart
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FintechBody
import com.example.ui.theme.FintechMicroLabel
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
import com.example.util.FileTextExtractor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = LocalIsDarkTheme.current

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingText by viewModel.currentStreamingText.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val rmsDb by viewModel.rmsDb.collectAsStateWithLifecycle()
    val recognizedText by viewModel.recognizedSpeechText.collectAsStateWithLifecycle()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(CircleShape)
                    .background(if (isDark) DarkGlassCard else LightGlassCard)
                    .border(1.dp, MintPrimary.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MintPrimary, strokeWidth = 2.dp)
                Text(
                    text = connectionStatus!!,
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Active Theme Proposal Confirmation Card
        if (activeThemeProposal != null) {
            val proposal = activeThemeProposal!!
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                borderTint = MintPrimary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(text = "PROPOSED THEME", type = StatusPillType.SUCCESS)
                    Text(
                        text = proposal.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = proposal.description,
                    style = FintechBody,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryPillButton(
                        text = "Apply Theme",
                        onClick = { viewModel.applyThemeProposal(proposal) },
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                    SecondaryPillButton(
                        text = "Dismiss",
                        onClick = { viewModel.dismissThemeProposal() },
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                }
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // Empty state hero with Fintech design language
            if (messages.isEmpty() && !isStreaming) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .shadow(16.dp, CircleShape, spotColor = MintAccentGlow)
                                    .clip(CircleShape)
                                    .background(MintGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⚡", fontSize = 26.sp)
                            }

                            Text(
                                text = "NOVA AI ASSISTANT",
                                style = FintechMicroLabel,
                                color = MintPrimary
                            )

                            Text(
                                text = "Autonomous Intelligence",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                            )

                            Text(
                                text = "Execute on-device hardware actions, summarize complex files, generate live web code, or chat with Gemini reasoning.",
                                style = FintechBody,
                                color = if (isDark) DarkTextSecondary else LightTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Horizontal scrollable quick suggestion chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "⚡ Turn on flashlight",
                                    "📊 Check memory specs",
                                    "⏰ Set timer 5 minutes",
                                    "🎨 Switch to Solar Flame theme",
                                    "📁 Read and summarize file"
                                ).forEach { prompt ->
                                    PillChip(
                                        label = prompt,
                                        selected = false,
                                        onClick = { viewModel.sendMessage(prompt) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Message list items
            items(messages, key = { it.id }) { message ->
                FintechMessageItem(message = message)
            }

            // Live streaming bubble
            if (isStreaming) {
                item {
                    FintechStreamingItem(streamingText = streamingText)
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Attached Document File Preview
        if (attachedFile != null) {
            val file = attachedFile!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) DarkGlassCard else LightGlassCard)
                    .border(1.dp, MintPrimary.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${file.name} (${file.readableSize})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = { viewModel.removeAttachedFile() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove file",
                        tint = Color(0xFFFF5A5F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Multimodal Image Attachment Preview
        if (selectedBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) DarkGlassCard else LightGlassCard)
                    .border(1.dp, MintPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected image preview",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Text(
                    text = "Multimodal image attached for analysis",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { selectedBitmap = null },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove image",
                        tint = if (isDark) DarkTextMuted else LightTextMuted
                    )
                }
            }
        }

        // Live Audio Waveform when Voice Recognition is Active
        if (isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(CircleShape)
                    .background(MintPrimary.copy(alpha = 0.12f))
                    .border(1.dp, MintPrimary.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiveWaveform(rmsDb = rmsDb)
                    Text(
                        text = "LISTENING LIVE",
                        style = FintechMicroLabel,
                        color = MintPrimary
                    )
                }
                IconButton(
                    onClick = { viewModel.stopVoiceInput() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Listening",
                        tint = MintPrimary
                    )
                }
            }
        }

        // Floating Glass Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .shadow(elevation = 14.dp, shape = CircleShape, spotColor = if (isDark) Color(0x66000000) else Color(0x1F103C2D))
                .clip(CircleShape)
                .background(if (isDark) DarkGlassCard else LightGlassCard)
                .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Attach Button (+)
                Box {
                    IconButton(
                        onClick = { showAttachMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F103C2D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = MintPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAttachMenu,
                        onDismissRequest = { showAttachMenu = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) DarkBgEnd else LightBgEnd)
                            .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MintPrimary) },
                            text = { Text("Document (PDF, Word, Code, CSV)", fontSize = 13.sp, color = if (isDark) DarkTextPrimary else LightTextPrimary) },
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
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MintPrimary) },
                            text = { Text("Camera Capture", fontSize = 13.sp, color = if (isDark) DarkTextPrimary else LightTextPrimary) },
                            onClick = {
                                showAttachMenu = false
                                cameraLauncher.launch(null)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MintPrimary) },
                            text = { Text("Photo Gallery", fontSize = 13.sp, color = if (isDark) DarkTextPrimary else LightTextPrimary) },
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

                // Text Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening to speech..." else "Ask NOVA anything...",
                            fontSize = 13.sp,
                            color = if (isDark) DarkTextMuted else LightTextMuted
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = if (isDark) DarkTextPrimary else LightTextPrimary,
                        unfocusedTextColor = if (isDark) DarkTextPrimary else LightTextPrimary,
                        cursorColor = MintPrimary
                    ),
                    maxLines = 3,
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

                // Push-to-Talk Mic Button
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isListening) MintPrimary else (if (isDark) Color(0x1AFFFFFF) else Color(0x0F103C2D)))
                        .testTag("push_to_talk_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Push to Talk",
                        tint = if (isListening) DarkBgStart else MintPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Send Button (Mint Gradient Pill)
                val canSend = inputText.isNotBlank() || selectedBitmap != null || attachedFile != null
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(
                            elevation = if (canSend) 8.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = MintAccentGlow
                        )
                        .clip(CircleShape)
                        .background(
                            if (canSend) MintGradient else Brush.linearGradient(
                                listOf(
                                    if (isDark) Color(0x1AFFFFFF) else Color(0x0F103C2D),
                                    if (isDark) Color(0x1AFFFFFF) else Color(0x0F103C2D)
                                )
                            )
                        )
                        .clickable(
                            enabled = canSend,
                            onClick = {
                                if (canSend) {
                                    val textToSend = inputText
                                    val bitmapToSend = selectedBitmap
                                    inputText = ""
                                    selectedBitmap = null
                                    viewModel.sendMessage(textToSend, bitmapToSend)
                                }
                            }
                        )
                        .testTag("send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) DarkBgStart else (if (isDark) DarkTextMuted else LightTextMuted),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// Fintech Message Bubble Rendering (User Glass Pill & NOVA Full Glass Card)
// =========================================================================

@Composable
fun FintechMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    val isDark = LocalIsDarkTheme.current

    if (isUser) {
        // User message: right-aligned glass pill with faint mint border
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 22.dp,
                            topEnd = 22.dp,
                            bottomStart = 22.dp,
                            bottomEnd = 6.dp
                        )
                    )
                    .background(if (isDark) Color(0x1A34D399) else Color(0x1410B981))
                    .border(
                        width = 1.dp,
                        color = MintPrimary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(
                            topStart = 22.dp,
                            topEnd = 22.dp,
                            bottomStart = 22.dp,
                            bottomEnd = 6.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.content,
                    style = FintechBody,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
            }
        }
    } else {
        // NOVA AI reply: full-width Glass Card with avatar chip, name row, and status pill
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderTint = if (message.toolStatus != null) MintPrimary.copy(alpha = 0.5f) else null
        ) {
            // Header Row: Avatar chip + Name + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .shadow(6.dp, CircleShape, spotColor = MintAccentGlow)
                            .clip(CircleShape)
                            .background(MintGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⚡", fontSize = 14.sp)
                    }

                    Column {
                        Text(
                            text = "NOVA AI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                    }
                }

                if (message.toolStatus != null) {
                    val statusType = when (message.toolStatus.uppercase()) {
                        "SUCCESS" -> StatusPillType.SUCCESS
                        "FAILED" -> StatusPillType.ERROR
                        "PENDING" -> StatusPillType.WARNING
                        else -> StatusPillType.INFO
                    }
                    StatusPill(text = message.toolStatus, type = statusType)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text
            Text(
                text = message.content,
                style = FintechBody,
                color = if (isDark) DarkTextPrimary else LightTextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun FintechStreamingItem(streamingText: String) {
    val isDark = LocalIsDarkTheme.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderTint = MintPrimary,
        isGlowActive = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MintPrimary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "NOVA AI GENERATING",
                    style = FintechMicroLabel,
                    color = MintPrimary
                )
            }

            StatusPill(text = "STREAMING", type = StatusPillType.INFO)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (streamingText.isBlank()) "Thinking and formulating response..." else streamingText,
            style = FintechBody,
            color = if (isDark) DarkTextPrimary else LightTextPrimary,
            lineHeight = 20.sp
        )
    }
}
