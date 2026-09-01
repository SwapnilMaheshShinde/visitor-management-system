package com.example.ui.screens.employee

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun EmployeeDashboardScreen(
    currentUser: User?,
    requests: List<VisitRequest>,
    appointments: List<Appointment>,
    insideVisits: List<Visit>,
    isServerConnected: Boolean,
    latencyMs: Long,
    notificationCount: Int,
    onNavigatePreRegister: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateHistory: () -> Unit,
    onDecideRequest: (requestId: Int, accept: Boolean, reason: String?, room: String?) -> Unit,
    onVerifyMeeting: (visitId: Int, signatureData: String, notes: String?) -> Unit,
    onOpenServerConfig: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    // Filter items related to this employee host
    val myPendingRequests = requests.filter { it.status == RequestStatus.PENDING }
    val myInsideVisits = insideVisits
    val myUpcomingAppointments = appointments.filter { it.status == AppointmentStatus.SCHEDULED }

    // Dialog state for Digital Signature
    var activeSignVisit by remember { mutableStateOf<Visit?>(null) }
    // Dialog state for viewing Appointment QR Pass
    var activePassAppt by remember { mutableStateOf<Appointment?>(null) }

    Scaffold(
        topBar = {
            VmsTopAppBar(
                title = "Employee Host Portal",
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Host Information Banner
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
                                text = "${currentUser?.name ?: "Host Employee"}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentUser?.designation ?: "Senior Staff"} • ${currentUser?.department ?: "Technology"}",
                                color = AccentCyanGlow,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusApprovedGreenContainer
                        ) {
                            Text(
                                text = "HOST READY",
                                color = StatusApprovedGreenText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Pending Approvals",
                        value = myPendingRequests.size.toString(),
                        icon = Icons.Outlined.NotificationsActive,
                        color = if (myPendingRequests.isNotEmpty()) StatusDeclinedRed else StatusApprovedGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Scheduled Passes",
                        value = myUpcomingAppointments.size.toString(),
                        icon = Icons.Outlined.EventAvailable,
                        color = AccentCyan,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateAppointments() }
                    )
                }
            }

            // Hero Action: Pre-Register Visitor Appointment
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StatusApprovedGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigatePreRegister() }
                        .testTag("employee_preregister_button")
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
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pre-Register Visitor / Appointment",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Generate secure OTP & digital QR pass for fast entry",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // SECTION: Live Walk-In Requests Awaiting Decision
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VISITOR ARRIVAL REQUESTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SlateLightTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    if (myPendingRequests.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusDeclinedRedContainer
                        ) {
                            Text(
                                text = "${myPendingRequests.size} ACTION NEEDED",
                                color = StatusDeclinedRedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (myPendingRequests.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Pending Approval Requests",
                        message = "When a security guard registers a visitor at the gate, your phone will receive an instant alert here.",
                        icon = Icons.Outlined.CheckCircle
                    )
                }
            } else {
                items(myPendingRequests) { req ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SlateLightCard,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, StatusPendingAmber, RoundedCornerShape(16.dp))
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(NavySurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(req.visitorName.take(1).uppercase(), color = AccentCyanGlow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(req.visitorName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepNavyDark)
                                        Text("${req.visitorCompany} • ${req.visitorMobile}", fontSize = 12.sp, color = SlateLightTextSecondary)
                                    }
                                }
                                StatusBadge("PENDING")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = SlateLightBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Purpose of Meeting:", fontSize = 11.sp, color = SlateLightTextSecondary)
                            Text(req.purpose, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DeepNavyDark)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Arrival Point: ${req.gateName} at ${req.createdAt}", fontSize = 11.sp, color = SlateLightTextSecondary)

                            Spacer(modifier = Modifier.height(16.dp))

                            // ACCEPT / DECLINE Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onDecideRequest(req.id, false, "Host unavailable / Busy in scheduled call", null) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDeclinedRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("decline_request_button_${req.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Decline", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onDecideRequest(req.id, true, null, "Conference Room 4A") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("accept_request_button_${req.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ACCEPT VISIT", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: Visitors Currently Meeting With You (Requires Digital Sign-off)
            item {
                Text(
                    text = "ACTIVE MEETINGS & VERIFICATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SlateLightTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            if (myInsideVisits.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Active Visitors On-Site",
                        message = "Visitors granted entry by security will appear here for meeting verification and host digital sign-off."
                    )
                }
            } else {
                items(myInsideVisits) { visit ->
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
                                    Text(visit.visitorName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepNavyDark)
                                    Text("${visit.visitorCompany} • Entered at ${visit.entryTime}", fontSize = 12.sp, color = SlateLightTextSecondary)
                                }
                                StatusBadge("INSIDE")
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Purpose: ${visit.purpose}", fontSize = 13.sp, color = DeepNavyDark)

                            Spacer(modifier = Modifier.height(12.dp))

                            if (visit.employeeVerified) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StatusApprovedGreenContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = StatusApprovedGreenText, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Meeting Verified & Digitally Signed by Host",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = StatusApprovedGreenText
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { activeSignVisit = visit },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyDark),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verify_meeting_button_${visit.id}")
                                ) {
                                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentCyanGlow)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify Meeting & Sign Digitally", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: Pre-Registered Appointments List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHEDULED PASSES (${myUpcomingAppointments.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SlateLightTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    TextButton(onClick = onNavigateAppointments) {
                        Text("Manage All", fontSize = 12.sp)
                    }
                }
            }

            items(myUpcomingAppointments) { appt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SlateLightCard,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateLightBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(appt.visitorName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepNavyDark)
                            Text("${appt.visitorCompany} • OTP: ${appt.otpCode}", fontSize = 12.sp, color = AccentCyan, fontWeight = FontWeight.SemiBold)
                            Text(appt.expectedDateTime, fontSize = 11.sp, color = SlateLightTextSecondary)
                        }

                        Button(
                            onClick = { activePassAppt = appt },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pass", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal: Digital Signature Pad
    if (activeSignVisit != null) {
        Dialog(onDismissRequest = { activeSignVisit = null }) {
            DigitalSignaturePad(
                visitorName = activeSignVisit!!.visitorName,
                onSignatureConfirmed = { sigData, notes ->
                    onVerifyMeeting(activeSignVisit!!.id, sigData, notes)
                    activeSignVisit = null
                },
                onCancel = { activeSignVisit = null }
            )
        }
    }

    // Modal: QR Pass Visualizer
    if (activePassAppt != null) {
        Dialog(onDismissRequest = { activePassAppt = null }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrPassVisualizer(
                    visitorName = activePassAppt!!.visitorName,
                    otpCode = activePassAppt!!.otpCode,
                    qrToken = activePassAppt!!.qrToken,
                    hostName = activePassAppt!!.hostName,
                    expectedTime = activePassAppt!!.expectedDateTime
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { activePassAppt = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close Pass", color = Color.White)
                }
            }
        }
    }
}
