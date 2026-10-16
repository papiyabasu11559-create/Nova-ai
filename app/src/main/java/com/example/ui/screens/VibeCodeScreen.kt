package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VibeCodeScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProject by viewModel.currentVibeProject.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingVibeCode.collectAsStateWithLifecycle()
    val streamText by viewModel.vibeCodeStreamText.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Preview, 1: Code
    var promptInput by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Toolbar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(text = "Vibe Coding Canvas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = currentProject.prompt.take(30) + if (currentProject.prompt.length > 30) "..." else "", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Undo button
                    if (currentProject.versionHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.undoVibeProject() }) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Undo", tint = VividViolet, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Copy code
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("VibeCode HTML", currentProject.htmlCode))
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    }

                    // Share/Export HTML
                    IconButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, currentProject.htmlCode)
                            putExtra(Intent.EXTRA_SUBJECT, "vibe_app.html")
                            type = "text/html"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Vibe App"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color(0xFFF28C52), modifier = Modifier.size(20.dp))
                    }

                    // Reload WebView
                    IconButton(onClick = {
                        webViewRef?.loadDataWithBaseURL(null, currentProject.htmlCode, "text/html", "UTF-8", null)
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Tabs: Preview | Code
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = ObsidianSurface,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = ElectricCyan
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "Live Sandbox Preview", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "HTML / CSS / JS Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                .background(if (selectedTabIndex == 0) Color.White else ObsidianSurfaceVariant)
        ) {
            if (selectedTabIndex == 0) {
                // Sandboxed WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(null, currentProject.htmlCode, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        view.loadDataWithBaseURL(null, currentProject.htmlCode, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Code View
                val codeToDisplay = if (isGenerating && streamText.isNotBlank()) streamText else currentProject.htmlCode
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = codeToDisplay,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ElectricCyan,
                        lineHeight = 16.sp
                    )
                }
            }

            // Generating indicator overlay
            if (isGenerating) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ElectricCyan, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚡ Vibe Coding in progress...",
                            color = ElectricCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Synthesizing self-contained responsive code",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Quick Preset Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Neon Synthwave Timer",
                "Retro Arcade Snake Game",
                "Interactive Calculator",
                "Personal Portfolio Card",
                "Particle Physics Visualizer"
            ).forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, ObsidianBorder),
                    modifier = Modifier.clickable {
                        promptInput = suggestion
                        viewModel.generateVibeProject(suggestion)
                    }
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Prompt Input Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            text = if (currentProject.htmlCode.isNotEmpty()) "Describe an edit or new app..." else "Describe a web app or tool...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (promptInput.isNotBlank()) {
                            val text = promptInput
                            promptInput = ""
                            viewModel.updateVibeProject(text)
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (promptInput.isNotBlank()) {
                            val text = promptInput
                            promptInput = ""
                            viewModel.updateVibeProject(text)
                        }
                    },
                    enabled = promptInput.isNotBlank() && !isGenerating
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (promptInput.isNotBlank() && !isGenerating) ElectricCyan else TextMuted
                    )
                }
            }
        }
    }
}
