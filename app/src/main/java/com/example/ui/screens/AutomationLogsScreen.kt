package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AutomationLogItem
import com.example.ui.NovaAssistantViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusPillType
import com.example.ui.theme.DarkBgStart
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassCardBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.FintechBody
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutomationLogsScreen(
    viewModel: NovaAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.automationLogs.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Top Header Glass Card
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
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = DarkBgStart, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Automation Audit Log",
                            style = FintechSectionTitle,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        Text(
                            text = "VERIFIED LOCAL DEVICE TELEMETRY",
                            style = FintechMicroLabel,
                            color = MintPrimary
                        )
                    }
                }

                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAutomationLogs() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) DarkGlassCard else LightGlassCard)
                            .border(1.dp, Color(0xFFFF5A5F).copy(alpha = 0.4f), CircleShape)
                            .testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = Color(0xFFFF5A5F),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isDark) DarkGlassCard else LightGlassCard)
                                .border(1.dp, MintPrimary.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🛡️", fontSize = 26.sp)
                        }
                        Text(
                            text = "NO AUDIT RECORDS YET",
                            style = FintechMicroLabel,
                            color = MintPrimary
                        )
                        Text(
                            text = "Device actions (flashlight, alarms, specs, volume) will automatically be cryptographically logged here.",
                            style = FintechBody,
                            color = if (isDark) DarkTextSecondary else LightTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs, key = { it.id }) { logItem ->
                    FintechAuditLogCard(item = logItem)
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun FintechAuditLogCard(item: AutomationLogItem) {
    val isDark = LocalIsDarkTheme.current
    val isSuccess = item.status == "SUCCESS" || item.status == "DISPATCHED"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderTint = if (isSuccess) MintPrimary.copy(alpha = 0.35f) else Color(0xFFFF5A5F).copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSuccess) MintPrimary.copy(alpha = 0.2f) else Color(0xFFFF5A5F).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚡", fontSize = 12.sp)
                }
                Text(
                    text = item.actionType.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
            }

            StatusPill(
                text = item.status,
                type = if (isSuccess) StatusPillType.SUCCESS else StatusPillType.ERROR
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.description,
            style = FintechBody,
            color = if (isDark) DarkTextSecondary else LightTextSecondary
        )

        if (item.result.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.result,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) MintPrimary else Color(0xFFFF5A5F)
            )
        }

        if (item.parameters.isNotBlank() && item.parameters != "{}") {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A103C2D))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.parameters,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) DarkTextMuted else LightTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDateTime(item.timestamp),
                style = FintechMicroLabel,
                color = if (isDark) DarkTextMuted else LightTextMuted
            )
            Text(
                text = "ID #${item.id % 10000}",
                style = FintechMicroLabel,
                color = MintPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault())
    return sdf.format(date)
}
