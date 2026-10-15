package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NovaAssistantViewModel
import com.example.ui.NovaTab
import com.example.ui.screens.AutomationLogsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NovaAiTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VividViolet

import android.content.Intent
import com.example.overlay.FloatingAssistantService

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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MainAppScreen(viewModel: NovaAssistantViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isImeVisible = WindowInsets.isImeVisible

    // Handle back button to return to CHAT tab
    BackHandler(enabled = activeTab != NovaTab.CHAT) {
        viewModel.selectTab(NovaTab.CHAT)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
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
                NovaTab.AUTOMATION_LOGS -> AutomationLogsScreen(viewModel = viewModel)
                NovaTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
