package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_visits")
data class VisitEntity(
    @PrimaryKey val id: Int,
    val requestId: Int?,
    val appointmentId: Int?,
    val visitType: String,
    val visitorName: String,
    val visitorMobile: String,
    val visitorCompany: String,
    val purpose: String,
    val hostEmployeeId: Int,
    val hostName: String,
    val gateInName: String,
    val status: String,
    val entryTime: String,
    val exitTime: String?,
    val totalDurationMinutes: Int?,
    val employeeVerified: Boolean,
    val employeeVerifiedTime: String?,
    val employeeSignatureData: String?,
    val verificationNotes: String?,
    val notes: String?
)

@Entity(tableName = "cached_requests")
data class VisitRequestEntity(
    @PrimaryKey val id: Int,
    val visitorName: String,
    val visitorMobile: String,
    val visitorCompany: String,
    val purpose: String,
    val idProofType: String,
    val idProofNumber: String,
    val vehicleNumber: String?,
    val hostEmployeeId: Int,
    val hostName: String,
    val gateId: Int,
    val gateName: String,
    val status: String,
    val decisionTime: String?,
    val decisionReason: String?,
    val meetingRoom: String?,
    val createdAt: String
)

@Entity(tableName = "cached_appointments")
data class AppointmentEntity(
    @PrimaryKey val id: Int,
    val visitorName: String,
    val visitorMobile: String,
    val visitorCompany: String,
    val visitorEmail: String?,
    val hostEmployeeId: Int,
    val hostName: String,
    val purpose: String,
    val expectedDateTime: String,
    val status: String,
    val otpCode: String,
    val qrToken: String,
    val otpExpiresAt: String,
    val otpUsed: Boolean,
    val createdAt: String
)

@Entity(tableName = "cached_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val type: String,
    val requestId: Int?,
    val visitId: Int?,
    val isRead: Boolean,
    val timestamp: String
)

@Entity(tableName = "cached_audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: Int,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val createdAt: String
)
