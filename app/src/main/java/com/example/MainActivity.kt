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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.screens.AntigravityScreen
import com.example.ui.screens.AutomationLogsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.screens.VibeCodeScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NovaAiTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: NovaAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleActionIntent(intent)

        setContent {
            NovaAiTheme {
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
    val isImeVisible = WindowInsets.isImeVisible

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ObsidianSurface,
                drawerContentColor = TextPrimary,
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = ObsidianBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "NOVA AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Next-Gen Autonomous Agent",
                                fontSize = 11.sp,
                                color = ElectricCyan
                            )
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Navigation items
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
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) ElectricCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                viewModel.selectTab(tab)
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.15f),
                                unselectedContainerColor = Color.Transparent,
                                selectedTextColor = ElectricCyan,
                                unselectedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "QUICK CONTROLS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Hands-Free Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isHandsFree) ElectricCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(text = "Hands-Free", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = if (isHandsFree) "Continuous listening" else "Push-to-talk",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Switch(
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
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ElectricCyan,
                                    checkedTrackColor = ElectricCyan.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.size(width = 44.dp, height = 24.dp)
                            )
                        }
                    }

                    // Auto-Speak Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (settings.autoSpeakResponses) VividViolet else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(text = "Auto-Speak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "Voice responses live", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                            Switch(
                                checked = settings.autoSpeakResponses,
                                onCheckedChange = { viewModel.toggleAutoSpeak(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VividViolet,
                                    checkedTrackColor = VividViolet.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.size(width = 44.dp, height = 24.dp)
                            )
                        }
                    }

                    // Model Selector Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val choices = ModelChoice.values()
                                val nextIndex = (choices.indexOf(settings.selectedModel) + 1) % choices.size
                                viewModel.setModelChoice(choices[nextIndex])
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(text = "Model", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = settings.selectedModel.displayName, fontSize = 10.sp, color = ElectricCyan)
                                }
                            }
                            Text(text = "TAP TO CYCLE", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Clear Chat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.clearHistory()
                                Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch { drawerState.close() }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Clear Chat History",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBackground,
            topBar = {
                // Header: logo + title + hamburger only
                Surface(
                    color = ObsidianSurface,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 0.8.dp,
                        color = ObsidianBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⚡", fontSize = 16.sp)
                            }
                            Text(
                                text = "NOVA AI",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            },
                            modifier = Modifier.testTag("hamburger_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = ElectricCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!isImeVisible) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.8.dp,
                                color = ObsidianBorder
                            )
                    ) {
                        NavigationBarItem(
                            selected = activeTab == NovaTab.CHAT,
                            onClick = { viewModel.selectTab(NovaTab.CHAT) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Chat Assistant"
                                )
                            },
                            label = { Text("Chat", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = ElectricCyan,
                                indicatorColor = ElectricCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_chat")
                        )

                        NavigationBarItem(
                            selected = activeTab == NovaTab.AUTOMATION_LOGS,
                            onClick = { viewModel.selectTab(NovaTab.AUTOMATION_LOGS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Automation Audit Logs"
                                )
                            },
                            label = { Text("Audit", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = VividViolet,
                                indicatorColor = VividViolet,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_logs")
                        )

                        NavigationBarItem(
                            selected = activeTab == NovaTab.SETTINGS,
                            onClick = { viewModel.selectTab(NovaTab.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = ElectricCyan,
                                indicatorColor = ElectricCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
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
