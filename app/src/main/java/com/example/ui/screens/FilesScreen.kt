package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.NovaAssistantViewModel
import com.example.ui.NovaTab
import com.example.ui.components.GlassCard
import com.example.ui.components.PillChip
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusPillType
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
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
import com.example.util.FileTextExtractor

@Composable
fun FilesScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val attachedFile by viewModel.attachedFile.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = FileTextExtractor.processUri(context, uri)
            result.fold(
                onSuccess = { file ->
                    viewModel.attachFile(file)
                    Toast.makeText(context, "Loaded ${file.name} (${file.readableSize})", Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Hero Card
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
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = DarkBgStart, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Document & File Reader",
                            style = FintechSectionTitle,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        Text(
                            text = "LOCAL ON-DEVICE CONTEXT",
                            style = FintechMicroLabel,
                            color = MintPrimary
                        )
                    }
                }

                StatusPill(
                    text = if (attachedFile != null) "FILE LOADED" else "READY",
                    type = if (attachedFile != null) StatusPillType.SUCCESS else StatusPillType.INFO
                )
            }
        }

        // Active Attachment Glass Card
        if (attachedFile != null) {
            val file = attachedFile!!
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderTint = MintPrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintPrimary.copy(alpha = 0.2f))
                                .border(1.dp, MintPrimary, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${file.readableSize} • ${file.mimeType}",
                                fontSize = 11.sp,
                                color = if (isDark) DarkTextSecondary else LightTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.removeAttachedFile() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) DarkGlassCard else LightGlassCard)
                            .border(1.dp, FintechDestructive.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = FintechDestructive, modifier = Modifier.size(16.dp))
                    }
                }

                // Extracted Text Preview
                if (!file.extractedText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A103C2D))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "PARSED TEXT STREAM:",
                                style = FintechMicroLabel,
                                color = MintPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = file.extractedText!!.take(500) + if (file.extractedText!!.length > 500) "\n... [more content extracted]" else "",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) DarkTextMuted else LightTextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "DISPATCH QUESTIONS TO NOVA AI:",
                    style = FintechMicroLabel,
                    color = if (isDark) DarkTextMuted else LightTextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Summarize file",
                        "Key findings",
                        "Explain code"
                    ).forEach { query ->
                        Box(modifier = Modifier.weight(1f)) {
                            PillChip(
                                label = query,
                                selected = false,
                                onClick = {
                                    viewModel.selectTab(NovaTab.CHAT)
                                    viewModel.sendMessage(query)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Upload Button Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(12.dp, CircleShape, spotColor = MintAccentGlow)
                        .clip(CircleShape)
                        .background(MintGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = DarkBgStart, modifier = Modifier.size(26.dp))
                }

                Text(
                    text = "Attach & Analyze Documents",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )

                Text(
                    text = "Select any document from local storage to provide immediate grounded context to Gemini reasoning models.",
                    style = FintechBody,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                PrimaryPillButton(
                    text = "Choose Document (SAF Picker)",
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "text/*",
                                "application/json",
                                "text/csv",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            )
                        )
                    },
                    height = 50.dp
                )
            }
        }

        // Supported Formats Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SUPPORTED DOCUMENT TYPES",
                style = FintechMicroLabel,
                color = if (isDark) DarkTextMuted else LightTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            val formats = listOf(
                "PDF" to "Embedded text & metadata",
                "DOCX" to "Word XML paragraphs",
                "CSV / JSON" to "Data sheets & schemas",
                "TXT / MD" to "Plaintext & documentation",
                "CODE" to "Kotlin, TS, Python, C++, HTML"
            )

            formats.forEach { (title, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintPrimary
                    )
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
