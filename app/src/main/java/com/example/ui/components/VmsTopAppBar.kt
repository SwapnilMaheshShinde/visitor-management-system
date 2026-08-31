package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Role
import com.example.data.model.User
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmsTopAppBar(
    title: String,
    currentUser: User?,
    isServerConnected: Boolean,
    latencyMs: Long,
    notificationCount: Int = 0,
    onOpenServerConfig: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUserMenu by remember { mutableStateOf(false) }

    Surface(
        color = DeepNavyDark,
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: App Logo & Screen Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "VMS Logo",
                            tint = AccentCyanGlow,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp
                            ),
                            maxLines = 1
                        )
                        if (currentUser != null) {
                            Text(
                                text = "${currentUser.name} • ${currentUser.role.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyanGlow,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Right: Server Connection Status & Quick Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Server Connection Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NavyCard,
                        modifier = Modifier.clickable { onOpenServerConfig() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isServerConnected) StatusApprovedGreen else StatusPendingAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServerConnected) "${latencyMs}ms" else "Local DB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Sensors,
                                contentDescription = "Server Settings",
                                tint = AccentCyanGlow,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Notification Bell with Badge
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.size(36.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (notificationCount > 0) {
                                    Badge(containerColor = StatusDeclinedRed) {
                                        Text(
                                            text = if (notificationCount > 9) "9+" else "$notificationCount",
                                            color = Color.White,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // User Profile / Menu
                    Box {
                        IconButton(
                            onClick = { showUserMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (currentUser?.role) {
                                            Role.GUARD -> AccentBlue
                                            Role.EMPLOYEE -> StatusApprovedGreen
                                            Role.ADMIN -> StatusPendingAmber
                                            null -> NavyLight
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false },
                            modifier = Modifier.background(NavyCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Server & Backend Settings", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = AccentCyanGlow)
                                },
                                onClick = {
                                    showUserMenu = false
                                    onOpenServerConfig()
                                }
                            )
                            Divider(color = SlateLightBorder.copy(alpha = 0.2f))
                            DropdownMenuItem(
                                text = { Text("Log Out", color = StatusDeclinedRed) },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = StatusDeclinedRed)
                                },
                                onClick = {
                                    showUserMenu = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
