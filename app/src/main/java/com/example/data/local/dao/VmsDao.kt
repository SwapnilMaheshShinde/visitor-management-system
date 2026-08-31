package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppointmentEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.VisitEntity
import com.example.data.local.entity.VisitRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VmsDao {

    // Visits
    @Query("SELECT * FROM cached_visits ORDER BY entryTime DESC")
    fun getAllVisitsFlow(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM cached_visits WHERE status = 'INSIDE' ORDER BY entryTime DESC")
    fun getInsideVisitsFlow(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM cached_visits WHERE hostEmployeeId = :hostId ORDER BY entryTime DESC")
    fun getVisitsByHostFlow(hostId: Int): Flow<List<VisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    // Visit Requests
    @Query("SELECT * FROM cached_requests ORDER BY createdAt DESC")
    fun getAllRequestsFlow(): Flow<List<VisitRequestEntity>>

    @Query("SELECT * FROM cached_requests WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingRequestsFlow(): Flow<List<VisitRequestEntity>>

    @Query("SELECT * FROM cached_requests WHERE hostEmployeeId = :hostId ORDER BY createdAt DESC")
    fun getRequestsByHostFlow(hostId: Int): Flow<List<VisitRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<VisitRequestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: VisitRequestEntity)

    // Appointments
    @Query("SELECT * FROM cached_appointments ORDER BY expectedDateTime ASC")
    fun getAllAppointmentsFlow(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM cached_appointments WHERE hostEmployeeId = :hostId ORDER BY expectedDateTime ASC")
    fun getAppointmentsByHostFlow(hostId: Int): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    // Notifications
    @Query("SELECT * FROM cached_notifications ORDER BY id DESC")
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE cached_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    // Audit Logs
    @Query("SELECT * FROM cached_audit_logs ORDER BY createdAt DESC LIMIT 100")
    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)
}
