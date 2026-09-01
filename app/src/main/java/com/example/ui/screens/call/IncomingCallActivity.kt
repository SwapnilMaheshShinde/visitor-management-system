package com.example.ui.screens.call

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.repository.VmsRepository
import com.example.service.IncomingCallActionReceiver
import com.example.service.VisitorCallPlayer
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncomingCallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_VISITOR_NAME = "extra_visitor_name"
        const val EXTRA_VISITOR_MOBILE = "extra_visitor_mobile"
        const val EXTRA_VISITOR_COMPANY = "extra_visitor_company"
        const val EXTRA_PURPOSE = "extra_purpose"
        const val EXTRA_GATE_NAME = "extra_gate_name"
        const val EXTRA_GUARD_NAME = "extra_guard_name"
        const val EXTRA_VEHICLE_NUMBER = "extra_vehicle_number"
        const val EXTRA_ID_PROOF_TYPE = "extra_id_proof_type"
        const val EXTRA_ID_PROOF_NUMBER = "extra_id_proof_number"
        const val EXTRA_CREATED_AT = "extra_created_at"

        private const val TAG = "IncomingCallActivity"
    }

    private var requestId: Int = 0
    private var notificationId: Int = 0
    private var isCallFinished = false

    private val callDismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == IncomingCallActionReceiver.ACTION_CALL_DISMISSED) {
                val dismissReqId = intent.getIntExtra(IncomingCallActionReceiver.EXTRA_REQUEST_ID, 0)
                if (dismissReqId == 0 || dismissReqId == requestId) {
                    Log.d(TAG, "Received call dismissed broadcast. Closing activity.")
                    finishCallAndExit()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "IncomingCallActivity onCreate")

        // 1. Turn on screen and show over keyguard/lockscreen
        setupLockscreenFlags()

        // 2. Parse Intent Extras
        requestId = intent.getIntExtra(EXTRA_REQUEST_ID, 0)
        notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val visitorName = intent.getStringExtra(EXTRA_VISITOR_NAME) ?: "Guest Visitor"
        val visitorMobile = intent.getStringExtra(EXTRA_VISITOR_MOBILE) ?: ""
        val visitorCompany = intent.getStringExtra(EXTRA_VISITOR_COMPANY) ?: "Visitor"
        val purpose = intent.getStringExtra(EXTRA_PURPOSE) ?: "Official Visit"
        val gateName = intent.getStringExtra(EXTRA_GATE_NAME) ?: "Main Gate"
        val guardName = intent.getStringExtra(EXTRA_GUARD_NAME) ?: "Security Officer"
        val vehicleNumber = intent.getStringExtra(EXTRA_VEHICLE_NUMBER) ?: ""
        val idProofType = intent.getStringExtra(EXTRA_ID_PROOF_TYPE) ?: "National ID"
        val idProofNumber = intent.getStringExtra(EXTRA_ID_PROOF_NUMBER) ?: ""
        val createdAt = intent.getStringExtra(EXTRA_CREATED_AT) ?: ""

        // 3. Register dismiss receiver
        val filter = IntentFilter(IncomingCallActionReceiver.ACTION_CALL_DISMISSED)
        ContextCompat.registerReceiver(
            this,
            callDismissReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // 4. Start Ringtone and Vibration
        VisitorCallPlayer.start(this)

        setContent {
            MyApplicationTheme {
                var isSubmitting by remember { mutableStateOf(false) }

                IncomingVisitorCallScreen(
                    requestId = requestId,
                    visitorName = visitorName,
                    visitorMobile = visitorMobile,
                    visitorCompany = visitorCompany,
                    purpose = purpose,
                    gateName = gateName,
                    guardName = guardName,
                    vehicleNumber = vehicleNumber,
                    idProofType = idProofType,
                    idProofNumber = idProofNumber,
                    createdAt = createdAt,
                    isSubmitting = isSubmitting,
                    onAccept = { meetingRoom ->
                        if (!isSubmitting) {
                            isSubmitting = true
                            handleAccept(meetingRoom, visitorName)
                        }
                    },
                    onDecline = { reason ->
                        if (!isSubmitting) {
                            isSubmitting = true
                            handleDecline(reason, visitorName)
                        }
                    },
                    onTimeout = {
                        handleTimeout(visitorName)
                    }
                )
            }
        }
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun handleAccept(meetingRoom: String, visitorName: String) {
        VisitorCallPlayer.stop()
        dismissNotification()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = VmsRepository.getInstance(applicationContext)
                val result = repo.submitRequestDecision(
                    requestId = requestId,
                    accept = true,
                    reason = null,
                    meetingRoom = meetingRoom
                )

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(
                            this@IncomingCallActivity,
                            "✅ Approved entry for $visitorName (Room: $meetingRoom)",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@IncomingCallActivity,
                            "Decision submitted: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    finishCallAndExit(launchMain = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting visitor: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    finishCallAndExit(launchMain = true)
                }
            }
        }
    }

    private fun handleDecline(reason: String, visitorName: String) {
        VisitorCallPlayer.stop()
        dismissNotification()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = VmsRepository.getInstance(applicationContext)
                repo.submitRequestDecision(
                    requestId = requestId,
                    accept = false,
                    reason = reason,
                    meetingRoom = null
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@IncomingCallActivity,
                        "❌ Declined entry for $visitorName",
                        Toast.LENGTH_SHORT
                    ).show()
                    finishCallAndExit(launchMain = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error declining visitor: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    finishCallAndExit(launchMain = false)
                }
            }
        }
    }

    private fun handleTimeout(visitorName: String) {
        VisitorCallPlayer.stop()
        dismissNotification()
        Toast.makeText(this, "Missed visitor alert: $visitorName", Toast.LENGTH_SHORT).show()
        finishCallAndExit(launchMain = false)
    }

    private fun dismissNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationId != 0) {
                notificationManager.cancel(notificationId)
            }
            notificationManager.cancel(1001) // common call notification ID
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing notification: ${e.message}")
        }
    }

    private fun finishCallAndExit(launchMain: Boolean = false) {
        if (isCallFinished) return
        isCallFinished = true
        VisitorCallPlayer.stop()

        if (launchMain) {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("nav_destination", "employee_dashboard")
            }
            startActivity(mainIntent)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        VisitorCallPlayer.stop()
        try {
            unregisterReceiver(callDismissReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}
