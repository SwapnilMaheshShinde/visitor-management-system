package com.example.ui.screens.admin

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminUserItem
import com.example.data.model.AuditLogEntry
import com.example.data.model.PendingUser
import com.example.data.model.SystemStats
import com.example.data.model.User
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.VmsTopAppBar
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    currentUser: User?,
    stats: SystemStats,
    auditLogs: List<AuditLogEntry>,
    pendingUsers: List<PendingUser>,
    adminUsers: List<AdminUserItem>,
    isServerConnected: Boolean,
    latencyMs: Long,
    notificationCount: Int,
    onApproveUser: (Int) -> Unit,
    onRejectUser: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenServerConfig: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Pending Approvals, 2: Users & Gates, 3: Audit Logs

    Scaffold(
        topBar = {
            VmsTopAppBar(
                title = "Security Admin Portal",
                currentUser = currentUser,
                isServerConnected = isServerConnected,
                latencyMs = latencyMs,
                notificationCount = notificationCount,
                onOpenServerConfig = onOpenServerConfig,
                onOpenNotifications = onOpenNotifications,
                onLogout = onLogout
            )
        },
        containerColor = SlateLightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Admin Navigation Tabs
            Surface(
                color = DeepNavyDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DeepNavyDark,
                    contentColor = AccentCyanGlow,
                    edgePadding = 12.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Executive KPI", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            onRefresh()
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pending Approvals", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (pendingUsers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = StatusPendingAmber
                                    ) {
                                        Text(
                                            text = pendingUsers.size.toString(),
                                            color = DeepNavyDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            onRefresh()
                        },
                        text = { Text("Users & Gates", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            onRefresh()
                        },
                        text = { Text("Audit Trail (${auditLogs.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Overview & KPIs
                        item {
                            Text(
                                text = "REAL-TIME CAMPUS OCCUPANCY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SlateLightTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMetricCard(
                                    title = "Currently Inside",
                                    value = stats.currentlyInside.toString(),
                                    icon = Icons.Outlined.MeetingRoom,
                                    color = AccentCyan,
                                    subtitle = "Active on campus",
                                    modifier = Modifier.weight(1f)
                                )
                                StatMetricCard(
                                    title = "Pending Visitors",
                                    value = stats.pendingApprovals.toString(),
                                    icon = Icons.Outlined.HourglassTop,
                                    color = StatusPendingAmber,
                                    subtitle = "At security gates",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMetricCard(
                                    title = "Total Visitors Today",
                                    value = stats.visitorsToday.toString(),
                                    icon = Icons.Outlined.People,
                                    color = StatusApprovedGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                StatMetricCard(
                                    title = "Pending Registrations",
                                    value = pendingUsers.size.toString(),
                                    icon = Icons.Outlined.PersonAdd,
                                    color = AccentBlue,
                                    subtitle = "Staff & Guards",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMetricCard(
                                    title = "Scheduled Passes",
                                    value = stats.scheduledAppointments.toString(),
                                    icon = Icons.Outlined.ConfirmationNumber,
                                    color = AccentBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                StatMetricCard(
                                    title = "Active Security Gates",
                                    value = "3 Gates",
                                    icon = Icons.Outlined.Security,
                                    color = StatusApprovedGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Infrastructure Diagnostics
                        item {
                            Text(
                                text = "POSTGRESQL & PUSH INFRASTRUCTURE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SlateLightTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DeepNavyDark,
                                tonalElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isServerConnected) StatusApprovedGreen else StatusPendingAmber)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isServerConnected) "Central PostgreSQL Cluster Connected" else "Zero-Config In-Memory Engine Active",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Text(
                                            text = if (isServerConnected) "${latencyMs}ms ping" else "Local Standalone",
                                            color = AccentCyanGlow,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = Color.White.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "FCM Notification Engine: Ready • Push Channels: /topics/guards & /topics/hosts • Audit Level: STRICT",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Pending User Registrations Approval
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "USER REGISTRATION APPROVAL REQUESTS (${pendingUsers.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SlateLightTextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                TextButton(onClick = { onRefresh() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh", fontSize = 12.sp)
                                }
                            }
                        }

                        if (pendingUsers.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No Pending Approvals",
                                    message = "All user registrations are currently approved and active."
                                )
                            }
                        } else {
                            items(pendingUsers) { pending ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SlateLightCard,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SlateLightBorder, RoundedCornerShape(14.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(NavySurface),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = pending.name.take(1).uppercase(),
                                                        color = AccentCyanGlow,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = pending.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = DeepNavyDark
                                                    )
                                                    Text(
                                                        text = "${pending.email} • ${pending.mobile}",
                                                        fontSize = 11.sp,
                                                        color = SlateLightTextSecondary
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when (pending.role.name) {
                                                    "ADMIN" -> DeepNavyDark
                                                    "GUARD" -> AccentBlue
                                                    else -> StatusApprovedGreen
                                                }
                                            ) {
                                                Text(
                                                    text = pending.role.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        Divider(color = SlateLightBorder)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onRejectUser(pending.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDeclinedRed),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject", fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = { onApproveUser(pending.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve & Activate", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Master Users & Gates
                        item {
                            Text(
                                text = "SYSTEM USERS DIRECTORY (${adminUsers.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SlateLightTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        if (adminUsers.isEmpty()) {
                            val defaultUsers = listOf(
                                Triple("Swapnil Shinde", "Primary System Administrator (swapnilshinde538@gmail.com)", "ACTIVE"),
                                Triple("Amit Verma", "Principal Tech Lead (amit.verma@vms.com)", "ACTIVE"),
                                Triple("Priya Nair", "Head of HR (priya.nair@vms.com)", "ACTIVE"),
                                Triple("Officer Vikram Singh", "Security Guard - Gate 1 (guard@vms.com)", "ACTIVE")
                            )
                            items(defaultUsers) { u ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SlateLightCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SlateLightBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(NavySurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(u.first.take(1).uppercase(), color = AccentCyanGlow, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(u.first, fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 14.sp)
                                            Text(u.second, color = SlateLightTextSecondary, fontSize = 11.sp)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = StatusApprovedGreenContainer
                                        ) {
                                            Text(u.third, color = StatusApprovedGreenText, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            items(adminUsers) { u ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SlateLightCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SlateLightBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(NavySurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(u.name.take(1).uppercase(), color = AccentCyanGlow, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(u.name, fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 14.sp)
                                            Text("${u.email} • ${u.role.name}", color = SlateLightTextSecondary, fontSize = 11.sp)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (u.active) StatusApprovedGreenContainer else StatusPendingAmberContainer
                                        ) {
                                            Text(
                                                text = if (u.active) "ACTIVE" else "PENDING",
                                                color = if (u.active) StatusApprovedGreenText else StatusPendingAmberText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "CAMPUS ACCESS GATES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SlateLightTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        val gates = listOf(
                            Triple("Gate 1", "Main Security Gate (North)", "Pedestrian & Vehicle Access"),
                            Triple("Gate 2", "South Visitor & VIP Gate", "Executive Visitors & Board Members"),
                            Triple("Gate 3", "Logistics & Cargo Gate", "Vendor Delivery & Freight")
                        )

                        items(gates) { gate ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SlateLightCard,
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateLightBorder, RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NavySurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(gate.second, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepNavyDark)
                                        Text(gate.third, fontSize = 11.sp, color = SlateLightTextSecondary)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusApprovedGreenContainer
                                    ) {
                                        Text("ONLINE", color = StatusApprovedGreenText, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Audit Logs Stream
                        item {
                            Text(
                                text = "SYSTEM AUDIT TRAIL (CRYPTOGRAPHIC EVENT LOG)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SlateLightTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        if (auditLogs.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No Audit Logs Yet",
                                    message = "System security events will stream here in real-time."
                                )
                            }
                        } else {
                            items(auditLogs) { log ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SlateLightCard,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SlateLightBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = NavySurface
                                            ) {
                                                Text(
                                                    text = log.action,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = AccentCyanGlow,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = log.createdAt,
                                                fontSize = 11.sp,
                                                color = SlateLightTextSecondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = log.details,
                                            fontSize = 12.sp,
                                            color = DeepNavyDark
                                        )
                                        Text(
                                            text = "Entity: ${log.entityType} #${log.entityId}",
                                            fontSize = 10.sp,
                                            color = SlateLightTextSecondary,
                                            fontFamily = FontFamily.Monospace
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
}
