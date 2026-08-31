package com.example.ui.screens.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWalkInScreen(
    isLoading: Boolean,
    onSubmit: (
        visitorName: String,
        visitorMobile: String,
        visitorCompany: String,
        purpose: String,
        idProofType: String,
        idProofNumber: String,
        vehicleNumber: String?,
        hostEmployeeId: Int,
        hostName: String,
        gateId: Int,
        gateName: String
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    var visitorName by remember { mutableStateOf("") }
    var visitorMobile by remember { mutableStateOf("") }
    var visitorCompany by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Official Business Meeting") }
    var idProofType by remember { mutableStateOf("National ID") }
    var idProofNumber by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }

    // Hosts list for selection
    val employees = remember {
        listOf(
            Triple(3, "Amit Verma", "Principal Tech Lead • Tower A"),
            Triple(4, "Priya Nair", "Head of Human Resources • Tower B")
        )
    }
    var selectedEmployeeIndex by remember { mutableIntStateOf(0) }
    var expandedHostDropdown by remember { mutableStateOf(false) }

    // Purpose presets
    val purposePresets = listOf(
        "Official Business Meeting",
        "Technical Interview",
        "Vendor Delivery & Logistics",
        "Facility Maintenance",
        "Executive VIP Visit"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Register Walk-In Visitor", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Instant Security Approval Flow", fontSize = 11.sp, color = AccentCyanGlow)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Visitor Contact & Identity
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateLightCard,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NavySurface, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Visitor Information", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Full Name *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorName,
                        onValueChange = { visitorName = it },
                        placeholder = { Text("e.g. Rajesh Kumar") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("walkin_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Mobile Number *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorMobile,
                        onValueChange = { visitorMobile = it },
                        placeholder = { Text("e.g. 9876543210") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("walkin_mobile_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Organization / Company", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorCompany,
                        onValueChange = { visitorCompany = it },
                        placeholder = { Text("e.g. Cisco Systems Ltd") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Section 2: Host Employee Selection
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateLightCard,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(StatusApprovedGreenContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = StatusApprovedGreenText, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Host Employee (Recipient)", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Host selection cards
                    employees.forEachIndexed { index, emp ->
                        val selected = selectedEmployeeIndex == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) NavySurface else SlateLightBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedEmployeeIndex = index }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { selectedEmployeeIndex = index },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentCyanGlow)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = emp.second,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else DeepNavyDark,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = emp.third,
                                        color = if (selected) Color(0xFFCBD5E1) else SlateLightTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Purpose & Identification
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateLightCard,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NavySurface, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Purpose & Verification", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Purpose of Visit", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { purpose = "Technical Interview" },
                            label = { Text("Interview", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { purpose = "Official Business Meeting" },
                            label = { Text("Meeting", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { purpose = "Vendor Delivery" },
                            label = { Text("Delivery", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ID Proof Type", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = idProofType,
                                onValueChange = { idProofType = it },
                                singleLine = true,
                                colors = vmsOutlinedTextFieldColors(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ID Number", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = idProofNumber,
                                onValueChange = { idProofNumber = it },
                                placeholder = { Text("e.g. DL-9812") },
                                singleLine = true,
                                colors = vmsOutlinedTextFieldColors(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Vehicle Number (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = vehicleNumber,
                        onValueChange = { vehicleNumber = it },
                        placeholder = { Text("e.g. KA 01 AB 9988") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Action Button: Send Request
            Button(
                onClick = {
                    if (visitorName.isBlank() || visitorMobile.isBlank()) return@Button
                    val host = employees[selectedEmployeeIndex]
                    onSubmit(
                        visitorName,
                        visitorMobile,
                        visitorCompany,
                        purpose,
                        idProofType,
                        idProofNumber.ifBlank { "ID-VERIFIED" },
                        vehicleNumber.ifBlank { null },
                        host.first,
                        host.second,
                        1,
                        "Main Security Gate (North)"
                    )
                },
                enabled = visitorName.isNotBlank() && visitorMobile.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_walkin_request_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEND REQUEST TO EMPLOYEE PHONE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
