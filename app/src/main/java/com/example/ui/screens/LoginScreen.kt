package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Role
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    isServerConnected: Boolean,
    latencyMs: Long,
    isLoading: Boolean,
    onLogin: (identifier: String, password: String?, role: Role?) -> Unit,
    onRegister: (
        email: String,
        mobile: String,
        password: String,
        name: String,
        role: Role,
        employeeCode: String?,
        departmentId: Int?,
        designation: String?,
        badgeNumber: String?,
        gateId: Int?
    ) -> Unit,
    onOpenServerConfig: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }

    // Sign-in states - Default to Bootstrap Admin
    var selectedRole by remember { mutableStateOf(Role.ADMIN) }
    var identifier by remember { mutableStateOf("swapnilshinde538@gmail.com") }
    var password by remember { mutableStateOf("12345678@Ss") }

    // Registration states
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf(Role.EMPLOYEE) }
    var regEmployeeCode by remember { mutableStateOf("") }
    var regDesignation by remember { mutableStateOf("") }
    var regBadgeNumber by remember { mutableStateOf("") }

    // Synchronize default credentials when role changes in login mode
    LaunchedEffect(selectedRole) {
        if (!isRegisterMode) {
            when (selectedRole) {
                Role.ADMIN -> {
                    identifier = "swapnilshinde538@gmail.com"
                    password = "12345678@Ss"
                }
                Role.GUARD -> {
                    identifier = ""
                    password = ""
                }
                Role.EMPLOYEE -> {
                    identifier = ""
                    password = ""
                }
            }
        }
    }

    Scaffold(
        containerColor = DeepNavyDark,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Server Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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
                            text = if (isServerConnected) "Cloud DB Live (${latencyMs}ms)" else "Local Hybrid Mode",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Outlined.Sensors, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(13.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Logo
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NavySurface)
                    .border(2.dp, AccentCyanGlow.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "VMS Enterprise Shield",
                    tint = AccentCyanGlow,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Visitor Management System",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 21.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Multi-Device Enterprise Access Control Platform",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SlateLightTextSecondary,
                    fontSize = 12.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mode Selector: Sign In vs Register
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .padding(3.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isRegisterMode) NavyCard else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRegisterMode = false }
                ) {
                    Text(
                        text = "Sign In",
                        color = if (!isRegisterMode) Color.White else Color(0xFF94A3B8),
                        fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp),
                        fontSize = 13.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isRegisterMode) NavyCard else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRegisterMode = true }
                ) {
                    Text(
                        text = "Register Account",
                        color = if (isRegisterMode) Color.White else Color(0xFF94A3B8),
                        fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!isRegisterMode) {
                // Role Selector Tabs for Login
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavySurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RoleTabItem(
                        title = "Guard",
                        icon = Icons.Outlined.Security,
                        selected = selectedRole == Role.GUARD,
                        onClick = { selectedRole = Role.GUARD },
                        modifier = Modifier.weight(1f)
                    )
                    RoleTabItem(
                        title = "Employee",
                        icon = Icons.Outlined.Badge,
                        selected = selectedRole == Role.EMPLOYEE,
                        onClick = { selectedRole = Role.EMPLOYEE },
                        modifier = Modifier.weight(1f)
                    )
                    RoleTabItem(
                        title = "Admin",
                        icon = Icons.Outlined.AdminPanelSettings,
                        selected = selectedRole == Role.ADMIN,
                        onClick = { selectedRole = Role.ADMIN },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Login Card Form
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SlateLightCard,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = when (selectedRole) {
                                Role.GUARD -> "Security Checkpoint Portal"
                                Role.EMPLOYEE -> "Employee Host Portal"
                                Role.ADMIN -> "System Administrator Portal"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyDark,
                                fontSize = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Email or Mobile Number",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavyDark
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            singleLine = true,
                            colors = vmsOutlinedTextFieldColors(),
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AccentCyan)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_identifier_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavyDark
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = vmsOutlinedTextFieldColors(),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentCyan)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { onLogin(identifier, password, selectedRole) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (selectedRole) {
                                    Role.GUARD -> AccentBlue
                                    Role.EMPLOYEE -> StatusApprovedGreen
                                    Role.ADMIN -> DeepNavyDark
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Access ${selectedRole.name} Portal",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bootstrap Admin Master Control & Quick Access
                Text(
                    text = "MASTER BOOTSTRAP ADMINISTRATOR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Bootstrap Admin Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NavyCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedRole = Role.ADMIN
                            identifier = "swapnilshinde538@gmail.com"
                            password = "12345678@Ss"
                            onLogin("swapnilshinde538@gmail.com", "12345678@Ss", Role.ADMIN)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(AccentCyanGlow)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Swapnil Shinde (Bootstrap Admin)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("swapnilshinde538@gmail.com • Approves new Guards & Staff", color = SlateLightTextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = "Log in as Admin", tint = AccentCyanGlow, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // New Staff / Guard Registration Prompt Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateLightCard.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isRegisterMode = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Need Guard or Employee Access?", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Register your account to request Admin approval.", color = SlateLightTextSecondary, fontSize = 10.sp)
                        }
                        Text(
                            "Register",
                            color = AccentCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // REGISTRATION FORM
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SlateLightCard,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Register New VMS Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyDark,
                                fontSize = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusPendingAmberContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = StatusPendingAmberText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "All new user registrations require Administrator approval before first login.",
                                    fontSize = 11.sp,
                                    color = StatusPendingAmberText,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Role Selector
                        Text(
                            text = "Select Account Role",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(Role.EMPLOYEE, Role.GUARD, Role.ADMIN).forEach { role ->
                                FilterChip(
                                    selected = regRole == role,
                                    onClick = { regRole = role },
                                    label = { Text(role.name, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Full Legal Name", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            placeholder = { Text("e.g. Rahul Sharma") },
                            singleLine = true,
                            colors = vmsOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Work Email Address", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            placeholder = { Text("rahul@vms.com") },
                            singleLine = true,
                            colors = vmsOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Mobile Number", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = regMobile,
                            onValueChange = { regMobile = it },
                            placeholder = { Text("9876543210") },
                            singleLine = true,
                            colors = vmsOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Create Password", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            placeholder = { Text("Min 6 characters") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = vmsOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (regRole == Role.EMPLOYEE) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Employee ID Code", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = regEmployeeCode,
                                onValueChange = { regEmployeeCode = it },
                                placeholder = { Text("e.g. EMP-2041") },
                                singleLine = true,
                                colors = vmsOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else if (regRole == Role.GUARD) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Security Badge Number", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = DeepNavyDark))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = regBadgeNumber,
                                onValueChange = { regBadgeNumber = it },
                                placeholder = { Text("e.g. SEC-9912") },
                                singleLine = true,
                                colors = vmsOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (regName.isNotBlank() && regEmail.isNotBlank() && regMobile.isNotBlank() && regPassword.isNotBlank()) {
                                    onRegister(
                                        regEmail,
                                        regMobile,
                                        regPassword,
                                        regName,
                                        regRole,
                                        regEmployeeCode.ifBlank { null },
                                        1,
                                        regDesignation.ifBlank { null },
                                        regBadgeNumber.ifBlank { null },
                                        1
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavyDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Submit for Admin Approval", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) NavyCard else Color.Transparent,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AccentCyanGlow else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color.White else Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            )
        }
    }
}
