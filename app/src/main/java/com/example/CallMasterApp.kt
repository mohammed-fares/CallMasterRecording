package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.CallDirection
import com.example.data.CompletenessStatus
import com.example.data.RecordingEntity
import com.example.data.RecordingSource
import com.example.recording.CallMasterRecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CallMasterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        seedSampleDataIfEmpty()
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

    private fun seedSampleDataIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CallMasterApp)
            val count = db.recordingDao().getTotalCount().first()
            if (count == 0) {
                val recordingsDir = File(filesDir, "recordings").apply { if (!exists()) mkdirs() }

                // Create placeholder sample audio files
                fun createSampleFile(name: String): String {
                    val file = File(recordingsDir, name)
                    if (!file.exists()) {
                        FileOutputStream(file).use { fos ->
                            // Dummy audio header bytes to represent sample audio file
                            fos.write(ByteArray(4096) { 0x41 })
                        }
                    }
                    return file.absolutePath
                }

                val now = System.currentTimeMillis()
                val samples = listOf(
                    RecordingEntity(
                        id = UUID.randomUUID().toString(),
                        title = "مكالمة عمل - المهندس أحمد",
                        contactName = "م. أحمد الشافعي",
                        contactNumber = "+20 100 123 4567",
                        sourceType = RecordingSource.CELLULAR,
                        callDirection = CallDirection.INCOMING,
                        startedAt = now - 3600_000 * 3,
                        endedAt = now - 3600_000 * 3 + 185_000,
                        durationMs = 185_000,
                        filePath = createSampleFile("sample_ahmed.m4a"),
                        fileSize = 1024 * 340,
                        mimeType = "audio/mp4",
                        validationStatus = CompletenessStatus.MIC_ONLY,
                        notes = "مكالمة هامة لمناقشة خطة تسليم المشروع وتأكيد المتطلبات.",
                        isFavorite = true,
                        transcript = "[00:01] م. أحمد: مرحباً، تأكيداً لاجتماع الغد بخصوص المشروع.\n[00:05] أنا: أهلاً بك يا باشمهندس، كل الملفات جاهزة ومراجعة تماماً.",
                        aiSummary = "مناقشة الاستعدادات النهائية لاجتماع الغد وتأكيد استلام كل الملفات والتصاميم.",
                        aiKeyPoints = "• تأكيد موعد الاجتماع غداً الساعة 10 صباحاً.\n• مراجعة مسودة العقد.\n• إرسال العرض التقديمي.",
                        aiActionItems = "• تجهيز تقرير التوافق النهائي.\n• إرسال رابط الاجتماع للحضور.",
                        aiLanguage = "ar"
                    ),
                    RecordingEntity(
                        id = UUID.randomUUID().toString(),
                        title = "WhatsApp Voice - Sarah Miller",
                        contactName = "Sarah Miller",
                        contactNumber = "+1 (415) 555-0199",
                        sourceType = RecordingSource.WHATSAPP,
                        callDirection = CallDirection.OUTGOING,
                        startedAt = now - 3600_000 * 12,
                        endedAt = now - 3600_000 * 12 + 95_000,
                        durationMs = 95_000,
                        filePath = createSampleFile("sample_sarah.m4a"),
                        fileSize = 1024 * 180,
                        mimeType = "audio/mp4",
                        validationStatus = CompletenessStatus.MIC_ONLY,
                        notes = "Sprint sync about design guidelines and Arabic typography.",
                        isFavorite = true,
                        transcript = "[00:01] Sarah: Hi there! How is the Cairo font rendering looking?\n[00:04] Me: It looks fantastic, both Arabic and English typography are aligned perfectly.",
                        aiSummary = "Design checkpoint confirming Cairo font integration and localized RTL layout validation.",
                        aiKeyPoints = "• Cairo font typography approved.\n• RTL layout verified.\n• Golden accent palette looks high-end.",
                        aiActionItems = "• Export final design deliverables.",
                        aiLanguage = "en"
                    ),
                    RecordingEntity(
                        id = UUID.randomUUID().toString(),
                        title = "تسجيل صوتي - اجتماع استراتيجي",
                        contactName = "فريق الإدارة والتقنية",
                        contactNumber = "",
                        sourceType = RecordingSource.MIC,
                        callDirection = CallDirection.NONE,
                        startedAt = now - 3600_000 * 24,
                        endedAt = now - 3600_000 * 24 + 320_000,
                        durationMs = 320_000,
                        filePath = createSampleFile("sample_meeting.m4a"),
                        fileSize = 1024 * 580,
                        mimeType = "audio/mp4",
                        validationStatus = CompletenessStatus.MIC_ONLY,
                        notes = "جلسة عصف ذهني لإطلاق ميزات الأمان والتشفير المحلي.",
                        isFavorite = false
                    ),
                    RecordingEntity(
                        id = UUID.randomUUID().toString(),
                        title = "Telegram Call - Dr. Omar",
                        contactName = "د. عمر خالد",
                        contactNumber = "+971 50 123 4567",
                        sourceType = RecordingSource.TELEGRAM,
                        callDirection = CallDirection.INCOMING,
                        startedAt = now - 3600_000 * 48,
                        endedAt = now - 3600_000 * 48 + 140_000,
                        durationMs = 140_000,
                        filePath = createSampleFile("sample_omar.m4a"),
                        fileSize = 1024 * 260,
                        mimeType = "audio/mp4",
                        validationStatus = CompletenessStatus.MIC_ONLY,
                        notes = "استشارة فنية حول خوارزميات التشفير AES-256.",
                        isFavorite = false
                    )
                )

                db.recordingDao().insertAll(samples)
            }
        }
    }
}
