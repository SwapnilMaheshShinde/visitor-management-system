package com.example.ui.screens.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Appointment
import com.example.ui.components.QrPassVisualizer
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreRegisterScreen(
    isLoading: Boolean,
    onSubmit: (
        visitorName: String,
        visitorMobile: String,
        visitorCompany: String,
        visitorEmail: String?,
        purpose: String,
        expectedDateTime: String,
        departmentId: Int
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    var visitorName by remember { mutableStateOf("") }
    var visitorMobile by remember { mutableStateOf("") }
    var visitorCompany by remember { mutableStateOf("") }
    var visitorEmail by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Strategic Client Meeting") }
    var expectedDateTime by remember { mutableStateOf("Today, 02:30 PM") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pre-Register Visitor", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Generate Secure QR & 6-Digit OTP Pass", fontSize = 11.sp, color = AccentCyanGlow)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        Text("Visitor & Pass Information", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Visitor Full Name *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorName,
                        onValueChange = { visitorName = it },
                        placeholder = { Text("e.g. Meera Kapoor") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preregister_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Mobile Number (For Pass Delivery) *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorMobile,
                        onValueChange = { visitorMobile = it },
                        placeholder = { Text("e.g. 9876500000") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preregister_mobile_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Organization / Client Company", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorCompany,
                        onValueChange = { visitorCompany = it },
                        placeholder = { Text("e.g. Microsoft Azure Enterprise") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Email Address (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorEmail,
                        onValueChange = { visitorEmail = it },
                        placeholder = { Text("e.g. meera@microsoft.com") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateLightCard,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Purpose & Expected Schedule", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Purpose of Meeting", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Expected Date & Time", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = expectedDateTime,
                        onValueChange = { expectedDateTime = it },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (visitorName.isBlank() || visitorMobile.isBlank()) return@Button
                    onSubmit(
                        visitorName,
                        visitorMobile,
                        visitorCompany,
                        visitorEmail.ifBlank { null },
                        purpose,
                        expectedDateTime,
                        1
                    )
                },
                enabled = visitorName.isNotBlank() && visitorMobile.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_preregister_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE SECURE OTP & QR PASS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
