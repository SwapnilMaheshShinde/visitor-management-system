package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.Role
import com.example.ui.components.ServerConfigDialog
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.employee.EmployeeAppointmentsScreen
import com.example.ui.screens.employee.EmployeeDashboardScreen
import com.example.ui.screens.employee.EmployeeHistoryScreen
import com.example.ui.screens.employee.PreRegisterScreen
import com.example.ui.screens.guard.GuardDashboardScreen
import com.example.ui.screens.guard.GuardInsideVisitorsScreen
import com.example.ui.screens.guard.GuardOtpVerifyScreen
import com.example.ui.screens.guard.GuardPendingRequestsScreen
import com.example.ui.screens.guard.GuardQrScannerScreen
import com.example.ui.screens.guard.NewWalkInScreen
import com.example.viewmodel.VmsViewModel
import kotlinx.coroutines.launch

@Composable
fun VmsNavHost(
    viewModel: VmsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isServerConnected by viewModel.isServerConnected.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val lastDevOtp by viewModel.lastDevOtp.collectAsState()
    val isDevOtpMode by viewModel.isDevOtpMode.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    val requests by viewModel.requests.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val insideVisits by viewModel.insideVisits.collectAsState()
    val visitHistory by viewModel.visitHistory.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val adminUsers by viewModel.adminUsers.collectAsState()
    val availableEmployees by viewModel.availableEmployees.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showServerConfigDialog by remember { mutableStateOf(false) }

    // Show Snackbars when ViewModel emits uiMessage
    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUiMessage()
        }
    }

    // Auto-navigate to respective dashboard upon login
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            when (currentUser?.role) {
                Role.GUARD -> navController.navigate(Screen.GuardDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                Role.EMPLOYEE -> navController.navigate(Screen.EmployeeDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                Role.ADMIN -> navController.navigate(Screen.AdminDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                null -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (currentUser == null) Screen.Login.route else when (currentUser?.role) {
                    Role.GUARD -> Screen.GuardDashboard.route
                    Role.EMPLOYEE -> Screen.EmployeeDashboard.route
                    Role.ADMIN -> Screen.AdminDashboard.route
                    null -> Screen.Login.route
                }
            ) {
                // Login & Registration
                composable(Screen.Login.route) {
                    LoginScreen(
                        isServerConnected = isServerConnected,
                        latencyMs = latencyMs,
                        isLoading = isLoading,
                        onLogin = { ident, pass, role -> viewModel.login(ident, pass, role) },
                        onRegister = { email, mobile, pass, name, role, code, deptId, desig, badge, gateId ->
                            viewModel.register(
                                email = email,
                                mobile = mobile,
                                password = pass,
                                name = name,
                                role = role,
                                employeeCode = code,
                                departmentId = deptId,
                                designation = desig,
                                badgeNumber = badge,
                                gateId = gateId,
                                onSuccess = {
                                    // Remain on login screen with notification
                                }
                            )
                        },
                        onOpenServerConfig = { showServerConfigDialog = true }
                    )
                }

                // Guard Portal
                composable(Screen.GuardDashboard.route) {
                    GuardDashboardScreen(
                        currentUser = currentUser,
                        stats = stats,
                        requests = requests,
                        insideCount = insideVisits.size,
                        isServerConnected = isServerConnected,
                        latencyMs = latencyMs,
                        notificationCount = notifications.size,
                        onNavigateNewWalkIn = { navController.navigate(Screen.GuardNewWalkIn.route) },
                        onNavigateScanQr = { navController.navigate(Screen.GuardScanQr.route) },
                        onNavigateEnterOtp = { navController.navigate(Screen.GuardEnterOtp.route) },
                        onNavigateInsideVisitors = { navController.navigate(Screen.GuardInsideVisitors.route) },
                        onNavigatePendingRequests = { navController.navigate(Screen.GuardPendingRequests.route) },
                        onGrantEntry = { reqId -> viewModel.grantEntry(reqId) },
                        onOpenServerConfig = { showServerConfigDialog = true },
                        onOpenNotifications = { navController.navigate(Screen.NotificationCenter.route) },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.GuardNewWalkIn.route) {
                    NewWalkInScreen(
                        isLoading = isLoading,
                        availableEmployees = availableEmployees,
                        onSubmit = { name, mob, comp, purp, idType, idNum, veh, hostId, hostName, gateId, gateName ->
                            viewModel.submitWalkIn(name, mob, comp, purp, idType, idNum, veh, hostId, hostName, gateId, gateName) {
                                navController.popBackStack()
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.GuardScanQr.route) {
                    GuardQrScannerScreen(
                        onScanToken = { token ->
                            viewModel.verifyQr(token) {
                                navController.popBackStack()
                            }
                        },
                        onNavigateEnterOtp = {
                            navController.navigate(Screen.GuardEnterOtp.route) {
                                popUpTo(Screen.GuardScanQr.route) { inclusive = true }
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.GuardEnterOtp.route) {
                    GuardOtpVerifyScreen(
                        lastDevOtp = lastDevOtp,
                        onVerifyOtp = { otp ->
                            viewModel.verifyOtp(otp) {
                                navController.popBackStack()
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.GuardInsideVisitors.route) {
                    GuardInsideVisitorsScreen(
                        insideVisits = insideVisits,
                        onMarkExit = { visitId -> viewModel.markExit(visitId) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.GuardPendingRequests.route) {
                    GuardPendingRequestsScreen(
                        requests = requests,
                        onGrantEntry = { reqId -> viewModel.grantEntry(reqId) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Employee Portal
                composable(Screen.EmployeeDashboard.route) {
                    EmployeeDashboardScreen(
                        currentUser = currentUser,
                        requests = requests,
                        appointments = appointments,
                        insideVisits = insideVisits,
                        isServerConnected = isServerConnected,
                        latencyMs = latencyMs,
                        notificationCount = notifications.size,
                        onNavigatePreRegister = { navController.navigate(Screen.EmployeePreRegister.route) },
                        onNavigateAppointments = { navController.navigate(Screen.EmployeeAppointments.route) },
                        onNavigateHistory = { navController.navigate(Screen.EmployeeHistory.route) },
                        onDecideRequest = { reqId, accept, reason, room ->
                            viewModel.decideRequest(reqId, accept, reason, room)
                        },
                        onVerifyMeeting = { visitId, sigData, notes ->
                            viewModel.verifyMeeting(visitId, sigData, notes)
                        },
                        onOpenServerConfig = { showServerConfigDialog = true },
                        onOpenNotifications = { navController.navigate(Screen.NotificationCenter.route) },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.EmployeePreRegister.route) {
                    PreRegisterScreen(
                        isLoading = isLoading,
                        onSubmit = { name, mob, comp, email, purp, time, deptId ->
                            viewModel.createAppointment(name, mob, comp, email, purp, time, deptId) {
                                navController.popBackStack()
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.EmployeeAppointments.route) {
                    EmployeeAppointmentsScreen(
                        appointments = appointments,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.EmployeeHistory.route) {
                    EmployeeHistoryScreen(
                        visits = visitHistory,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Admin Portal
                composable(Screen.AdminDashboard.route) {
                    AdminDashboardScreen(
                        currentUser = currentUser,
                        stats = stats,
                        auditLogs = auditLogs,
                        pendingUsers = pendingUsers,
                        adminUsers = adminUsers,
                        isServerConnected = isServerConnected,
                        latencyMs = latencyMs,
                        notificationCount = notifications.size,
                        onApproveUser = { userId -> viewModel.approveUser(userId) },
                        onRejectUser = { userId -> viewModel.rejectUser(userId) },
                        onRefresh = { viewModel.fetchAdminData() },
                        onOpenServerConfig = { showServerConfigDialog = true },
                        onOpenNotifications = { navController.navigate(Screen.NotificationCenter.route) },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // Shared Notifications
                composable(Screen.NotificationCenter.route) {
                    NotificationCenterScreen(
                        notifications = notifications,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // Central Server Configuration Dialog
            if (showServerConfigDialog) {
                ServerConfigDialog(
                    currentUrl = baseUrl,
                    isConnected = isServerConnected,
                    latencyMs = latencyMs,
                    lastDevOtp = lastDevOtp,
                    isDevOtpMode = isDevOtpMode,
                    onSaveUrl = { newUrl -> viewModel.updateBaseUrl(newUrl) },
                    onTestPing = { viewModel.testPing() },
                    onToggleDevOtp = { enabled -> viewModel.toggleDevOtp(enabled) },
                    onDismiss = { showServerConfigDialog = false }
                )
            }
        }
    }
}
