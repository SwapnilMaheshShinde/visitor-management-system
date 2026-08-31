package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.remote.NetworkManager
import com.example.data.remote.RegisterFcmDto
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VmsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_URGENT_ALERTS = "vms_urgent_visitor_alerts"
        const val CHANNEL_NAME_URGENT_ALERTS = "Urgent Visitor Arrivals & Approvals"
        const val CHANNEL_ID_STANDARD = "vms_standard_notifications"
        const val CHANNEL_NAME_STANDARD = "VMS Updates & Notices"
        const val TAG = "VmsFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase Messaging Token received: $token")

        // Sync token with backend API if user is authenticated
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val netMgr = NetworkManager.getInstance(applicationContext)
                val authToken = netMgr.getAuthToken()
                if (authToken.isNotEmpty()) {
                    val api = netMgr.getApiService()
                    api.registerFcm(authToken, RegisterFcmDto(fcmToken = token))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register FCM token with backend: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "🚨 Visitor Arrival Alert"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "A visitor is waiting at the security checkpoint"
        val requestId = remoteMessage.data["requestId"]
        val visitId = remoteMessage.data["visitId"]
        val type = remoteMessage.data["type"] ?: "VISITOR_REQUEST"

        showSystemNotification(title, body, type, requestId, visitId)
    }

    private fun showSystemNotification(
        title: String,
        body: String,
        type: String,
        requestId: String?,
        visitId: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isUrgent = type.contains("VISITOR_REQUEST", ignoreCase = true) || type.contains("URGENT", ignoreCase = true)
        val channelId = if (isUrgent) CHANNEL_ID_URGENT_ALERTS else CHANNEL_ID_STANDARD

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (isUrgent) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                channelId,
                if (isUrgent) CHANNEL_NAME_URGENT_ALERTS else CHANNEL_NAME_STANDARD,
                importance
            ).apply {
                description = "Real-time alerts for visitor arrivals, approvals, and security checkpoints"
                enableVibration(true)
                vibrationPattern = if (isUrgent) longArrayOf(0, 500, 200, 500, 200, 500) else longArrayOf(0, 250, 250, 250)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("request_id", requestId)
            putExtra("visit_id", visitId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isUrgent) {
            notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
        }

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notificationBuilder.build())
    }
}
