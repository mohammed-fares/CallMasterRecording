package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AppDatabase
import com.example.recording.CallMasterRecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class CallMasterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        cleanDummyDataIfPresent()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CallMasterRecordingService.CHANNEL_ID,
                "CallMaster Active Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground recording notifications"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Ensures all mock/sample files are purged and no dummy records exist.
     * The app functions on real phone calls and actual microphone audio only.
     */
    private fun cleanDummyDataIfPresent() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CallMasterApp)
            // Purge any dummy records that were previously generated with placeholder titles
            val dummyIds = listOf(
                "مكالمة عمل - المهندس أحمد",
                "WhatsApp Voice - Sarah Miller",
                "تسجيل صوتي - اجتماع استراتيجي",
                "Telegram Call - Dr. Omar"
            )
            dummyIds.forEach { dummyTitle ->
                db.recordingDao().deleteByTitle(dummyTitle)
            }

            // Clean up any sample audio files
            val recordingsDir = File(filesDir, "recordings")
            if (recordingsDir.exists()) {
                val sampleFiles = listOf("sample_ahmed.m4a", "sample_sarah.m4a", "sample_meeting.m4a", "sample_omar.m4a")
                sampleFiles.forEach { sName ->
                    val f = File(recordingsDir, sName)
                    if (f.exists()) f.delete()
                }
            }
        }
    }
}
