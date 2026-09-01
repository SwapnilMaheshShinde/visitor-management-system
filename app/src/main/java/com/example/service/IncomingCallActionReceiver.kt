package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.repository.VmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ACCEPT_CALL = "com.example.vms.ACTION_ACCEPT_CALL"
        const val ACTION_DECLINE_CALL = "com.example.vms.ACTION_DECLINE_CALL"
        const val ACTION_CALL_DISMISSED = "com.example.vms.ACTION_CALL_DISMISSED"

        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_VISITOR_NAME = "visitor_name"
        const val EXTRA_REASON = "reason"
        const val EXTRA_MEETING_ROOM = "meeting_room"

        private const val TAG = "IncomingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val requestId = intent.getIntExtra(EXTRA_REQUEST_ID, 0)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val visitorName = intent.getStringExtra(EXTRA_VISITOR_NAME) ?: "Visitor"
        val reason = intent.getStringExtra(EXTRA_REASON)
        val meetingRoom = intent.getStringExtra(EXTRA_MEETING_ROOM) ?: "Conference Room A"

        Log.d(TAG, "onReceive: action=$action, requestId=$requestId, notificationId=$notificationId")

        // 1. Stop audio & vibration immediately
        VisitorCallPlayer.stop()

        // 2. Dismiss incoming call notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationId != 0) {
            notificationManager.cancel(notificationId)
        }

        // 3. Notify any running IncomingCallActivity to close
        val dismissIntent = Intent(ACTION_CALL_DISMISSED).apply {
            putExtra(EXTRA_REQUEST_ID, requestId)
            setPackage(context.packageName)
        }
        context.sendBroadcast(dismissIntent)

        // 4. Submit decision to server
        val isAccept = action == ACTION_ACCEPT_CALL
        if (requestId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = VmsRepository.getInstance(context.applicationContext)
                    val result = repo.submitRequestDecision(
                        requestId = requestId,
                        accept = isAccept,
                        reason = if (isAccept) null else (reason ?: "Host declined via quick action"),
                        meetingRoom = if (isAccept) meetingRoom else null
                    )
                    Log.d(TAG, "Decision submitted for request $requestId: isAccept=$isAccept, success=${result.isSuccess}")

                    // Post confirmation notification
                    showConfirmationNotification(
                        context,
                        visitorName = visitorName,
                        isApproved = isAccept,
                        details = if (isAccept) "Entry approved. Room: $meetingRoom" else "Visitor entry declined."
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to submit decision: ${e.message}", e)
                }
            }
        }
    }

    private fun showConfirmationNotification(
        context: Context,
        visitorName: String,
        isApproved: Boolean,
        details: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vms_standard_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VMS Updates & Notices",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (isApproved) "✅ Approved: $visitorName" else "❌ Declined: $visitorName"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(details)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
