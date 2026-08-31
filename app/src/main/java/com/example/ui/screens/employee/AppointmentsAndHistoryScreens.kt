package com.example.ui.screens.employee

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Appointment
import com.example.data.model.Visit
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.QrPassVisualizer
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeAppointmentsScreen(
    appointments: List<Appointment>,
    onNavigateBack: () -> Unit
) {
    var selectedApptForPass by remember { mutableStateOf<Appointment?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = appointments.filter {
        searchQuery.isBlank() ||
                it.visitorName.contains(searchQuery, ignoreCase = true) ||
                it.visitorCompany.contains(searchQuery, ignoreCase = true) ||
                it.otpCode.contains(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pre-Registered Passes (${appointments.size})", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Active & Past Appointment Tokens", fontSize = 11.sp, color = AccentCyanGlow)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, company, or OTP...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateLightTextSecondary) },
                singleLine = true,
                colors = vmsOutlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    title = "No Passes Found",
                    message = "No matching appointments. Create a new pre-registration to generate a pass."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filtered) { appt ->
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
                                        Text(appt.visitorName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepNavyDark)
                                        Text("${appt.visitorCompany} • ${appt.visitorMobile}", fontSize = 12.sp, color = SlateLightTextSecondary)
                                    }
                                    StatusBadge(appt.status.name)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = SlateLightBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Security Pass OTP:", fontSize = 11.sp, color = SlateLightTextSecondary)
                                        Text(appt.otpCode, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccentCyan, letterSpacing = 2.sp)
                                    }

                                    Button(
                                        onClick = { selectedApptForPass = appt },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavySurface),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View Pass", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedApptForPass != null) {
        Dialog(onDismissRequest = { selectedApptForPass = null }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrPassVisualizer(
                    visitorName = selectedApptForPass!!.visitorName,
                    otpCode = selectedApptForPass!!.otpCode,
                    qrToken = selectedApptForPass!!.qrToken,
                    hostName = selectedApptForPass!!.hostName,
                    expectedTime = selectedApptForPass!!.expectedDateTime
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { selectedApptForPass = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close Pass", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeHistoryScreen(
    visits: List<Visit>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Visit History & Records", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Completed Meetings & Audit Logs", fontSize = 11.sp, color = AccentCyanGlow)
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
        if (visits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No Historical Visits Yet",
                    message = "Visits that are completed and checked out by security guards will be archived here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(visits) { visit ->
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
                                    Text(visit.visitorName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepNavyDark)
                                    Text("${visit.visitorCompany} • ${visit.purpose}", fontSize = 12.sp, color = SlateLightTextSecondary)
                                }
                                StatusBadge("COMPLETED")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = SlateLightBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Entry / Exit", fontSize = 11.sp, color = SlateLightTextSecondary)
                                    Text("${visit.entryTime} → ${visit.exitTime ?: "N/A"}", fontSize = 12.sp, color = DeepNavyDark, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Duration", fontSize = 11.sp, color = SlateLightTextSecondary)
                                    Text("${visit.totalDurationMinutes ?: 45} mins", fontSize = 12.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
