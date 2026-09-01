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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RequestStatus
import com.example.data.model.Visit
import com.example.data.model.VisitRequest
import com.example.ui.components.*
import com.example.ui.theme.*

// =========================================================================
// 1. QR SCANNER SCREEN
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardQrScannerScreen(
    onScanToken: (String) -> Unit,
    onNavigateEnterOtp: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("QR Pass Checkpoint", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Pre-Registered Visitor Entry", fontSize = 11.sp, color = AccentCyanGlow)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyDark)
            )
        },
        containerColor = DeepNavyDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrScannerViewfinder(
                onScanToken = onScanToken,
                onManualInput = onNavigateEnterOtp
            )
        }
    }
}

// =========================================================================
// 2. OTP KEYPAD SCREEN
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardOtpVerifyScreen(
    lastDevOtp: String,
    onVerifyOtp: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var otpInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Verify Visitor OTP", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Gate Entry Validation", fontSize = 11.sp, color = AccentCyanGlow)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyDark)
            )
        },
        containerColor = SlateLightBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OtpKeypadBox(
                otpValue = otpInput,
                onOtpChange = { otpInput = it },
                onVerify = onVerifyOtp,
                lastDevOtp = lastDevOtp
            )
        }
    }
}

// =========================================================================
// 3. CURRENTLY INSIDE & CHECK-OUT SCREEN
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardInsideVisitorsScreen(
    insideVisits: List<Visit>,
    onMarkExit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inside Premises (${insideVisits.size})", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Active Visitor Tracking & Gate Exit", fontSize = 11.sp, color = AccentCyanGlow)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyDark)
            )
        },
        containerColor = SlateLightBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (insideVisits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No Visitors Currently Inside",
                    message = "All checked-in visitors have marked their exit or none have arrived yet."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(insideVisits) { visit ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SlateLightCard,
                        tonalElevation = 3.dp,
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
                                        text = visit.visitorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = DeepNavyDark
                                    )
                                    Text(
                                        text = "${visit.visitorCompany} • ${visit.visitorMobile}",
                                        fontSize = 12.sp,
                                        color = SlateLightTextSecondary
                                    )
                                }
                                StatusBadge("INSIDE")
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SlateLightBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Host", fontSize = 11.sp, color = SlateLightTextSecondary)
                                    Text(visit.hostName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DeepNavyDark)
                                }
                                Column {
                                    Text("Entry Time", fontSize = 11.sp, color = SlateLightTextSecondary)
                                    Text(visit.entryTime, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DeepNavyDark)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Host Sign-off", fontSize = 11.sp, color = SlateLightTextSecondary)
                                    Text(
                                        text = if (visit.employeeVerified) "Verified ✓" else "Pending",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (visit.employeeVerified) StatusApprovedGreenText else StatusPendingAmberText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onMarkExit(visit.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusDeclinedRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mark_exit_button_${visit.id}")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MARK EXIT (CHECK-OUT)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. GUARD PENDING REQUESTS QUEUE SCREEN
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardPendingRequestsScreen(
    requests: List<VisitRequest>,
    onGrantEntry: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Walk-In Request Queue", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Live Host Decisions & Entry Grants", fontSize = 11.sp, color = AccentCyanGlow)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyDark)
            )
        },
        containerColor = SlateLightBackground
    ) { padding ->
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No Walk-in Requests",
                    message = "Register a new visitor to start the approval flow."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(requests) { req ->
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
                                    Text(req.visitorName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepNavyDark)
                                    Text("${req.visitorCompany} • ${req.purpose}", fontSize = 12.sp, color = SlateLightTextSecondary)
                                }
                                StatusBadge(req.status.name)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SlateLightBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

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
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("GRANT ENTRY (STATUS: APPROVED)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
