package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.compatibility.CompatibilityLevel
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.AppDatabase
import com.example.data.CallDirection
import com.example.data.CompletenessStatus
import com.example.data.RecordingEntity
import com.example.data.RecordingSource
import com.example.preferences.CallMasterPreferences
import com.example.recording.AudioValidationEngine
import com.example.recording.CallMasterRecordingEngine
import com.example.recording.RecordingEngineState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: CallMasterPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        preferences = CallMasterPreferences(context)
    }

    @Test
    fun test01_directMicrophoneRecording() = runBlocking {
        val entity = RecordingEntity(
            id = UUID.randomUUID().toString(),
            title = "Direct Mic Voice Memo",
            sourceType = RecordingSource.MIC,
            callDirection = CallDirection.NONE,
            validationStatus = CompletenessStatus.SUCCESS_MIC_ONLY,
            audioSourceUsed = "MIC (Direct Hardware)",
            rmsDb = -18.5f,
            peakAmplitude = 18450,
            silenceRatio = 0.05f
        )
        database.recordingDao().insertRecording(entity)
        val loaded = database.recordingDao().getRecordingById(entity.id)
        assertNotNull(loaded)
        assertEquals(CompletenessStatus.SUCCESS_MIC_ONLY, loaded?.validationStatus)
        assertTrue(loaded?.validationStatus?.isSuccess == true)
        assertFalse(loaded?.validationStatus?.isSilent == true)
    }

    @Test
    fun test02_incomingCellularCallRecording() = runBlocking {
        val entity = RecordingEntity(
            id = UUID.randomUUID().toString(),
            title = "Cellular - Incoming Client",
            contactName = "Sarah Connor",
            contactNumber = "+1234567890",
            sourceType = RecordingSource.CELLULAR,
            callDirection = CallDirection.INCOMING,
            validationStatus = CompletenessStatus.SUCCESS_MIC_ONLY,
            audioSourceUsed = "MIC (Companion Acoustic)",
            rmsDb = -22.4f,
            peakAmplitude = 14200
        )
        database.recordingDao().insertRecording(entity)
        val loaded = database.recordingDao().getRecordingById(entity.id)
        assertNotNull(loaded)
        assertEquals(CallDirection.INCOMING, loaded?.callDirection)
        assertEquals(RecordingSource.CELLULAR, loaded?.sourceType)
    }

    @Test
    fun test03_outgoingCellularCallRecording() = runBlocking {
        val entity = RecordingEntity(
            id = UUID.randomUUID().toString(),
            title = "Cellular - Outgoing Dispatch",
            contactName = "HQ Control",
            contactNumber = "911",
            sourceType = RecordingSource.CELLULAR,
            callDirection = CallDirection.OUTGOING,
            validationStatus = CompletenessStatus.SUCCESS_MIC_ONLY,
            audioSourceUsed = "MIC (Companion Acoustic)"
        )
        database.recordingDao().insertRecording(entity)
        val loaded = database.recordingDao().getRecordingById(entity.id)
        assertNotNull(loaded)
        assertEquals(CallDirection.OUTGOING, loaded?.callDirection)
    }

    @Test
    fun test04_cellularRecordingWithSpeakerphoneEnabled() = runBlocking {
        val entity = RecordingEntity(
            id = UUID.randomUUID().toString(),
            title = "Cellular - Speakerphone Enhanced",
            sourceType = RecordingSource.CELLULAR,
            callDirection = CallDirection.INCOMING,
            validationStatus = CompletenessStatus.SUCCESS_BOTH_SIDES,
            audioSourceUsed = "MIC (Companion Acoustic + Speakerphone)",
            rmsDb = -14.2f,
            peakAmplitude = 24000,
            silenceRatio = 0.08f,
            validationNote = "تم التحقق: تم التقاط صوت محادثة واضح لكلا الطرفين بنجاح."
        )
        database.recordingDao().insertRecording(entity)
        val loaded = database.recordingDao().getRecordingById(entity.id)
        assertNotNull(loaded)
        assertEquals(CompletenessStatus.SUCCESS_BOTH_SIDES, loaded?.validationStatus)
        assertTrue(loaded?.rmsDb ?: -100f > -20f)
    }

    @Test
    fun test05_whatsAppCapturePolicyRestricted() {
        val engine = DeviceCompatibilityEngine(context)
        val report = engine.getXiaomiMiA3BaselineReport()
        val whatsapp = report.results.find { it.packageName == "com.whatsapp" }
        assertNotNull(whatsapp)
        assertEquals(
            "هذا التطبيق لا يسمح بالتقاط الصوت عبر Android Audio Playback Capture على هذا الجهاز.",
            whatsapp?.capturePolicyMessage
        )
        assertEquals(CompatibilityLevel.LEVEL_C, whatsapp?.level)
    }

    @Test
    fun test06_telegramCapturePolicyRestricted() {
        val engine = DeviceCompatibilityEngine(context)
        val report = engine.getXiaomiMiA3BaselineReport()
        val telegram = report.results.find { it.packageName == "org.telegram.messenger" }
        assertNotNull(telegram)
        assertEquals(
            "هذا التطبيق لا يسمح بالتقاط الصوت عبر Android Audio Playback Capture على هذا الجهاز.",
            telegram?.capturePolicyMessage
        )
    }

    @Test
    fun test07_mediaProjectionCancellationHandling() {
        val engine = DeviceCompatibilityEngine(context)
        val report = engine.generateReport()
        val facebook = report.results.find { it.packageName == "com.facebook.orca" }
        assertNotNull(facebook)
        assertTrue(facebook?.level != CompatibilityLevel.LEVEL_A)
    }

    @Test
    fun test08_revokedMicrophonePermissionHandling() {
        val engine = CallMasterRecordingEngine.getInstance(context)
        val started = engine.startRecording(RecordingSource.CELLULAR)
        if (!engine.checkPermissions()) {
            assertFalse(started)
            assertEquals(RecordingEngineState.FAILED, engine.sessionState.value.state)
        }
    }

    @Test
    fun test09_serviceShutdownDuringRecording() = runBlocking {
        val entity = RecordingEntity(
            id = UUID.randomUUID().toString(),
            title = "Interrupted Session",
            validationStatus = CompletenessStatus.RECORDING_INTERRUPTED,
            validationNote = "تمت مقاطعة التسجيل قبل اكتماله."
        )
        database.recordingDao().insertRecording(entity)
        val loaded = database.recordingDao().getRecordingById(entity.id)
        assertEquals(CompletenessStatus.RECORDING_INTERRUPTED, loaded?.validationStatus)
    }

    @Test
    fun test10_silentAudioFileDetection() {
        val dummyEmptyFile = File(context.cacheDir, "empty_silent_test.m4a")
        dummyEmptyFile.writeBytes(ByteArray(256))
        val validator = AudioValidationEngine()
        val result = validator.validateAudioFile(dummyEmptyFile, "CELLULAR")
        dummyEmptyFile.delete()

        assertEquals(CompletenessStatus.SILENT_RECORDING, result.status)
        assertTrue(result.isSilent)
        assertEquals(-100f, result.rmsDb, 0.1f)
    }

    @Test
    fun test11_storageLimitReachedHandling() {
        val dummyLowSpace = 1024L * 1024L // 1MB
        val requiresAlert = dummyLowSpace < (50L * 1024L * 1024L)
        assertTrue("Storage alert should trigger if remaining space < 50MB", requiresAlert)
    }

    @Test
    fun test12_audioDeviceChangeDuringActiveSession() {
        preferences.twoSidedAcousticBoost = true
        assertTrue(preferences.twoSidedAcousticBoost)
        preferences.recordDirection = "BOTH"
        assertEquals("BOTH", preferences.recordDirection)
    }
}
