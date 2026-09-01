package com.example.ui.screens.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RequestStatus
import com.example.data.model.SystemStats
import com.example.data.model.User
import com.example.data.model.VisitRequest
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.VmsTopAppBar
import com.example.ui.theme.*

@Composable
fun GuardDashboardScreen(
    currentUser: User?,
    stats: SystemStats,
    requests: List<VisitRequest>,
    insideCount: Int,
    isServerConnected: Boolean,
    latencyMs: Long,
    notificationCount: Int,
    onNavigateNewWalkIn: () -> Unit,
    onNavigateScanQr: () -> Unit,
    onNavigateEnterOtp: () -> Unit,
    onNavigateInsideVisitors: () -> Unit,
    onNavigatePendingRequests: () -> Unit,
    onGrantEntry: (Int) -> Unit,
    onOpenServerConfig: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val pendingOrAccepted = requests.filter { it.status == RequestStatus.PENDING || it.status == RequestStatus.ACCEPTED }

    Scaffold(
        topBar = {
            VmsTopAppBar(
                title = "Security Guard Portal",
                currentUser = currentUser,
                isServerConnected = isServerConnected,
                latencyMs = latencyMs,
                notificationCount = notificationCount,
                onOpenServerConfig = onOpenServerConfig,
                onOpenNotifications = onOpenNotifications,
                onLogout = onLogout
            )
        },
        containerColor = SlateLightBackground,
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Guard Checkpoint Info Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DeepNavyDark,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.assignedGate ?: "Main Security Gate (North)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Officer Badge: ${currentUser?.badgeNumber ?: "SEC-8821"} • Active Shift",
                                color = AccentCyanGlow,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NavyLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusApprovedGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GATE ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Currently Inside",
                        value = insideCount.toString(),
                        icon = Icons.Outlined.MeetingRoom,
                        color = AccentCyan,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateInsideVisitors() }
                    )
                    StatMetricCard(
                        title = "Pending Approval",
                        value = stats.pendingApprovals.toString(),
                        icon = Icons.Outlined.HourglassTop,
                        color = StatusPendingAmber,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigatePendingRequests() }
                    )
                }
            }

            // Guard Action Buttons (Optimized for 1-handed operation)
            item {
                Text(
                    text = "CHECKPOINT ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SlateLightTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            item {
                // Large Primary Hero Button: Register Walk-In Visitor
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AccentBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateNewWalkIn() }
                        .testTag("guard_new_walkin_button")
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Register New Walk-In Visitor",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Enter details & send instant approval request to host",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Fast Pre-registered Pass Checkers: QR Scan & OTP Keypad
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuardActionTile(
                        title = "Scan QR Pass",
                        subtitle = "High-speed camera check",
                        icon = Icons.Outlined.QrCodeScanner,
                        color = NavySurface,
                        textColor = Color.White,
                        onClick = onNavigateScanQr,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("guard_scan_qr_button")
                    )
                    GuardActionTile(
                        title = "Enter 6-Digit OTP",
                        subtitle = "Keypad pass validation",
                        icon = Icons.Outlined.Pin,
                        color = StatusApprovedGreen,
                        textColor = Color.White,
                        onClick = onNavigateEnterOtp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("guard_enter_otp_button")
                    )
                }
            }

            // Secondary Quick Navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuardActionTile(
                        title = "Inside Visitors",
                        subtitle = "$insideCount currently on-site",
                        icon = Icons.Outlined.Badge,
                        color = SlateLightCard,
                        textColor = DeepNavyDark,
                        onClick = onNavigateInsideVisitors,
                        modifier = Modifier.weight(1f)
                    )
                    GuardActionTile(
                        title = "Request Queue",
                        subtitle = "${requests.size} total requests",
                        icon = Icons.Outlined.History,
                        color = SlateLightCard,
                        textColor = DeepNavyDark,
                        onClick = onNavigatePendingRequests,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Live Requests Stream & Fast Entry Granting
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VISITOR APPROVAL STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SlateLightTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    TextButton(onClick = onNavigatePendingRequests) {
                        Text("View All (${requests.size})", fontSize = 12.sp)
                    }
                }
            }

            if (pendingOrAccepted.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Pending Walk-in Requests",
                        message = "All visitor requests have been processed. Register a new walk-in or validate a pre-registered pass."
                    )
                }
            } else {
                items(pendingOrAccepted) { req ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SlateLightCard,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateLightBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = req.visitorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DeepNavyDark
                                    )
                                    Text(
                                        text = "${req.visitorCompany} • ${req.visitorMobile}",
                                        fontSize = 12.sp,
                                        color = SlateLightTextSecondary
                                    )
                                }
                                StatusBadge(req.status.name)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = SlateLightBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Host: ${req.hostName}", fontSize = 12.sp, color = DeepNavyDark, fontWeight = FontWeight.Medium)
                                Text("Time: ${req.createdAt}", fontSize = 12.sp, color = SlateLightTextSecondary)
                            }

                            if (req.status == RequestStatus.ACCEPTED) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onGrantEntry(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grant_entry_button_${req.id}")
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("GRANT ENTRY TO VISITOR", fontWeight = FontWeight.Bold)
                                }
                            } else if (req.status == RequestStatus.PENDING) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = StatusPendingAmber
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Awaiting response from ${req.hostName}...",
                                        fontSize = 11.sp,
                                        color = StatusPendingAmberText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color,
        tonalElevation = 2.dp,
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, if (color == SlateLightCard) SlateLightBorder else Color.Transparent, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (color == SlateLightCard) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (color == SlateLightCard) DeepNavyDark else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (color == SlateLightCard) SlateLightTextSecondary else Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
