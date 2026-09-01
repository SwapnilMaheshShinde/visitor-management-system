package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.NetworkManager
import com.example.data.repository.VmsRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VmsRepository.getInstance(application)
    private val networkManager = NetworkManager.getInstance(application)

    val currentUser: StateFlow<User?> = repository.currentUser
    val requests: StateFlow<List<VisitRequest>> = repository.requests
    val appointments: StateFlow<List<Appointment>> = repository.appointments
    val insideVisits: StateFlow<List<Visit>> = repository.insideVisits
    val visitHistory: StateFlow<List<Visit>> = repository.visitHistory
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val auditLogs: StateFlow<List<AuditLogEntry>> = repository.auditLogs
    val stats: StateFlow<SystemStats> = repository.stats
    val pendingUsers: StateFlow<List<PendingUser>> = repository.pendingUsers
    val adminUsers: StateFlow<List<AdminUserItem>> = repository.adminUsers
    val availableEmployees: StateFlow<List<EmployeeHostItem>> = repository.availableEmployees

    val baseUrl: StateFlow<String> = networkManager.baseUrlState
    val isServerConnected: StateFlow<Boolean> = networkManager.isServerConnected
    val latencyMs: StateFlow<Long> = networkManager.lastPingLatencyMs
    val lastDevOtp: StateFlow<String> = networkManager.lastDevOtp

    private val _isDevOtpMode = MutableStateFlow(networkManager.isDevOtpMode())
    val isDevOtpMode: StateFlow<Boolean> = _isDevOtpMode.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            networkManager.testConnection()
            repository.syncDataFromServer()
        }

        // Reactively manage live server polling loop whenever a user is logged in
        viewModelScope.launch {
            currentUser.collect { user ->
                syncJob?.cancel()
                if (user != null) {
                    registerCurrentFcmToken()
                    startLiveSyncLoop()
                }
            }
        }
    }

    private fun startLiveSyncLoop() {
        syncJob = viewModelScope.launch {
            while (isActive) {
                try {
                    repository.syncDataFromServer()
                } catch (e: Exception) {
                    Log.w("VmsViewModel", "Live sync loop exception: ${e.message}")
                }
                delay(3000) // Poll shared backend every 3 seconds for instant real-time multi-device sync
            }
        }
    }

    private fun registerCurrentFcmToken() {
        try {
            val fcm = FirebaseMessaging.getInstance()
            // Set auto init to true on demand when user logs in
            fcm.isAutoInitEnabled = true
            fcm.token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result
                    Log.d("VmsViewModel", "Retrieved FCM Token: ${token.take(15)}...")
                    viewModelScope.launch {
                        repository.registerFcmDeviceToken(token)
                    }
                } else {
                    Log.i("VmsViewModel", "FCM token not available in current environment (${task.exception?.message ?: "Not registered"}); live sync active.")
                }
            }
        } catch (e: Exception) {
            Log.i("VmsViewModel", "Firebase messaging not available in current environment: ${e.message}")
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncDataFromServer()
            _isLoading.value = false
        }
    }

    fun register(
        email: String,
        mobile: String,
        password: String,
        name: String,
        role: Role,
        employeeCode: String? = null,
        departmentId: Int? = null,
        designation: String? = null,
        badgeNumber: String? = null,
        gateId: Int? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.registerUser(
                email = email,
                mobile = mobile,
                password = password,
                name = name,
                role = role,
                employeeCode = employeeCode,
                departmentId = departmentId,
                designation = designation,
                badgeNumber = badgeNumber,
                gateId = gateId
            )
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = res.getOrNull() ?: "Registration submitted! Pending Admin approval."
                onSuccess()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Registration failed"
            }
        }
    }

    fun login(identifier: String, password: String?, role: Role?) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.login(identifier, password, role)
            _isLoading.value = false
            if (res.isSuccess) {
                val user = res.getOrNull()
                _uiMessage.value = "Welcome, ${user?.name} (${user?.role})"
                registerCurrentFcmToken()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun logout() {
        syncJob?.cancel()
        repository.logout()
        _uiMessage.value = "Logged out successfully"
    }

    fun approveUser(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.approveUser(userId)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "User approved and activated in PostgreSQL database."
                repository.fetchPendingUsers()
                repository.fetchAdminUsers()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to approve user"
            }
        }
    }

    fun rejectUser(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.rejectUser(userId)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "User registration rejected."
                repository.fetchPendingUsers()
                repository.fetchAdminUsers()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to reject user"
            }
        }
    }

    fun fetchAdminData() {
        viewModelScope.launch {
            repository.syncDataFromServer()
            repository.fetchPendingUsers()
            repository.fetchAdminUsers()
            repository.fetchAdminAuditLogs()
        }
    }

    fun submitWalkIn(
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
        gateName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.submitWalkInRequest(
                visitorName = visitorName,
                visitorMobile = visitorMobile,
                visitorCompany = visitorCompany,
                purpose = purpose,
                idProofType = idProofType,
                idProofNumber = idProofNumber,
                vehicleNumber = vehicleNumber,
                hostEmployeeId = hostEmployeeId,
                hostName = hostName,
                gateId = gateId,
                gateName = gateName
            )
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "Request submitted for $visitorName. Host notified via push."
                onSuccess()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to submit request"
            }
        }
    }

    fun decideRequest(requestId: Int, accept: Boolean, reason: String? = null, meetingRoom: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.submitRequestDecision(requestId, accept, reason, meetingRoom)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = if (accept) "Request approved. Guard notified." else "Request declined."
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to record decision"
            }
        }
    }

    fun grantEntry(requestId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.grantEntryForRequest(requestId)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "Entry granted! Visitor is now INSIDE premises."
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to grant entry"
            }
        }
    }

    fun createAppointment(
        visitorName: String,
        visitorMobile: String,
        visitorCompany: String,
        visitorEmail: String?,
        purpose: String,
        expectedDateTime: String,
        departmentId: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createAppointment(
                visitorName = visitorName,
                visitorMobile = visitorMobile,
                visitorCompany = visitorCompany,
                visitorEmail = visitorEmail,
                purpose = purpose,
                expectedDateTime = expectedDateTime,
                departmentId = departmentId
            )
            _isLoading.value = false
            if (res.isSuccess) {
                val appt = res.getOrNull()!!
                _uiMessage.value = "Pass generated! Pass OTP: ${appt.otpCode}"
                onSuccess()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to create appointment"
            }
        }
    }

    fun verifyOtp(otp: String, gateId: Int = 1, gateName: String = "Main Gate", onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyOtpAndGrantEntry(otp, gateId, gateName)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "OTP Verified! Entry granted for ${res.getOrNull()?.visitorName}."
                onSuccess()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "OTP Verification failed"
            }
        }
    }

    fun verifyQr(qrToken: String, gateId: Int = 1, gateName: String = "Main Gate", onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyQrAndGrantEntry(qrToken, gateId, gateName)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "QR Pass Verified! Entry granted for ${res.getOrNull()?.visitorName}."
                onSuccess()
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "QR Verification failed"
            }
        }
    }

    fun verifyMeeting(visitId: Int, signatureData: String, notes: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyMeetingWithSignature(visitId, signatureData, notes)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "Meeting verified & digitally signed."
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to verify meeting"
            }
        }
    }

    fun markExit(visitId: Int, gateOutName: String = "Main Gate") {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.markExit(visitId, gateOutName)
            _isLoading.value = false
            if (res.isSuccess) {
                _uiMessage.value = "Visitor checkout completed. Duration recorded."
            } else {
                _uiMessage.value = res.exceptionOrNull()?.message ?: "Failed to mark exit"
            }
        }
    }

    suspend fun testPing(): Pair<Boolean, Long> {
        return networkManager.testConnection()
    }

    fun updateBaseUrl(newUrl: String) {
        networkManager.setBaseUrl(newUrl)
    }

    fun toggleDevOtp(enabled: Boolean) {
        networkManager.setDevOtpMode(enabled)
        _isDevOtpMode.value = enabled
    }
}
