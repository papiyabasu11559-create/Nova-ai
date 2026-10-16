package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.AutoFixHigh
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.GlassCard
import com.example.ui.components.PillChip
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.SegmentedPillControl
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusPillType
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VibeCodeScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val currentProject by viewModel.currentVibeProject.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingVibeCode.collectAsStateWithLifecycle()
    val streamText by viewModel.vibeCodeStreamText.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Preview, 1: Code
    var promptInput by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Toolbar Card
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(8.dp, CircleShape, spotColor = MintAccentGlow)
                            .clip(CircleShape)
                            .background(MintGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = DarkBgStart, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Vibe Code Engine",
                            style = FintechSectionTitle,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        Text(
                            text = currentProject.prompt.take(28) + if (currentProject.prompt.length > 28) "..." else "",
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else LightTextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Undo button
                    if (currentProject.versionHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.undoVibeProject() }) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Undo", tint = MintPrimary, modifier = Modifier.size(19.dp))
                        }
                    }

                    // Copy code
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("VibeCode HTML", currentProject.htmlCode))
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = MintPrimary, modifier = Modifier.size(19.dp))
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
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = MintPrimary, modifier = Modifier.size(19.dp))
                    }

                    // Reload WebView
                    IconButton(onClick = {
                        webViewRef?.loadDataWithBaseURL(null, currentProject.htmlCode, "text/html", "UTF-8", null)
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = if (isDark) DarkTextMuted else LightTextMuted, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }

        // Preview | Code Segmented Pill Control
        SegmentedPillControl(
            options = listOf("⚡ Live Sandbox Preview", "💻 HTML/CSS/JS Source"),
            selectedIndex = selectedTabIndex,
            onSelect = { selectedTabIndex = it }
        )

        // Main Sandbox Canvas Area: Framed in a Glass Panel with Hairline Mint Border (24dp radius)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = MintAccentGlow)
                .clip(RoundedCornerShape(24.dp))
                .background(if (selectedTabIndex == 0) Color.White else (if (isDark) DarkGlassCard else LightGlassCard))
                .border(1.2.dp, MintPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
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
                            tag = currentProject.htmlCode
                            loadDataWithBaseURL(null, currentProject.htmlCode, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        if (view.tag != currentProject.htmlCode) {
                            view.tag = currentProject.htmlCode
                            view.loadDataWithBaseURL(null, currentProject.htmlCode, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Code View
                val codeToDisplay = if (isGenerating && streamText.isNotBlank()) streamText else currentProject.htmlCode
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = codeToDisplay,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (isDark) MintPrimary else Color(0xFF0F5132),
                        lineHeight = 18.sp
                    )
                }
            }

            // Live Generating Overlay
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.70f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = MintPrimary, strokeWidth = 3.dp)
                        Text(
                            text = "SYNTHESIZING VIBE APP",
                            style = FintechMicroLabel,
                            color = MintPrimary
                        )
                        Text(
                            text = "Assembling responsive web components",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Horizontal Scrollable Prompt Suggestion Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "⚡ Neon Synthwave Timer",
                "🕹️ Retro Arcade Snake Game",
                "📈 Fintech Crypto Tracker",
                "🧮 Glass Calculator",
                "🌌 Particle Physics Visualizer"
            ).forEach { suggestion ->
                PillChip(
                    label = suggestion,
                    selected = false,
                    onClick = {
                        promptInput = suggestion
                        viewModel.generateVibeProject(suggestion)
                    }
                )
            }
        }

        // Prompt Input Row: Floating Glass Pill with Send Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(if (isDark) DarkGlassCard else LightGlassCard)
                .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = {
                    Text(
                        text = if (currentProject.htmlCode.isNotEmpty()) "Describe an edit (e.g. make cards emerald)..." else "Describe a web app or tool...",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextMuted else LightTextMuted
                    )
                },
                modifier = Modifier.weight(1f),
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
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (promptInput.isNotBlank()) {
                        val text = promptInput
                        promptInput = ""
                        viewModel.updateVibeProject(text)
                    }
                })
            )

            val canSend = promptInput.isNotBlank() && !isGenerating
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(if (canSend) 8.dp else 0.dp, CircleShape, spotColor = MintAccentGlow)
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
                                val text = promptInput
                                promptInput = ""
                                viewModel.updateVibeProject(text)
                            }
                        }
                    ),
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
