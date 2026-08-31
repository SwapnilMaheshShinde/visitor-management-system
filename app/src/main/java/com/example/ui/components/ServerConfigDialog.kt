package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ServerConfigDialog(
    currentUrl: String,
    isConnected: Boolean,
    latencyMs: Long,
    lastDevOtp: String,
    isDevOtpMode: Boolean,
    onSaveUrl: (String) -> Unit,
    onTestPing: suspend () -> Pair<Boolean, Long>,
    onToggleDevOtp: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(currentUrl) }
    var pinging by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SlateLightCard,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NavySurface, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = AccentCyanGlow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Central Backend & Network",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavyDark,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "Multi-Device Communication Engine",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SlateLightTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateLightTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Server Status Banner
                val effectiveConnected = testResult?.first ?: isConnected
                val effectiveLatency = testResult?.second ?: latencyMs

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (effectiveConnected) StatusApprovedGreenContainer else StatusPendingAmberContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (effectiveConnected) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (effectiveConnected) StatusApprovedGreenText else StatusPendingAmberText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (effectiveConnected) "PostgreSQL / Express API Online" else "Local Standalone Hybrid Engine",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (effectiveConnected) StatusApprovedGreenText else StatusPendingAmberText
                                )
                            )
                            Text(
                                text = if (effectiveConnected) "Latency: ${effectiveLatency}ms • Multi-device live sync active" else "Running in zero-friction in-memory DB mode.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (effectiveConnected) StatusApprovedGreenText else StatusPendingAmberText,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API URL Input Field
                Text(
                    text = "API Base URL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DeepNavyDark
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    singleLine = true,
                    colors = vmsOutlinedTextFieldColors(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = DeepNavyDark),
                    placeholder = { Text("http://192.168.1.X:5000/api/") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Preset Chips
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall.copy(color = SlateLightTextSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { inputUrl = "http://10.0.2.2:5000/api/" },
                        label = { Text("Emulator (10.0.2.2)", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { inputUrl = "http://localhost:5000/api/" },
                        label = { Text("Localhost", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Connection Button
                Button(
                    onClick = {
                        scope.launch {
                            pinging = true
                            onSaveUrl(inputUrl)
                            val res = onTestPing()
                            testResult = res
                            pinging = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (pinging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing Endpoint Ping...", color = Color.White)
                    } else {
                        Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Test Connection", color = Color.White)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = SlateLightBorder)

                // Dev Mode OTP Inspector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Key, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Development OTP Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark)
                            )
                            Text(
                                text = "Shows real OTP on device without SMS costs",
                                style = MaterialTheme.typography.labelSmall.copy(color = SlateLightTextSecondary, fontSize = 11.sp)
                            )
                        }
                    }
                    Switch(
                        checked = isDevOtpMode,
                        onCheckedChange = { onToggleDevOtp(it) }
                    )
                }

                if (isDevOtpMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Latest Generated Pass OTP:", color = SlateLightTextSecondary, fontSize = 11.sp)
                                Text(
                                    text = lastDevOtp,
                                    color = AccentCyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = 3.sp
                                )
                            }
                            Text(
                                text = "Ready for Guard Verification",
                                color = StatusApprovedGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
