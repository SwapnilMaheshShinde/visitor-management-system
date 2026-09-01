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
import com.example.data.model.EmployeeHostItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWalkInScreen(
    isLoading: Boolean,
    availableEmployees: List<EmployeeHostItem> = emptyList(),
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
    var hostSearchQuery by remember { mutableStateOf("") }

    // Use live employees from DB or sensible fallback if offline
    val employeeOptions = remember(availableEmployees) {
        if (availableEmployees.isNotEmpty()) {
            availableEmployees.map { emp ->
                Triple(emp.id, emp.name, "${emp.designation} • ${emp.department} (${emp.employeeCode})")
            }
        } else {
            listOf(
                Triple(3, "Amit Verma", "Principal Tech Lead • Engineering"),
                Triple(4, "Priya Nair", "Head of Human Resources • HR Department")
            )
        }
    }

    val filteredEmployees = remember(employeeOptions, hostSearchQuery) {
        if (hostSearchQuery.isBlank()) employeeOptions
        else employeeOptions.filter {
            it.second.contains(hostSearchQuery, ignoreCase = true) ||
            it.third.contains(hostSearchQuery, ignoreCase = true)
        }
    }

    var selectedEmployeeId by remember(employeeOptions) {
        mutableIntStateOf(employeeOptions.firstOrNull()?.first ?: 3)
    }

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
                        Text("Incoming Call Alert Delivery", fontSize = 11.sp, color = AccentCyanGlow)
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
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Call Dispatch Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyanGlow.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentCyanGlow.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Instant Phone Call Dispatch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Submitting will ring the host employee's phone with a full-screen incoming visitor call.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

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
                        Text("Host Employee (Call Recipient)", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (employeeOptions.size > 2) {
                        OutlinedTextField(
                            value = hostSearchQuery,
                            onValueChange = { hostSearchQuery = it },
                            placeholder = { Text("Search host by name or department...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DeepNavyDark) },
                            singleLine = true,
                            colors = vmsOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Host selection cards
                    val displayList = if (filteredEmployees.isNotEmpty()) filteredEmployees else employeeOptions
                    displayList.forEach { emp ->
                        val selected = selectedEmployeeId == emp.first
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) NavySurface else SlateLightBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedEmployeeId = emp.first }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { selectedEmployeeId = emp.first },
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

                    Text("Purpose of Visit *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        purposePresets.forEach { p ->
                            val selected = purpose == p
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) AccentCyanGlow.copy(alpha = 0.15f) else SlateLightBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) AccentCyanGlow else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { purpose = p }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { purpose = p },
                                        colors = RadioButtonDefaults.colors(selectedColor = AccentCyanGlow),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = p,
                                        fontSize = 13.sp,
                                        color = if (selected) DeepNavyDark else SlateLightTextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Government ID Proof Type", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = idProofType,
                        onValueChange = { idProofType = it },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ID Proof Number / Last 4 digits", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = idProofNumber,
                        onValueChange = { idProofNumber = it },
                        placeholder = { Text("e.g. XXXX-1234") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

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

            // Action Button: Send Request & Ring Host Phone
            Button(
                onClick = {
                    if (visitorName.isBlank() || visitorMobile.isBlank()) return@Button
                    val selectedHost = employeeOptions.firstOrNull { it.first == selectedEmployeeId } ?: employeeOptions.first()
                    onSubmit(
                        visitorName,
                        visitorMobile,
                        visitorCompany,
                        purpose,
                        idProofType,
                        idProofNumber.ifBlank { "ID-VERIFIED" },
                        vehicleNumber.ifBlank { null },
                        selectedHost.first,
                        selectedHost.second,
                        1,
                        "Main Security Gate"
                    )
                },
                enabled = visitorName.isNotBlank() && visitorMobile.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_walkin_request_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INITIATE INCOMING CALL TO HOST", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
