package com.example.data.model

enum class Role {
    GUARD,
    EMPLOYEE,
    ADMIN
}

data class User(
    val id: Int,
    val email: String,
    val mobile: String,
    val name: String,
    val role: Role,
    val avatarUrl: String? = null,
    val employeeCode: String? = null,
    val designation: String? = null,
    val department: String? = null,
    val badgeNumber: String? = null,
    val assignedGate: String? = null
)

data class Department(
    val id: Int,
    val code: String,
    val name: String,
    val description: String?
)

data class Gate(
    val id: Int,
    val code: String,
    val name: String,
    val gateType: String,
    val active: Boolean = true
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    ENTRY_GRANTED,
    EXPIRED
}

data class VisitRequest(
    val id: Int,
    val visitorName: String,
    val visitorMobile: String,
    val visitorCompany: String,
    val purpose: String,
    val idProofType: String = "National ID",
    val idProofNumber: String = "",
    val vehicleNumber: String? = null,
    val hostEmployeeId: Int,
    val hostName: String = "",
    val hostEmail: String = "",
    val gateId: Int = 1,
    val gateName: String = "Main Gate",
    val guardUserId: Int = 0,
    val status: RequestStatus = RequestStatus.PENDING,
    val decisionTime: String? = null,
    val decisionReason: String? = null,
    val meetingRoom: String? = null,
    val createdAt: String = ""
)

enum class AppointmentStatus {
    SCHEDULED,
    ARRIVED,
    COMPLETED,
    CANCELLED,
    EXPIRED
}

data class Appointment(
    val id: Int,
    val visitorName: String,
    val visitorMobile: String,
    val visitorCompany: String,
    val visitorEmail: String? = null,
    val hostEmployeeId: Int,
    val hostName: String = "",
    val departmentId: Int = 1,
    val purpose: String,
    val expectedDateTime: String,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val otpCode: String,
    val qrToken: String,
    val otpExpiresAt: String,
    val otpUsed: Boolean = false,
    val createdAt: String = ""
)

enum class VisitStatus {
    INSIDE,
    COMPLETED,
    OVERSTAY
}

data class Visit(
    val id: Int,
    val requestId: Int? = null,
    val appointmentId: Int? = null,
    val visitType: String = "WALK_IN", // WALK_IN or PRE_REGISTERED
    val visitorName: String,
    val visitorMobile: String,
    val visitorCompany: String,
    val purpose: String,
    val idProofType: String? = null,
    val idProofNumber: String? = null,
    val vehicleNumber: String? = null,
    val hostEmployeeId: Int,
    val hostName: String = "",
    val gateInId: Int = 1,
    val gateInName: String = "Main Gate",
    val gateOutId: Int? = null,
    val gateOutName: String? = null,
    val status: VisitStatus = VisitStatus.INSIDE,
    val entryTime: String,
    val exitTime: String? = null,
    val totalDurationMinutes: Int? = null,
    val employeeVerified: Boolean = false,
    val employeeVerifiedTime: String? = null,
    val employeeSignatureData: String? = null,
    val verificationNotes: String? = null,
    val notes: String? = null
)

data class NotificationItem(
    val id: Int,
    val title: String,
    val body: String,
    val type: String,
    val requestId: Int? = null,
    val visitId: Int? = null,
    val isRead: Boolean = false,
    val timestamp: String
)

data class AuditLogEntry(
    val id: Int,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val createdAt: String
)

data class SystemStats(
    val visitorsToday: Int = 0,
    val currentlyInside: Int = 0,
    val pendingApprovals: Int = 0,
    val totalAppointments: Int = 0,
    val scheduledAppointments: Int = 0,
    val completedVisits: Int = 0,
    val activeGuardsCount: Int = 0,
    val totalEmployeesCount: Int = 0,
    val gatesCount: Int = 0
)

data class PendingUser(
    val id: Int,
    val email: String,
    val mobile: String,
    val name: String,
    val role: Role,
    val createdAt: String
)

data class AdminUserItem(
    val id: Int,
    val email: String,
    val mobile: String,
    val name: String,
    val role: Role,
    val active: Boolean,
    val createdAt: String,
    val approvedAt: String? = null
)

data class EmployeeHostItem(
    val id: Int,
    val name: String,
    val email: String,
    val mobile: String,
    val employeeCode: String,
    val designation: String,
    val department: String
)

data class ServerConfig(
    val baseUrl: String = "https://vms-backend-3n5i.onrender.com/api/",
    val useLiveServer: Boolean = true,
    val isOnline: Boolean = true,
    val lastPingMs: Long = -1,
    val isDevOtpMode: Boolean = true
)
