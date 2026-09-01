package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.remote.NetworkManager
import com.example.data.remote.RegisterFcmDto
import com.example.data.repository.VmsRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class VmsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_VISITOR_ARRIVALS = "vms_visitor_arrivals"
        const val CHANNEL_NAME_VISITOR_ARRIVALS = "Visitor Arrivals"

        const val CHANNEL_ID_URGENT_ALERTS = "vms_urgent_visitor_alerts"
        const val CHANNEL_NAME_URGENT_ALERTS = "Urgent Security Alerts"

        const val CHANNEL_ID_STANDARD = "vms_standard_notifications"
        const val CHANNEL_NAME_STANDARD = "VMS Notices & Updates"

        private const val TAG = "VmsFCM"

        // Deduplication cache to prevent firing duplicate notifications if duplicate FCM frames arrive
        private val recentNotificationTimestamps = ConcurrentHashMap<String, Long>()
        private const val DEDUP_WINDOW_MS = 10_000L // 10 seconds
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase Messaging Token received: $token")

        // Sync token with backend API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val netMgr = NetworkManager.getInstance(applicationContext)
                val authToken = netMgr.getAuthToken()
                if (authToken.isNotEmpty()) {
                    val api = netMgr.getApiService()
                    api.registerFcm(
                        authToken,
                        RegisterFcmDto(
                            fcmToken = token,
                            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register FCM token with backend: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received. Data payload: ${remoteMessage.data}")

        val data = remoteMessage.data
        val type = data["type"] ?: "STANDARD"

        // Sync local database in background for fresh real-time state
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VmsRepository.getInstance(applicationContext).syncDataFromServer()
            } catch (e: Exception) {
                Log.w(TAG, "Background sync on FCM arrival failed: ${e.message}")
            }
        }

        when {
            type.equals("VISITOR_ARRIVAL", ignoreCase = true) ||
            type.equals("INCOMING_VISITOR_CALL", ignoreCase = true) ||
            type.equals("VISITOR_REQUEST", ignoreCase = true) -> {
                handleVisitorArrivalNotification(data)
            }
            type.equals("REQUEST_DECISION", ignoreCase = true) -> {
                handleRequestDecision(data)
            }
            else -> {
                val title = remoteMessage.notification?.title ?: data["title"] ?: "VMS Security Notice"
                val body = remoteMessage.notification?.body ?: data["body"] ?: "New update from Visitor Management System"
                showStandardNotification(title, body, data)
            }
        }
    }

    /**
     * Handles professional Visitor Arrival Push Notification
     * Displays a clean, high-priority notification with visitor details and opens the Employee Dashboard on tap.
     */
    private fun handleVisitorArrivalNotification(data: Map<String, String>) {
        val requestId = data["requestId"]?.toIntOrNull() ?: (System.currentTimeMillis() % 100000).toInt()
        val visitorName = data["visitorName"]?.takeIf { it.isNotBlank() } ?: "Guest Visitor"
        val visitorCompany = data["visitorCompany"]?.takeIf { it.isNotBlank() } ?: "Guest"
        val purpose = data["purpose"]?.takeIf { it.isNotBlank() } ?: "Official Visit"
        val gateName = data["gateName"]?.takeIf { it.isNotBlank() } ?: "Main Security Gate"
        val guardName = data["guardName"]?.takeIf { it.isNotBlank() } ?: "Security Officer"

        // Deduplication check
        val dedupKey = "arrival_${requestId}_$visitorName"
        val now = System.currentTimeMillis()
        val lastSeen = recentNotificationTimestamps[dedupKey] ?: 0L
        if (now - lastSeen < DEDUP_WINDOW_MS) {
            Log.d(TAG, "Skipping duplicate notification for key: $dedupKey")
            return
        }
        recentNotificationTimestamps[dedupKey] = now

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Dedicated High-Importance "Visitor Arrivals" Channel (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST)
                .build()

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID_VISITOR_ARRIVALS,
                CHANNEL_NAME_VISITOR_ARRIVALS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications when visitors arrive at security gates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tapping the notification opens the Employee Dashboard directly
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_destination", "employee_dashboard")
            putExtra("request_id", requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Visitor Arrival Request"
        val summaryText = "$visitorName • $visitorCompany arrived at $gateName"

        val bigTextContent = buildString {
            append("Visitor: ").append(visitorName).append("\n")
            append("Company: ").append(visitorCompany).append("\n")
            append("Purpose: ").append(purpose).append("\n")
            append("Gate: ").append(gateName).append("\n")
            append("Guard: ").append(guardName)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_VISITOR_ARRIVALS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 150, 300))

        notificationManager.notify(requestId, notificationBuilder.build())
    }

    /**
     * Handles decision notifications dispatched when a Host approves or declines a visitor
     */
    private fun handleRequestDecision(data: Map<String, String>) {
        val requestId = data["requestId"]?.toIntOrNull() ?: (System.currentTimeMillis() % 100000).toInt()
        val status = data["status"] ?: "DECIDED"
        val visitorName = data["visitorName"] ?: "Visitor"
        val isApproved = status.equals("ACCEPTED", ignoreCase = true)

        Log.d(TAG, "Request decision notification received: $status for $visitorName")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID_URGENT_ALERTS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME_URGENT_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                description = "Security decisions and host verification responses"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (isApproved) "Host Approved: $visitorName" else "Host Declined: $visitorName"
        val body = if (isApproved) "Entry authorized. You may now print pass or grant gate entry." else "Host declined entry for this visitor."

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_destination", "guard_dashboard")
            putExtra("request_id", requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId + 500,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(requestId + 500, notification)
    }

    /**
     * Standard notification fallback for general updates
     */
    private fun showStandardNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID_STANDARD

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME_STANDARD,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications and appointment reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
