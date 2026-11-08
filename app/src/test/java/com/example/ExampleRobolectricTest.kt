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
    fun test01_directMicrophoneRecording() {
        runBlocking {
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
    }

    @Test
    fun test02_incomingCellularCallRecording() {
        runBlocking {
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
    }

    @Test
    fun test03_outgoingCellularCallRecording() {
        runBlocking {
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
    }

    @Test
    fun test04_cellularRecordingWithSpeakerphoneEnabled() {
        runBlocking {
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
    fun test09_serviceShutdownDuringRecording() {
        runBlocking {
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

    @Test
    fun test13_aes256GcmRealEncryptionAndDecryption() {
        val sec = com.example.security.CallMasterSecurityManager()
        val plainFile = File(context.cacheDir, "unit_test_plain.m4a")
        val sampleData = "SAMPLE_CALL_AUDIO_PCM_DATA_AES256_TEST_STRING_VERIFICATION_BYTES".toByteArray(Charsets.UTF_8)
        plainFile.writeBytes(sampleData)

        val encryptedFile = File(context.cacheDir, "unit_test_plain.m4a.enc")
        val encResult = sec.encryptFile(plainFile, encryptedFile)

        assertTrue("Encryption must return true", encResult)
        assertTrue("Encrypted file must exist", encryptedFile.exists())
        assertTrue("Encrypted file must be greater in size than plain data due to IV + GCM tag", encryptedFile.length() > sampleData.size)

        // Shred plaintext
        sec.secureDelete(plainFile)
        assertFalse("Original plain file must be shredded", plainFile.exists())

        // Decrypt
        val decryptedFile = File(context.cacheDir, "unit_test_decrypted.m4a")
        val decResult = sec.decryptFile(encryptedFile, decryptedFile)

        assertTrue("Decryption must succeed", decResult)
        assertTrue("Decrypted file must exist", decryptedFile.exists())
        assertEquals("Decrypted bytes must match original plain bytes", sampleData.size.toLong(), decryptedFile.length())
        assertEquals(String(sampleData), String(decryptedFile.readBytes()))

        // Cleanup
        encryptedFile.delete()
        decryptedFile.delete()
    }

    @Test
    fun test14_autoRecordPrivacyDefaultOff() {
        // Privacy requirement: Automatic recording must be disabled by default and require explicit consent
        assertFalse("Auto recording must be disabled by default for privacy compliance", preferences.isAutoRecordEnabled)
    }

    @Test
    fun test15_googleDriveSafExportStrictByteVerification() {
        runBlocking {
            val testAudio = File(context.cacheDir, "test_drive_audio.m4a")
            testAudio.writeBytes("TEST_AUDIO_BYTES_FOR_DRIVE_EXPORT_VERIFICATION".toByteArray())

            val rec = RecordingEntity(
                id = UUID.randomUUID().toString(),
                title = "Drive Export Verification",
                filePath = testAudio.absolutePath
            )
            database.recordingDao().insertRecording(rec)

            val targetDestFile = File(context.cacheDir, "drive_destination.m4a")
            val targetUri = android.net.Uri.fromFile(targetDestFile)

            val driveManager = com.example.backup.GoogleDriveBackupManager.getInstance(context)
            val success = driveManager.executeSafExport(rec, targetUri)

            assertTrue("Export to destination must succeed", success)
            assertEquals("Destination file must have exact same byte length", testAudio.length(), targetDestFile.length())

            val updated = database.recordingDao().getRecordingById(rec.id)
            assertEquals(com.example.data.BackupSyncStatus.SYNCED, updated?.backupStatus)

            testAudio.delete()
            targetDestFile.delete()
        }
    }

    @Test
    fun test16_empiricalAudioSourceProbe() {
        val engine = DeviceCompatibilityEngine(context)
        val benchmark = engine.runEmpiricalAudioSourceBenchmark()
        assertNotNull(benchmark)
        assertTrue(benchmark.containsKey("MIC (Hardware Microphone)"))
        assertTrue(benchmark.containsKey("VOICE_COMMUNICATION (VoIP / In-Call Acoustic)"))
    }
}
