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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.NovaAssistantViewModel
import com.example.ui.NovaTab
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet
import com.example.util.FileTextExtractor

@Composable
fun FilesScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(text = "Document & File Reader", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "PDF, DOCX, TXT, CSV, JSON, Markdown & Code analyzer", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Active Attachment Card
        if (attachedFile != null) {
            val file = attachedFile!!
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text(text = file.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "${file.readableSize} • ${file.mimeType}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = { viewModel.removeAttachedFile() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444))
                        }
                    }

                    // Extracted Preview
                    if (!file.extractedText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Extracted Text Preview:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ElectricCyan)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = file.extractedText!!.take(600) + if (file.extractedText!!.length > 600) "\n... [more content extracted]" else "",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Quick action prompt chips
                    Text(text = "Ask NOVA AI about this file:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Summarize this file",
                            "Explain this code",
                            "Find key takeaways"
                        ).forEach { query ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ObsidianSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.selectTab(NovaTab.CHAT)
                                        viewModel.sendMessage(query)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = query, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Upload Button Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(VividViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = VividViolet, modifier = Modifier.size(28.dp))
                }

                Text(
                    text = "Attach & Analyze Documents",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Select any PDF, Word (.docx), text file, CSV spreadsheet, JSON dataset, or code source file from your phone storage (up to 20MB).",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )

                Button(
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
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Choose Document File", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        // Supported Features List
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "File Intelligence Features", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "• Local zero-latency text extraction for DOCX, CSV, TXT & JSON", fontSize = 11.sp, color = TextSecondary)
                Text(text = "• Document preview rendering & Gemini multimodal visual analysis", fontSize = 11.sp, color = TextSecondary)
                Text(text = "• Large files automatically chunked safely up to 30,000 characters", fontSize = 11.sp, color = TextSecondary)
                Text(text = "• Instant synthesis, explanation, translation, or data extraction in Chat", fontSize = 11.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
