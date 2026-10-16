package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ModelChoice
import com.example.overlay.FloatingAssistantService
import com.example.ui.NovaAssistantViewModel
import com.example.ui.NovaTab
import com.example.ui.components.FintechBackground
import com.example.ui.components.MintSwitch
import com.example.ui.screens.AntigravityScreen
import com.example.ui.screens.AutomationLogsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.screens.VibeCodeScreen
import com.example.ui.theme.DarkBgEnd
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.FintechMicroLabel
import com.example.ui.theme.FintechSectionTitle
import com.example.ui.theme.LightBgEnd
import com.example.ui.theme.LightBgStart
import com.example.ui.theme.LightGlassCard
import com.example.ui.theme.LightGlassCardBorder
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MintAccentGlow
import com.example.ui.theme.MintGradient
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NovaAiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: NovaAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleActionIntent(intent)

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            NovaAiTheme(darkTheme = !currentTheme.isLightTheme) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleActionIntent(intent)
    }

    private fun handleActionIntent(intent: Intent?) {
        val action = intent?.getStringExtra(FloatingAssistantService.EXTRA_ACTION) ?: return
        when (action) {
            FloatingAssistantService.ACTION_VOICE -> {
                viewModel.selectTab(NovaTab.CHAT)
                viewModel.startVoiceInput(onError = {})
            }
            FloatingAssistantService.ACTION_CHAT -> {
                viewModel.selectTab(NovaTab.CHAT)
            }
            FloatingAssistantService.ACTION_CAMERA -> {
                viewModel.selectTab(NovaTab.CHAT)
                try {
                    val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivity(cameraIntent)
                } catch (e: Exception) {
                    // Ignored if device lacks camera app
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppScreen(viewModel: NovaAssistantViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isHandsFree by viewModel.isHandsFreeMode.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val isImeVisible = WindowInsets.isImeVisible
    val isDark = !currentTheme.isLightTheme

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleHandsFreeMode(true) {}
        } else {
            Toast.makeText(context, "Microphone permission needed for hands-free", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle back button: close drawer first, then return to CHAT tab
    BackHandler(enabled = drawerState.isOpen || activeTab != NovaTab.CHAT) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.selectTab(NovaTab.CHAT)
        }
    }

    FintechBackground(themeConfig = currentTheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = if (isDark) DarkBgEnd.copy(alpha = 0.95f) else LightBgEnd.copy(alpha = 0.95f),
                    drawerContentColor = if (isDark) DarkTextPrimary else LightTextPrimary,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .border(
                            width = 1.dp,
                            color = if (isDark) DarkGlassCardBorder else LightGlassCardBorder
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Drawer Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(10.dp, CircleShape, spotColor = MintAccentGlow)
                                    .clip(CircleShape)
                                    .background(MintGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⚡", fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = "NOVA AI",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                                Text(
                                    text = "FINTECH GLASS SUITE",
                                    style = FintechMicroLabel,
                                    color = MintPrimary
                                )
                            }
                        }

                        HorizontalDivider(
                            color = if (isDark) DarkGlassCardBorder else LightGlassCardBorder,
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Navigation Drawer items: each 48dp tall with 20dp icon; active gets mint pill highlight
                        val navItems = listOf(
                            Triple(NovaTab.CHAT, "Chat", Icons.Default.ChatBubble),
                            Triple(NovaTab.FILES, "Files", Icons.Default.Folder),
                            Triple(NovaTab.VIBE_CODE, "Vibe Code", Icons.Default.Code),
                            Triple(NovaTab.THEMES, "Themes", Icons.Default.Palette),
                            Triple(NovaTab.AUTOMATION_LOGS, "Audit Logs", Icons.Default.History),
                            Triple(NovaTab.ANTIGRAVITY, "Antigravity Agent", Icons.Default.SmartToy),
                            Triple(NovaTab.SETTINGS, "Settings", Icons.Default.Settings)
                        )

                        navItems.forEach { (tab, label, icon) ->
                            val isSelected = activeTab == tab
                            val itemBg = if (isSelected) Modifier.background(MintGradient) else Modifier.background(Color.Transparent)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .then(itemBg)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = MintPrimary),
                                        onClick = {
                                            viewModel.selectTab(tab)
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) DarkBgEnd else (if (isDark) DarkTextSecondary else LightTextSecondary),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) DarkBgEnd else (if (isDark) DarkTextPrimary else LightTextPrimary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = if (isDark) DarkGlassCardBorder else LightGlassCardBorder,
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "QUICK CONTROLS",
                            style = FintechMicroLabel,
                            color = if (isDark) DarkTextMuted else LightTextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        // Hands-Free Mode Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) DarkGlassCard else LightGlassCard)
                                .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isHandsFree) MintPrimary else (if (isDark) DarkTextSecondary else LightTextSecondary),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Hands-Free Mode",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                                    )
                                    Text(
                                        text = if (isHandsFree) "Continuous listening" else "Push-to-talk",
                                        fontSize = 10.sp,
                                        color = if (isDark) DarkTextMuted else LightTextMuted
                                    )
                                }
                            }
                            MintSwitch(
                                checked = isHandsFree,
                                onCheckedChange = { enable ->
                                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (enable && !hasMic) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.toggleHandsFreeMode(enable) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            )
                        }

                        // Auto-Speak Responses Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) DarkGlassCard else LightGlassCard)
                                .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (settings.autoSpeakResponses) MintPrimary else (if (isDark) DarkTextSecondary else LightTextSecondary),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Auto-Speak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                                    )
                                    Text(
                                        text = "Speaks responses live",
                                        fontSize = 10.sp,
                                        color = if (isDark) DarkTextMuted else LightTextMuted
                                    )
                                }
                            }
                            MintSwitch(
                                checked = settings.autoSpeakResponses,
                                onCheckedChange = { viewModel.toggleAutoSpeak(it) }
                            )
                        }

                        // Model Selector Quick Tile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) DarkGlassCard else LightGlassCard)
                                .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    val choices = ModelChoice.values()
                                    val nextIndex = (choices.indexOf(settings.selectedModel) + 1) % choices.size
                                    viewModel.setModelChoice(choices[nextIndex])
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MintPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Gemini Model",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                                    )
                                    Text(
                                        text = settings.selectedModel.displayName,
                                        fontSize = 10.sp,
                                        color = MintPrimary
                                    )
                                }
                            }
                            Text(
                                text = "CYCLE",
                                style = FintechMicroLabel,
                                color = if (isDark) DarkTextMuted else LightTextMuted
                            )
                        }

                        // Clear Chat Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Transparent)
                                .border(1.dp, Color(0xFFFF5A5F).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.clearHistory()
                                    Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = Color(0xFFFF5A5F),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Clear Chat History",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF5A5F)
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    // Header: logo + title + hamburger only
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) DarkBgStart.copy(alpha = 0.75f) else LightBgStart.copy(alpha = 0.85f))
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Logo + Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .shadow(8.dp, CircleShape, spotColor = MintAccentGlow)
                                        .clip(CircleShape)
                                        .background(MintGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "⚡", fontSize = 17.sp)
                                }
                                Text(
                                    text = "NOVA AI",
                                    style = FintechSectionTitle,
                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                            }

                            // Hamburger Menu Icon
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) DarkGlassCard else LightGlassCard)
                                    .border(1.dp, if (isDark) DarkGlassCardBorder else LightGlassCardBorder, CircleShape)
                                    .testTag("hamburger_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Navigation Menu",
                                    tint = MintPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    // Floating glass pill bottom bar with raised circular center button
                    if (!isImeVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Floating Glass Pill Container
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(32.dp),
                                        spotColor = if (isDark) Color(0x66000000) else Color(0x1F103C2D)
                                    )
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(if (isDark) DarkGlassCard else LightGlassCard)
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) DarkGlassCardBorder else LightGlassCardBorder,
                                        shape = RoundedCornerShape(32.dp)
                                    )
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // 1. Chat Tab
                                FloatingNavItem(
                                    label = "Chat",
                                    icon = Icons.Default.ChatBubble,
                                    selected = activeTab == NovaTab.CHAT,
                                    onClick = { viewModel.selectTab(NovaTab.CHAT) }
                                )

                                // 2. Files Tab
                                FloatingNavItem(
                                    label = "Files",
                                    icon = Icons.Default.Folder,
                                    selected = activeTab == NovaTab.FILES,
                                    onClick = { viewModel.selectTab(NovaTab.FILES) }
                                )

                                // Center spacer for raised button
                                Spacer(modifier = Modifier.width(52.dp))

                                // 3. Audit Logs Tab
                                FloatingNavItem(
                                    label = "Audit",
                                    icon = Icons.Default.History,
                                    selected = activeTab == NovaTab.AUTOMATION_LOGS,
                                    onClick = { viewModel.selectTab(NovaTab.AUTOMATION_LOGS) }
                                )

                                // 4. Settings Tab
                                FloatingNavItem(
                                    label = "Settings",
                                    icon = Icons.Default.Settings,
                                    selected = activeTab == NovaTab.SETTINGS,
                                    onClick = { viewModel.selectTab(NovaTab.SETTINGS) }
                                )
                            }

                            // Raised Circular Center Action Button (Mint Gradient with soft glow)
                            Box(
                                modifier = Modifier
                                    .offset(y = (-14).dp)
                                    .size(56.dp)
                                    .shadow(16.dp, CircleShape, spotColor = MintAccentGlow)
                                    .clip(CircleShape)
                                    .background(MintGradient)
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = Color.White),
                                        onClick = {
                                            viewModel.selectTab(NovaTab.CHAT)
                                            viewModel.startVoiceInput(onError = {})
                                        }
                                    )
                                    .testTag("center_action_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⚡", fontSize = 24.sp)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                ) {
                    when (activeTab) {
                        NovaTab.CHAT -> ChatScreen(viewModel = viewModel)
                        NovaTab.FILES -> FilesScreen(viewModel = viewModel)
                        NovaTab.VIBE_CODE -> VibeCodeScreen(viewModel = viewModel)
                        NovaTab.THEMES -> ThemesScreen(viewModel = viewModel)
                        NovaTab.AUTOMATION_LOGS -> AutomationLogsScreen(viewModel = viewModel)
                        NovaTab.ANTIGRAVITY -> AntigravityScreen(viewModel = viewModel)
                        NovaTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = !LocalIsDarkTheme.current.not()

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MintPrimary),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MintPrimary else (if (isDark) DarkTextMuted else LightTextMuted),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MintPrimary else (if (isDark) DarkTextMuted else LightTextMuted)
        )
    }
}
