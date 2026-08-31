package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.VmsDatabase
import com.example.data.model.*
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VmsRepository private constructor(
    private val context: Context,
    private val networkManager: NetworkManager,
    private val database: VmsDatabase
) {
    companion object {
        private const val TAG = "VmsRepository"

        @Volatile
        private var INSTANCE: VmsRepository? = null

        fun getInstance(context: Context): VmsRepository {
            return INSTANCE ?: synchronized(this) {
                val netMgr = NetworkManager.getInstance(context)
                val db = VmsDatabase.getInstance(context)
                val instance = VmsRepository(context.applicationContext, netMgr, db)
                INSTANCE = instance
                instance
            }
        }
    }

    private val dao = database.vmsDao()

    // Current logged-in user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Remote Shared Data Cache
    private val _requests = MutableStateFlow<List<VisitRequest>>(emptyList())
    val requests: StateFlow<List<VisitRequest>> = _requests.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _insideVisits = MutableStateFlow<List<Visit>>(emptyList())
    val insideVisits: StateFlow<List<Visit>> = _insideVisits.asStateFlow()

    private val _visitHistory = MutableStateFlow<List<Visit>>(emptyList())
    val visitHistory: StateFlow<List<Visit>> = _visitHistory.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _stats = MutableStateFlow(SystemStats())
    val stats: StateFlow<SystemStats> = _stats.asStateFlow()

    private val _pendingUsers = MutableStateFlow<List<PendingUser>>(emptyList())
    val pendingUsers: StateFlow<List<PendingUser>> = _pendingUsers.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<AdminUserItem>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserItem>> = _adminUsers.asStateFlow()

    private val _availableEmployees = MutableStateFlow<List<EmployeeHostItem>>(emptyList())
    val availableEmployees: StateFlow<List<EmployeeHostItem>> = _availableEmployees.asStateFlow()

    private fun updateComputedStats() {
        val insideCount = _insideVisits.value.size
        val completedCount = _visitHistory.value.size
        val pendingCount = _requests.value.count { it.status == RequestStatus.PENDING }
        val scheduledCount = _appointments.value.count { it.status == AppointmentStatus.SCHEDULED }

        _stats.value = SystemStats(
            visitorsToday = insideCount + completedCount,
            currentlyInside = insideCount,
            pendingApprovals = pendingCount,
            totalAppointments = _appointments.value.size,
            scheduledAppointments = scheduledCount,
            completedVisits = completedCount,
            activeGuardsCount = _adminUsers.value.count { it.role == Role.GUARD && it.active },
            totalEmployeesCount = _availableEmployees.value.size,
            gatesCount = 3
        )
    }

    // =========================================================================
    // AUTHENTICATION & REGISTRATION (SHARED CLOUD BACKEND)
    // =========================================================================

    suspend fun registerUser(
        email: String,
        mobile: String,
        password: String,
        name: String,
        role: Role,
        employeeCode: String? = null,
        departmentId: Int? = null,
        designation: String? = null,
        badgeNumber: String? = null,
        gateId: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = networkManager.getApiService()
            val response = api.register(
                RegisterRequestDto(
                    email = email.trim(),
                    mobile = mobile.trim(),
                    password = password,
                    name = name.trim(),
                    role = role.name,
                    employeeCode = employeeCode,
                    departmentId = departmentId,
                    designation = designation,
                    badgeNumber = badgeNumber,
                    gateId = gateId
                )
            )

            if (response.isSuccessful) {
                val msg = response.body()?.message ?: "Registration submitted successfully! Pending Administrator approval."
                return@withContext Result.success(msg)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val err = if (response.code() == 409) {
                    "An account with this email or mobile number already exists in the central database."
                } else {
                    "Registration failed (${response.code()}): $errorBody"
                }
                return@withContext Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            val serverUrl = networkManager.getBaseUrl()
            Log.e(TAG, "Registration connection failure to $serverUrl: ${e.message}")
            return@withContext Result.failure(
                Exception("Cannot reach central backend server at $serverUrl. Please verify your internet connection and backend URL.")
            )
        }
    }

    suspend fun login(identifier: String, password: String?, selectedRole: Role?): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val api = networkManager.getApiService()
                val response = api.login(
                    LoginRequestDto(
                        identifier = identifier.trim(),
                        password = password ?: "",
                        role = selectedRole?.name
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    networkManager.setAuthToken(body.token)
                    val u = body.user
                    val user = User(
                        id = u.id,
                        email = u.email,
                        mobile = u.mobile,
                        name = u.name,
                        role = try { Role.valueOf(u.role.uppercase()) } catch (e: Exception) { Role.EMPLOYEE },
                        avatarUrl = u.avatar
                    )
                    _currentUser.value = user
                    syncDataFromServer()
                    return@withContext Result.success(user)
                } else if (response.code() == 403) {
                    return@withContext Result.failure(
                        Exception("Your registration is pending Administrator approval. Once an Admin approves your account, you will be able to log in.")
                    )
                } else if (response.code() == 401) {
                    return@withContext Result.failure(
                        Exception("Invalid credentials. Please verify your password.")
                    )
                } else if (response.code() == 404) {
                    return@withContext Result.failure(
                        Exception("No account found with identifier '$identifier'. Please register first.")
                    )
                } else {
                    val err = response.errorBody()?.string() ?: "Login failed with code ${response.code()}"
                    return@withContext Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                val serverUrl = networkManager.getBaseUrl()
                Log.e(TAG, "Login network error connecting to $serverUrl: ${e.message}")
                return@withContext Result.failure(
                    Exception("Failed to connect to central server at $serverUrl. Please ensure the cloud backend is running and reachable over HTTPS.")
                )
            }
        }

    fun logout() {
        _currentUser.value = null
        networkManager.clearAuthToken()
        _requests.value = emptyList()
        _appointments.value = emptyList()
        _insideVisits.value = emptyList()
        _visitHistory.value = emptyList()
        _pendingUsers.value = emptyList()
        _adminUsers.value = emptyList()
    }

    suspend fun registerFcmDeviceToken(fcmToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = networkManager.getAuthToken()
        if (token.isEmpty() || fcmToken.isEmpty()) return@withContext Result.success(Unit)
        try {
            val api = networkManager.getApiService()
            val res = api.registerFcm(token, RegisterFcmDto(fcmToken = fcmToken))
            if (res.isSuccessful) {
                Log.d(TAG, "Registered FCM token with backend successfully: ${fcmToken.take(12)}...")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register FCM token (${res.code()})"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM token register network error: ${e.message}")
            Result.failure(e)
        }
    }

    // =========================================================================
    // SYNC SERVER DATA (MULTI-DEVICE LIVE SYNCHRONIZATION)
    // =========================================================================

    suspend fun syncDataFromServer() = withContext(Dispatchers.IO) {
        val token = networkManager.getAuthToken()
        val user = _currentUser.value ?: return@withContext

        try {
            val api = networkManager.getApiService()

            // 1. Sync Employees
            try {
                val empRes = api.getMetaEmployees()
                if (empRes.isSuccessful && empRes.body() != null) {
                    _availableEmployees.value = empRes.body()!!.map {
                        EmployeeHostItem(
                            id = it.id,
                            name = it.name,
                            email = it.email,
                            mobile = it.mobile,
                            employeeCode = it.employeeCode ?: "EMP-${it.id}",
                            designation = it.designation ?: "Staff",
                            department = it.department ?: "General"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync employees: ${e.message}")
            }

            // 2. Sync Requests
            try {
                val reqRes = if (user.role == Role.EMPLOYEE) {
                    api.getRequests(token, hostId = user.id)
                } else {
                    api.getRequests(token)
                }
                if (reqRes.isSuccessful && reqRes.body() != null) {
                    _requests.value = reqRes.body()!!.map { r ->
                        VisitRequest(
                            id = r.id,
                            visitorName = r.visitorName,
                            visitorMobile = r.visitorMobile,
                            visitorCompany = r.visitorCompany ?: "Guest Visitor",
                            purpose = r.purpose,
                            idProofType = r.idProofType ?: "National ID",
                            idProofNumber = r.idProofNumber ?: "",
                            vehicleNumber = r.vehicleNumber,
                            hostEmployeeId = r.hostEmployeeId,
                            hostName = r.hostName ?: "Host Employee",
                            gateId = r.gateId ?: 1,
                            gateName = r.gateName ?: "Main Gate",
                            guardUserId = 2,
                            status = try { RequestStatus.valueOf(r.status.uppercase()) } catch (e: Exception) { RequestStatus.PENDING },
                            decisionTime = r.decisionTime,
                            decisionReason = r.decisionReason,
                            meetingRoom = r.meetingRoom,
                            createdAt = r.createdAt ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync requests: ${e.message}")
            }

            // 3. Sync Appointments
            try {
                val apptRes = if (user.role == Role.EMPLOYEE) {
                    api.getAppointments(token, hostId = user.id)
                } else {
                    api.getAppointments(token)
                }
                if (apptRes.isSuccessful && apptRes.body() != null) {
                    _appointments.value = apptRes.body()!!.map { a ->
                        Appointment(
                            id = a.id,
                            visitorName = a.visitorName,
                            visitorMobile = a.visitorMobile,
                            visitorCompany = a.visitorCompany ?: "Partner Guest",
                            visitorEmail = a.visitorEmail,
                            hostEmployeeId = a.hostEmployeeId,
                            hostName = a.hostName ?: "Host",
                            departmentId = 1,
                            purpose = a.purpose,
                            expectedDateTime = a.expectedDateTime,
                            status = try { AppointmentStatus.valueOf(a.status.uppercase()) } catch (e: Exception) { AppointmentStatus.SCHEDULED },
                            otpCode = a.otpCode,
                            qrToken = a.qrToken,
                            otpExpiresAt = a.otpExpiresAt,
                            otpUsed = a.otpUsed,
                            createdAt = a.createdAt ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync appointments: ${e.message}")
            }

            // 4. Sync Active Inside Visits
            try {
                val insideRes = if (user.role == Role.EMPLOYEE) {
                    api.getInsideVisits(token, hostId = user.id)
                } else {
                    api.getInsideVisits(token)
                }
                if (insideRes.isSuccessful && insideRes.body() != null) {
                    _insideVisits.value = insideRes.body()!!.map { v ->
                        Visit(
                            id = v.id,
                            requestId = v.requestId,
                            appointmentId = v.appointmentId,
                            visitType = v.visitType ?: "WALK_IN",
                            visitorName = v.visitorName,
                            visitorMobile = v.visitorMobile,
                            visitorCompany = v.visitorCompany ?: "Guest Visitor",
                            purpose = v.purpose,
                            idProofType = "ID Verified",
                            idProofNumber = "",
                            hostEmployeeId = v.hostEmployeeId,
                            hostName = v.hostName ?: "Host",
                            gateInId = v.gateInId ?: 1,
                            gateInName = v.gateInName ?: "Main Gate",
                            status = VisitStatus.INSIDE,
                            entryTime = v.entryTime ?: "",
                            exitTime = v.exitTime,
                            totalDurationMinutes = v.totalDurationMinutes,
                            employeeVerified = v.employeeVerified,
                            employeeVerifiedTime = v.employeeVerifiedTime,
                            employeeSignatureData = v.employeeSignatureData,
                            verificationNotes = v.verificationNotes,
                            notes = v.notes
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync inside visits: ${e.message}")
            }

            // 5. Sync Visit History
            try {
                val historyRes = if (user.role == Role.EMPLOYEE) {
                    api.getVisitHistory(token, hostId = user.id)
                } else {
                    api.getVisitHistory(token)
                }
                if (historyRes.isSuccessful && historyRes.body() != null) {
                    _visitHistory.value = historyRes.body()!!.map { v ->
                        Visit(
                            id = v.id,
                            requestId = v.requestId,
                            appointmentId = v.appointmentId,
                            visitType = v.visitType ?: "WALK_IN",
                            visitorName = v.visitorName,
                            visitorMobile = v.visitorMobile,
                            visitorCompany = v.visitorCompany ?: "Guest Visitor",
                            purpose = v.purpose,
                            idProofType = "ID Verified",
                            idProofNumber = "",
                            hostEmployeeId = v.hostEmployeeId,
                            hostName = v.hostName ?: "Host",
                            gateInId = v.gateInId ?: 1,
                            gateInName = v.gateInName ?: "Main Gate",
                            status = VisitStatus.COMPLETED,
                            entryTime = v.entryTime ?: "",
                            exitTime = v.exitTime,
                            totalDurationMinutes = v.totalDurationMinutes,
                            employeeVerified = v.employeeVerified,
                            employeeVerifiedTime = v.employeeVerifiedTime,
                            employeeSignatureData = v.employeeSignatureData,
                            verificationNotes = v.verificationNotes,
                            notes = v.notes
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync visit history: ${e.message}")
            }

            // 6. Sync Admin Data if User is Admin
            if (user.role == Role.ADMIN) {
                try {
                    val statsRes = api.getAdminStats(token)
                    if (statsRes.isSuccessful && statsRes.body() != null) {
                        val s = statsRes.body()!!
                        _stats.value = SystemStats(
                            visitorsToday = s.visitorsToday,
                            currentlyInside = s.currentlyInside,
                            pendingApprovals = s.pendingApprovals,
                            totalAppointments = s.totalAppointments,
                            scheduledAppointments = s.scheduledAppointments,
                            completedVisits = s.completedVisits,
                            activeGuardsCount = s.activeGuardsCount,
                            totalEmployeesCount = s.totalEmployeesCount,
                            gatesCount = s.gatesCount
                        )
                    }
                    fetchPendingUsers()
                    fetchAdminUsers()
                    fetchAdminAuditLogs()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync admin stats: ${e.message}")
                }
            }

            updateComputedStats()
        } catch (e: Exception) {
            Log.e(TAG, "Sync all data failed: ${e.message}")
        }
    }

    // =========================================================================
    // ADMIN APPROVAL & USER MANAGEMENT
    // =========================================================================

    suspend fun fetchPendingUsers(): Result<List<PendingUser>> = withContext(Dispatchers.IO) {
        val token = networkManager.getAuthToken()
        try {
            val api = networkManager.getApiService()
            val res = api.getPendingUsers(token)
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!.map { p ->
                    PendingUser(
                        id = p.id,
                        email = p.email,
                        mobile = p.mobile,
                        name = p.fullName,
                        role = try { Role.valueOf(p.role.uppercase()) } catch (e: Exception) { Role.EMPLOYEE },
                        createdAt = p.createdAt ?: ""
                    )
                }
                _pendingUsers.value = list
                return@withContext Result.success(list)
            } else {
                return@withContext Result.failure(Exception("Failed to fetch pending users: ${res.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch pending users failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun approveUser(userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val token = networkManager.getAuthToken()
        try {
            val api = networkManager.getApiService()
            val res = api.approveUser(token, userId)
            if (res.isSuccessful) {
                syncDataFromServer()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to approve user: ${res.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Approve user failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun rejectUser(userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val token = networkManager.getAuthToken()
        try {
            val api = networkManager.getApiService()
            val res = api.rejectUser(token, userId)
            if (res.isSuccessful) {
                syncDataFromServer()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to reject user: ${res.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reject user failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchAdminUsers(): Result<List<AdminUserItem>> = withContext(Dispatchers.IO) {
        val token = networkManager.getAuthToken()
        try {
            val api = networkManager.getApiService()
            val res = api.getAdminUsers(token)
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!.map { u ->
                    AdminUserItem(
                        id = u.id,
                        email = u.email,
                        mobile = u.mobile,
                        name = u.fullName,
                        role = try { Role.valueOf(u.role.uppercase()) } catch (e: Exception) { Role.EMPLOYEE },
                        active = u.active,
                        createdAt = u.createdAt ?: "",
                        approvedAt = u.approvedAt
                    )
                }
                _adminUsers.value = list
                return@withContext Result.success(list)
            } else {
                return@withContext Result.failure(Exception("Failed to fetch users: ${res.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch admin users failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchAdminAuditLogs(): Result<List<AuditLogEntry>> = withContext(Dispatchers.IO) {
        val token = networkManager.getAuthToken()
        try {
            val api = networkManager.getApiService()
            val res = api.getAdminAuditLogs(token)
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!.map { l ->
                    AuditLogEntry(
                        id = l.id,
                        action = l.action,
                        entityType = l.entityType,
                        entityId = l.entityId,
                        details = l.details,
                        createdAt = l.createdAt
                    )
                }
                _auditLogs.value = list
                return@withContext Result.success(list)
            } else {
                return@withContext Result.failure(Exception("Failed to fetch audit logs: ${res.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch audit logs failed: ${e.message}")
            Result.failure(e)
        }
    }

    // =========================================================================
    // WALK-IN REQUEST WORKFLOW (GUARD -> HTTPS API -> FCM -> EMPLOYEE)
    // =========================================================================

    suspend fun submitWalkInRequest(
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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = networkManager.getApiService()
            val token = networkManager.getAuthToken()
            val res = api.createRequest(
                token = token,
                body = CreateRequestDto(
                    visitorName = visitorName.trim(),
                    visitorMobile = visitorMobile.trim(),
                    visitorCompany = visitorCompany.trim(),
                    purpose = purpose.trim(),
                    idProofType = idProofType,
                    idProofNumber = idProofNumber.trim(),
                    vehicleNumber = vehicleNumber?.trim(),
                    hostEmployeeId = hostEmployeeId,
                    gateId = gateId
                )
            )

            if (res.isSuccessful) {
                syncDataFromServer()
                Result.success(Unit)
            } else {
                val err = res.errorBody()?.string() ?: "Failed with code ${res.code()}"
                Result.failure(Exception("Server rejected walk-in request: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Submit walk-in failed: ${e.message}")
            Result.failure(Exception("Cannot connect to server to submit walk-in: ${e.message}"))
        }
    }

    suspend fun submitRequestDecision(
        requestId: Int,
        accept: Boolean,
        reason: String? = null,
        meetingRoom: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = networkManager.getApiService()
            val token = networkManager.getAuthToken()
            val res = api.submitRequestDecision(
                token = token,
                requestId = requestId,
                body = RequestDecisionDto(
                    decision = if (accept) "ACCEPTED" else "DECLINED",
                    reason = reason,
                    meetingRoom = meetingRoom
                )
            )

            if (res.isSuccessful) {
                syncDataFromServer()
                Result.success(Unit)
            } else {
                val err = res.errorBody()?.string() ?: "Failed with code ${res.code()}"
                Result.failure(Exception("Server failed to record decision: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decision submit failed: ${e.message}")
            Result.failure(Exception("Network error submitting decision: ${e.message}"))
        }
    }

    suspend fun grantEntryForRequest(requestId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = networkManager.getApiService()
            val token = networkManager.getAuthToken()
            val res = api.grantEntry(token, requestId)

            if (res.isSuccessful) {
                syncDataFromServer()
                Result.success(Unit)
            } else {
                val err = res.errorBody()?.string() ?: "Failed code ${res.code()}"
                Result.failure(Exception("Server rejected entry grant: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Grant entry failed: ${e.message}")
            Result.failure(Exception("Network error granting entry: ${e.message}"))
        }
    }

    // =========================================================================
    // PRE-REGISTERED APPOINTMENTS WORKFLOW (EMPLOYEE -> SERVER -> OTP/QR)
    // =========================================================================

    suspend fun createAppointment(
        visitorName: String,
        visitorMobile: String,
        visitorCompany: String,
        visitorEmail: String?,
        purpose: String,
        expectedDateTime: String,
        departmentId: Int
    ): Result<Appointment> = withContext(Dispatchers.IO) {
        try {
            val api = networkManager.getApiService()
            val token = networkManager.getAuthToken()
            val res = api.createAppointment(
                token = token,
                body = CreateAppointmentDto(
                    visitorName = visitorName.trim(),
                    visitorMobile = visitorMobile.trim(),
                    visitorCompany = visitorCompany.trim(),
                    visitorEmail = visitorEmail?.trim(),
                    purpose = purpose.trim(),
                    expectedDateTime = expectedDateTime.trim(),
                    departmentId = departmentId
                )
            )

            if (res.isSuccessful) {
                syncDataFromServer()
                val latest = _appointments.value.firstOrNull { it.visitorMobile == visitorMobile.trim() }
                if (latest != null) {
                    networkManager.setLastDevOtp(latest.otpCode)
                    return@withContext Result.success(latest)
                }
                val fallbackAppt = Appointment(
                    id = System.currentTimeMillis().toInt(),
                    visitorName = visitorName,
                    visitorMobile = visitorMobile,
                    visitorCompany = visitorCompany,
                    visitorEmail = visitorEmail,
                    hostEmployeeId = _currentUser.value?.id ?: 1,
                    hostName = _currentUser.value?.name ?: "Host",
                    departmentId = departmentId,
                    purpose = purpose,
                    expectedDateTime = expectedDateTime,
                    status = AppointmentStatus.SCHEDULED,
                    otpCode = networkManager.getLastDevOtp(),
                    qrToken = "VMS-PASS-${networkManager.getLastDevOtp()}-SEC",
                    otpExpiresAt = "Tomorrow, 11:59 PM",
                    otpUsed = false,
                    createdAt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                )
                Result.success(fallbackAppt)
            } else {
                val err = res.errorBody()?.string() ?: "Failed code ${res.code()}"
                Result.failure(Exception("Server failed to generate appointment: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create appointment failed: ${e.message}")
            Result.failure(Exception("Cannot connect to server to generate pass: ${e.message}"))
        }
    }

    // =========================================================================
    // OTP & QR VERIFICATION AT GUARD CHECKPOINT
    // =========================================================================

    suspend fun verifyOtpAndGrantEntry(otp: String, gateId: Int, gateName: String): Result<Visit> =
        withContext(Dispatchers.IO) {
            val cleanOtp = otp.trim()
            try {
                val api = networkManager.getApiService()
                val token = networkManager.getAuthToken()
                val res = api.verifyOtp(token, VerifyOtpDto(otp = cleanOtp, gateId = gateId))
                if (res.isSuccessful) {
                    syncDataFromServer()
                    val visit = _insideVisits.value.firstOrNull() ?: Visit(
                        id = System.currentTimeMillis().toInt(),
                        visitorName = "Pre-Registered Guest",
                        visitorMobile = "",
                        visitorCompany = "Partner Guest",
                        purpose = "Verified Meeting",
                        hostEmployeeId = 1,
                        hostName = "Host",
                        status = VisitStatus.INSIDE,
                        entryTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    )
                    Result.success(visit)
                } else {
                    val err = res.errorBody()?.string() ?: "Invalid or Expired OTP"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Log.e(TAG, "OTP verification network error: ${e.message}")
                Result.failure(Exception("Failed to verify OTP with cloud database: ${e.message}"))
            }
        }

    suspend fun verifyQrAndGrantEntry(qrData: String, gateId: Int, gateName: String): Result<Visit> =
        withContext(Dispatchers.IO) {
            val token = qrData.trim()
            try {
                val api = networkManager.getApiService()
                val authToken = networkManager.getAuthToken()
                val res = api.verifyQr(authToken, VerifyQrDto(qrToken = token, gateId = gateId))
                if (res.isSuccessful) {
                    syncDataFromServer()
                    val visit = _insideVisits.value.firstOrNull() ?: Visit(
                        id = System.currentTimeMillis().toInt(),
                        visitorName = "Pre-Registered Guest",
                        visitorMobile = "",
                        visitorCompany = "Partner Guest",
                        purpose = "Verified Meeting",
                        hostEmployeeId = 1,
                        hostName = "Host",
                        status = VisitStatus.INSIDE,
                        entryTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    )
                    Result.success(visit)
                } else {
                    val err = res.errorBody()?.string() ?: "Invalid or Expired QR Pass"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Log.e(TAG, "QR verification network error: ${e.message}")
                Result.failure(Exception("Failed to verify QR with cloud database: ${e.message}"))
            }
        }

    // =========================================================================
    // ACTIVE VISITS: MEETING VERIFICATION & CHECKOUT
    // =========================================================================

    suspend fun verifyMeetingWithSignature(
        visitId: Int,
        signatureData: String,
        notes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = networkManager.getApiService()
            val token = networkManager.getAuthToken()
            val res = api.verifyMeeting(token, visitId, VerifyMeetingDto(signatureData, notes))
            if (res.isSuccessful) {
                syncDataFromServer()
                Result.success(Unit)
            } else {
                val err = res.errorBody()?.string() ?: "Failed code ${res.code()}"
                Result.failure(Exception("Server failed to save signature: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification network error: ${e.message}")
            Result.failure(Exception("Failed to submit digital signature: ${e.message}"))
        }
    }

    suspend fun markExit(visitId: Int, gateOutName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = networkManager.getApiService()
            val token = networkManager.getAuthToken()
            val res = api.markExit(token, visitId, MarkExitDto(gateOutId = 1))
            if (res.isSuccessful) {
                syncDataFromServer()
                Result.success(Unit)
            } else {
                val err = res.errorBody()?.string() ?: "Failed code ${res.code()}"
                Result.failure(Exception("Server failed to mark exit: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mark exit network error: ${e.message}")
            Result.failure(Exception("Failed to record visitor exit: ${e.message}"))
        }
    }
}
