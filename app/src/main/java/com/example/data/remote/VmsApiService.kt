package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// =========================================================================
// REQUEST & RESPONSE DTOs
// =========================================================================

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "password") val password: String? = null,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "mobile") val mobile: String,
    @Json(name = "password") val password: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String,
    @Json(name = "employeeCode") val employeeCode: String? = null,
    @Json(name = "departmentId") val departmentId: Int? = null,
    @Json(name = "designation") val designation: String? = null,
    @Json(name = "badgeNumber") val badgeNumber: String? = null,
    @Json(name = "gateId") val gateId: Int? = null
)

@JsonClass(generateAdapter = true)
data class PendingUserDto(
    @Json(name = "id") val id: Int,
    @Json(name = "email") val email: String,
    @Json(name = "mobile") val mobile: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "role") val role: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminUserItemDto(
    @Json(name = "id") val id: Int,
    @Json(name = "email") val email: String,
    @Json(name = "mobile") val mobile: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "role") val role: String,
    @Json(name = "active") val active: Boolean,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "approved_at") val approvedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AuditLogDto(
    @Json(name = "id") val id: Int,
    @Json(name = "action") val action: String,
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "details") val details: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class EmployeeMetaDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "mobile") val mobile: String,
    @Json(name = "employeeCode") val employeeCode: String? = null,
    @Json(name = "designation") val designation: String? = null,
    @Json(name = "department") val department: String? = null,
    @Json(name = "cabinLocation") val cabinLocation: String? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Int,
    @Json(name = "email") val email: String,
    @Json(name = "mobile") val mobile: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String,
    @Json(name = "avatar") val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    @Json(name = "token") val token: String,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class RegisterFcmDto(
    @Json(name = "fcmToken") val fcmToken: String
)

@JsonClass(generateAdapter = true)
data class CreateRequestDto(
    @Json(name = "visitorName") val visitorName: String,
    @Json(name = "visitorMobile") val visitorMobile: String,
    @Json(name = "visitorCompany") val visitorCompany: String? = null,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "idProofType") val idProofType: String = "National ID",
    @Json(name = "idProofNumber") val idProofNumber: String = "",
    @Json(name = "vehicleNumber") val vehicleNumber: String? = null,
    @Json(name = "hostEmployeeId") val hostEmployeeId: Int,
    @Json(name = "gateId") val gateId: Int = 1
)

@JsonClass(generateAdapter = true)
data class RequestDecisionDto(
    @Json(name = "decision") val decision: String, // ACCEPTED or DECLINED
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "meetingRoom") val meetingRoom: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateAppointmentDto(
    @Json(name = "visitorName") val visitorName: String,
    @Json(name = "visitorMobile") val visitorMobile: String,
    @Json(name = "visitorCompany") val visitorCompany: String? = null,
    @Json(name = "visitorEmail") val visitorEmail: String? = null,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "expectedDateTime") val expectedDateTime: String,
    @Json(name = "departmentId") val departmentId: Int = 1
)

@JsonClass(generateAdapter = true)
data class VerifyOtpDto(
    @Json(name = "otp") val otp: String,
    @Json(name = "gateId") val gateId: Int = 1
)

@JsonClass(generateAdapter = true)
data class VerifyQrDto(
    @Json(name = "qrToken") val qrToken: String,
    @Json(name = "gateId") val gateId: Int = 1
)

