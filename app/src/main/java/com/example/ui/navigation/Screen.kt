package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")

    // Guard Routes
    object GuardDashboard : Screen("guard_dashboard")
    object GuardNewWalkIn : Screen("guard_new_walkin")
    object GuardScanQr : Screen("guard_scan_qr")
    object GuardEnterOtp : Screen("guard_enter_otp")
    object GuardInsideVisitors : Screen("guard_inside_visitors")
    object GuardPendingRequests : Screen("guard_pending_requests")

    // Employee Routes
    object EmployeeDashboard : Screen("employee_dashboard")
    object EmployeePreRegister : Screen("employee_preregister")
    object EmployeeAppointments : Screen("employee_appointments")
    object EmployeeHistory : Screen("employee_history")

    // Admin Routes
    object AdminDashboard : Screen("admin_dashboard")

    // Shared
    object NotificationCenter : Screen("notifications")
}
