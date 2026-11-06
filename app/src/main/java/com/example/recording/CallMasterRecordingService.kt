package com.example.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CallDirection
import com.example.data.RecordingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CallMasterRecordingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var recordingEngine: CallMasterRecordingEngine

    override fun onCreate() {
        super.onCreate()
        recordingEngine = CallMasterRecordingEngine.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                val sourceName = intent?.getStringExtra(EXTRA_SOURCE) ?: RecordingSource.MIC.name
                val contactName = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: ""
                val contactNumber = intent?.getStringExtra(EXTRA_CONTACT_NUMBER) ?: ""
                val directionName = intent?.getStringExtra(EXTRA_DIRECTION) ?: CallDirection.NONE.name
                val source = runCatching { RecordingSource.valueOf(sourceName) }.getOrDefault(RecordingSource.MIC)
                val direction = runCatching { CallDirection.valueOf(directionName) }.getOrDefault(CallDirection.NONE)

                val dirLabel = when (direction) {
                    CallDirection.INCOMING -> "Incoming Call"
                    CallDirection.OUTGOING -> "Outgoing Call"
                    CallDirection.NONE -> sourceName
                }

                val notification = buildNotification("Recording $dirLabel (${if (contactName.isNotBlank()) contactName else "Active"})")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        } else {
                            0
                        }
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                recordingEngine.startRecording(
                    source = source,
                    contactName = contactName,
                    contactNumber = contactNumber,
                    direction = direction
                )
            }
            ACTION_STOP -> {
                serviceScope.launch {
                    recordingEngine.stopRecording()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, CallMasterRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CallMaster Call Recorder")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.callmaster_icon_1793923695483)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Save Recording", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CallMaster Active Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notifications during active call recording"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "callmaster_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_RECORDING"
        const val ACTION_STOP = "com.example.action.STOP_RECORDING"

        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_CONTACT_NUMBER = "extra_contact_number"
        const val EXTRA_DIRECTION = "extra_direction"

        fun start(
            context: Context,
            source: RecordingSource,
            contactName: String = "",
            contactNumber: String = "",
            direction: CallDirection = CallDirection.NONE
        ) {
            val intent = Intent(context, CallMasterRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SOURCE, source.name)
                putExtra(EXTRA_CONTACT_NAME, contactName)
                putExtra(EXTRA_CONTACT_NUMBER, contactNumber)
                putExtra(EXTRA_DIRECTION, direction.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallMasterRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
