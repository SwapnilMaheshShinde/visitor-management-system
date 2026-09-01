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
import com.example.ui.screens.call.IncomingCallActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VmsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_INCOMING_CALL = "vms_incoming_visitor_call"
        const val CHANNEL_NAME_INCOMING_CALL = "🚨 Incoming Visitor Calls"

        const val CHANNEL_ID_URGENT_ALERTS = "vms_urgent_visitor_alerts"
        const val CHANNEL_NAME_URGENT_ALERTS = "Urgent Security Alerts"

        const val CHANNEL_ID_STANDARD = "vms_standard_notifications"
        const val CHANNEL_NAME_STANDARD = "VMS Notices & Updates"

        const val CALL_NOTIFICATION_ID = 1001
        const val TAG = "VmsFCM"
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
                    api.registerFcm(authToken, RegisterFcmDto(fcmToken = token, deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"))
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

        when {
            type.equals("INCOMING_VISITOR_CALL", ignoreCase = true) -> {
                handleIncomingVisitorCall(data)
            }
            type.equals("VISITOR_CALL_DISMISSED", ignoreCase = true) -> {
                handleCallDismissed(data)
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

    private fun handleIncomingVisitorCall(data: Map<String, String>) {
        val requestId = data["requestId"]?.toIntOrNull() ?: 0
        val callId = data["callId"] ?: "CALL_${requestId}_${System.currentTimeMillis()}"
        val visitorName = data["visitorName"] ?: "Guest Visitor"
        val visitorMobile = data["visitorMobile"] ?: ""
        val visitorCompany = data["visitorCompany"] ?: "Visitor"
        val purpose = data["purpose"] ?: "Official Visit"
        val gateName = data["gateName"] ?: "Main Security Gate"
        val guardName = data["guardName"] ?: "Security Officer"
        val vehicleNumber = data["vehicleNumber"] ?: ""
        val idProofType = data["idProofType"] ?: "National ID"
        val idProofNumber = data["idProofNumber"] ?: ""
        val createdAt = data["createdAt"] ?: ""

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 1. Create High-Priority Notification Channel with Ringtone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID_INCOMING_CALL,
                CHANNEL_NAME_INCOMING_CALL,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming real-time visitor phone calls for host employees"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 800, 1000, 800)
                setSound(ringtoneUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Full-Screen Intent for IncomingCallActivity
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(IncomingCallActivity.EXTRA_REQUEST_ID, requestId)
            putExtra(IncomingCallActivity.EXTRA_CALL_ID, callId)
            putExtra(IncomingCallActivity.EXTRA_NOTIFICATION_ID, CALL_NOTIFICATION_ID)
            putExtra(IncomingCallActivity.EXTRA_VISITOR_NAME, visitorName)
            putExtra(IncomingCallActivity.EXTRA_VISITOR_MOBILE, visitorMobile)
            putExtra(IncomingCallActivity.EXTRA_VISITOR_COMPANY, visitorCompany)
            putExtra(IncomingCallActivity.EXTRA_PURPOSE, purpose)
            putExtra(IncomingCallActivity.EXTRA_GATE_NAME, gateName)
            putExtra(IncomingCallActivity.EXTRA_GUARD_NAME, guardName)
            putExtra(IncomingCallActivity.EXTRA_VEHICLE_NUMBER, vehicleNumber)
            putExtra(IncomingCallActivity.EXTRA_ID_PROOF_TYPE, idProofType)
            putExtra(IncomingCallActivity.EXTRA_ID_PROOF_NUMBER, idProofNumber)
            putExtra(IncomingCallActivity.EXTRA_CREATED_AT, createdAt)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            requestId + 100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: ACCEPT Direct Action PendingIntent
        val acceptIntent = Intent(this, IncomingCallActionReceiver::class.java).apply {
            action = IncomingCallActionReceiver.ACTION_ACCEPT_CALL
            putExtra(IncomingCallActionReceiver.EXTRA_REQUEST_ID, requestId)
            putExtra(IncomingCallActionReceiver.EXTRA_NOTIFICATION_ID, CALL_NOTIFICATION_ID)
            putExtra(IncomingCallActionReceiver.EXTRA_VISITOR_NAME, visitorName)
            putExtra(IncomingCallActionReceiver.EXTRA_MEETING_ROOM, "Conference Room A")
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this,
            requestId + 200,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Action: DECLINE Direct Action PendingIntent
        val declineIntent = Intent(this, IncomingCallActionReceiver::class.java).apply {
            action = IncomingCallActionReceiver.ACTION_DECLINE_CALL
            putExtra(IncomingCallActionReceiver.EXTRA_REQUEST_ID, requestId)
            putExtra(IncomingCallActionReceiver.EXTRA_NOTIFICATION_ID, CALL_NOTIFICATION_ID)
            putExtra(IncomingCallActionReceiver.EXTRA_VISITOR_NAME, visitorName)
            putExtra(IncomingCallActionReceiver.EXTRA_REASON, "Declined by host")
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            requestId + 300,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 5. Build High-Priority Incoming Call Notification
        val notificationTitle = "🚨 INCOMING VISITOR: $visitorName"
        val notificationBody = "$visitorCompany • Purpose: $purpose at $gateName"

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_INCOMING_CALL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$visitorName ($visitorCompany) is waiting at $gateName.\nPurpose: $purpose\nSecurity Officer: $guardName")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 1000, 800, 1000, 800))
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_launcher_foreground, "✅ ACCEPT / APPROVE", acceptPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "❌ DECLINE", declinePendingIntent)

        notificationManager.notify(CALL_NOTIFICATION_ID, notificationBuilder.build())

        // 6. Direct Activity Launch (works immediately when screen is ON or unlocked)
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct activity launch deferred to full screen intent: ${e.message}")
        }
    }

    private fun handleCallDismissed(data: Map<String, String>) {
        val requestId = data["requestId"]?.toIntOrNull() ?: 0
        Log.d(TAG, "Handling call dismissal for request: $requestId")

        VisitorCallPlayer.stop()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CALL_NOTIFICATION_ID)

        val dismissBroadcast = Intent(IncomingCallActionReceiver.ACTION_CALL_DISMISSED).apply {
            putExtra(IncomingCallActionReceiver.EXTRA_REQUEST_ID, requestId)
            setPackage(packageName)
        }
        sendBroadcast(dismissBroadcast)
    }

    private fun handleRequestDecision(data: Map<String, String>) {
        val requestId = data["requestId"]
        val status = data["status"] ?: "DECIDED"
        val visitorName = data["visitorName"] ?: "Visitor"
        val isApproved = status.equals("ACCEPTED", ignoreCase = true)

        Log.d(TAG, "Request decision notification received: $status for $visitorName")

        // Sync data from server to refresh local lists
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VmsRepository.getInstance(applicationContext).syncDataFromServer()
            } catch (e: Exception) {
                Log.w(TAG, "Sync error on decision: ${e.message}")
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID_URGENT_ALERTS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME_URGENT_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (isApproved) "✅ Host Approved: $visitorName" else "❌ Host Declined: $visitorName"
        val body = if (isApproved) "Entry authorized. You may now print pass or grant gate entry." else "Host declined entry for this visitor."

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_destination", "guard_dashboard")
            putExtra("request_id", requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
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

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    private fun showStandardNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID_STANDARD

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME_STANDARD,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