@JsonClass(generateAdapter = true)
data class VerifyMeetingDto(
    @Json(name = "signatureData") val signatureData: String,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class MarkExitDto(
    @Json(name = "gateOutId") val gateOutId: Int = 1
)

@JsonClass(generateAdapter = true)
data class VisitRequestDto(
    @Json(name = "id") val id: Int,
    @Json(name = "visitor_name") val visitorName: String,
    @Json(name = "visitor_mobile") val visitorMobile: String,
    @Json(name = "visitor_company") val visitorCompany: String? = null,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "id_proof_type") val idProofType: String? = "National ID",
    @Json(name = "id_proof_number") val idProofNumber: String? = "",
    @Json(name = "vehicle_number") val vehicleNumber: String? = null,
    @Json(name = "host_employee_id") val hostEmployeeId: Int,
    @Json(name = "host_name") val hostName: String? = null,
    @Json(name = "gate_id") val gateId: Int? = 1,
    @Json(name = "gate_name") val gateName: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "decision_time") val decisionTime: String? = null,
    @Json(name = "decision_reason") val decisionReason: String? = null,
    @Json(name = "meeting_room") val meetingRoom: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AppointmentDto(
    @Json(name = "id") val id: Int,
    @Json(name = "visitor_name") val visitorName: String,
    @Json(name = "visitor_mobile") val visitorMobile: String,
    @Json(name = "visitor_company") val visitorCompany: String? = null,
    @Json(name = "visitor_email") val visitorEmail: String? = null,
    @Json(name = "host_employee_id") val hostEmployeeId: Int,
    @Json(name = "host_name") val hostName: String? = null,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "expected_date_time") val expectedDateTime: String,
    @Json(name = "status") val status: String,
    @Json(name = "otp_code") val otpCode: String,
    @Json(name = "qr_token") val qrToken: String,
    @Json(name = "otp_expires_at") val otpExpiresAt: String,
    @Json(name = "otp_used") val otpUsed: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VisitDto(
    @Json(name = "id") val id: Int,
    @Json(name = "request_id") val requestId: Int? = null,
    @Json(name = "appointment_id") val appointmentId: Int? = null,
    @Json(name = "visit_type") val visitType: String? = "WALK_IN",
    @Json(name = "visitor_name") val visitorName: String,
    @Json(name = "visitor_mobile") val visitorMobile: String,
    @Json(name = "visitor_company") val visitorCompany: String? = null,
    @Json(name = "purpose") val purpose: String,
    @Json(name = "host_employee_id") val hostEmployeeId: Int,
    @Json(name = "host_name") val hostName: String? = null,
    @Json(name = "gate_in_id") val gateInId: Int? = 1,
    @Json(name = "gate_in_name") val gateInName: String? = "Main Gate",
    @Json(name = "gate_out_id") val gateOutId: Int? = null,
    @Json(name = "gate_out_name") val gateOutName: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "entry_time") val entryTime: String,
    @Json(name = "exit_time") val exitTime: String? = null,
    @Json(name = "total_duration_minutes") val totalDurationMinutes: Int? = null,
    @Json(name = "employee_verified") val employeeVerified: Boolean = false,
    @Json(name = "employee_verified_time") val employeeVerifiedTime: String? = null,
    @Json(name = "employee_signature_data") val employeeSignatureData: String? = null,
    @Json(name = "verification_notes") val verificationNotes: String? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class StatsDto(
    @Json(name = "visitorsToday") val visitorsToday: Int = 0,
    @Json(name = "currentlyInside") val currentlyInside: Int = 0,
    @Json(name = "pendingApprovals") val pendingApprovals: Int = 0,
    @Json(name = "totalAppointments") val totalAppointments: Int = 0,
    @Json(name = "scheduledAppointments") val scheduledAppointments: Int = 0,
    @Json(name = "completedVisits") val completedVisits: Int = 0,
    @Json(name = "activeGuardsCount") val activeGuardsCount: Int = 0,
    @Json(name = "totalEmployeesCount") val totalEmployeesCount: Int = 0,
    @Json(name = "gatesCount") val gatesCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class HealthCheckDto(
    @Json(name = "status") val status: String,
    @Json(name = "serverTime") val serverTime: String,
    @Json(name = "database") val database: String,
    @Json(name = "version") val version: String
)

@JsonClass(generateAdapter = true)
data class DevLastOtpDto(
    @Json(name = "lastOtp") val lastOtp: Map<String, Any?>?,
    @Json(name = "note") val note: String? = null
)

// =========================================================================
// RETROFIT API INTERFACE
// =========================================================================

interface VmsApiService {

    @GET("health")
    suspend fun checkHealth(): Response<HealthCheckDto>

    @GET("dev/last-otp")
    suspend fun getDevLastOtp(): Response<DevLastOtpDto>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<LoginResponseDto>

    @POST("auth/register-fcm")
    suspend fun registerFcm(
        @Header("Authorization") token: String,
        @Body body: RegisterFcmDto
    ): Response<ApiResponseDto>

    @GET("requests")
    suspend fun getRequests(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("hostEmployeeId") hostId: Int? = null
    ): Response<List<VisitRequestDto>>

    @POST("requests")
    suspend fun createRequest(
        @Header("Authorization") token: String,
        @Body body: CreateRequestDto
    ): Response<ApiResponseDto>

    @PUT("requests/{id}/decision")
    suspend fun submitRequestDecision(
        @Header("Authorization") token: String,
        @Path("id") requestId: Int,
        @Body body: RequestDecisionDto
    ): Response<ApiResponseDto>

    @POST("requests/{id}/grant-entry")
    suspend fun grantEntry(
        @Header("Authorization") token: String,
        @Path("id") requestId: Int
    ): Response<ApiResponseDto>

    @GET("appointments")
    suspend fun getAppointments(
        @Header("Authorization") token: String,
        @Query("query") query: String? = null,
        @Query("hostEmployeeId") hostId: Int? = null
    ): Response<List<AppointmentDto>>

    @POST("appointments")
    suspend fun createAppointment(
        @Header("Authorization") token: String,
        @Body body: CreateAppointmentDto
    ): Response<ApiResponseDto>

    @POST("verify/otp")
    suspend fun verifyOtp(
        @Header("Authorization") token: String,
        @Body body: VerifyOtpDto
    ): Response<ApiResponseDto>

    @POST("verify/qr")
    suspend fun verifyQr(
        @Header("Authorization") token: String,
        @Body body: VerifyQrDto
    ): Response<ApiResponseDto>

    @GET("visits/inside")
    suspend fun getInsideVisits(
        @Header("Authorization") token: String,
        @Query("hostEmployeeId") hostId: Int? = null
    ): Response<List<VisitDto>>

    @GET("visits/history")
    suspend fun getVisitHistory(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null,
        @Query("hostEmployeeId") hostId: Int? = null
    ): Response<List<VisitDto>>

    @PUT("visits/{id}/verify-met")
    suspend fun verifyMeeting(
        @Header("Authorization") token: String,
        @Path("id") visitId: Int,
        @Body body: VerifyMeetingDto
    ): Response<ApiResponseDto>

    @PUT("visits/{id}/exit")
    suspend fun markExit(
        @Header("Authorization") token: String,
        @Path("id") visitId: Int,
        @Body body: MarkExitDto
    ): Response<ApiResponseDto>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<ApiResponseDto>

    @GET("meta/employees")
    suspend fun getMetaEmployees(): Response<List<EmployeeMetaDto>>

    @GET("admin/pending-users")
    suspend fun getPendingUsers(
        @Header("Authorization") token: String
    ): Response<List<PendingUserDto>>

    @POST("admin/users/{id}/approve")
    suspend fun approveUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<ApiResponseDto>

    @POST("admin/users/{id}/reject")
    suspend fun rejectUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<ApiResponseDto>

    @GET("admin/users")
    suspend fun getAdminUsers(
        @Header("Authorization") token: String
    ): Response<List<AdminUserItemDto>>

    @GET("admin/audit-logs")
    suspend fun getAdminAuditLogs(
        @Header("Authorization") token: String
    ): Response<List<AuditLogDto>>

    @GET("admin/stats")
    suspend fun getAdminStats(
        @Header("Authorization") token: String
    ): Response<StatsDto>
}
