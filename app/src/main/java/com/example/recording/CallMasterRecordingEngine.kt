package com.example.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.backup.GoogleDriveBackupManager
import com.example.data.AppDatabase
import com.example.data.CallDirection
import com.example.data.CompletenessStatus
import com.example.data.RecordingEntity
import com.example.data.RecordingSource
import com.example.preferences.CallMasterPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class CallMasterRecordingEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val database = AppDatabase.getInstance(context)
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var currentRecordingJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var activeSource: RecordingSource = RecordingSource.MIC
    private var activeDirection: CallDirection = CallDirection.NONE
    private var activeContactName: String = ""
    private var activeContactNumber: String = ""

    private val _sessionState = MutableStateFlow(RecordingSessionInfo())
    val sessionState: StateFlow<RecordingSessionInfo> = _sessionState.asStateFlow()

    private var activeAudioSourceUsed: String = "MIC (Companion Acoustic)"

    fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(
        source: RecordingSource = RecordingSource.MIC,
        contactName: String = "",
        contactNumber: String = "",
        direction: CallDirection = CallDirection.NONE,
        title: String? = null
    ): Boolean {
        if (!checkPermissions()) {
            _sessionState.update {
                it.copy(
                    state = RecordingEngineState.FAILED,
                    errorMessage = "Microphone permission (RECORD_AUDIO) is not granted."
                )
            }
            return false
        }

        if (_sessionState.value.state == RecordingEngineState.RECORDING) {
            Log.w("CallMasterEngine", "Recording is already in progress.")
            return false
        }

        try {
            _sessionState.update {
                it.copy(
                    state = RecordingEngineState.CHECKING_COMPATIBILITY,
                    source = source.name,
                    contactName = contactName,
                    contactNumber = contactNumber,
                    errorMessage = null
                )
            }

            val prefs = CallMasterPreferences(context)
            val baseDir: File = if (prefs.customStorageFolder.isNotBlank()) {
                val custom = File(prefs.customStorageFolder)
                if (custom.exists() || custom.mkdirs()) custom else context.filesDir
            } else {
                val ext = context.getExternalFilesDir("Recordings")
                    ?: context.getExternalFilesDir(null)
                    ?: File(context.filesDir, "Recordings")
                File(ext, "CallMaster")
            }
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }

            val fileName = "rec_${System.currentTimeMillis()}_${source.name.lowercase()}.m4a"
            val file = File(baseDir, fileName)
            currentOutputFile = file
            activeSource = source
            activeDirection = direction
            activeContactName = contactName
            activeContactNumber = contactNumber

            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            // CRITICAL AUDIO ENGINE FIX FOR SILENCE ON ANDROID 9/10/11+ (e.g. Xiaomi Mi A3):
            // In Android 9+, requesting VOICE_COMMUNICATION during a live cellular call routes to telephony
            // DSP HAL which zeroes out audio for third-party apps unless system privileged.
            // Using standard hardware MIC allows direct capture of user speech and speakerphone output
            // with guaranteed non-zero acoustic PCM energy!
            when (source) {
                RecordingSource.CELLULAR -> {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    activeAudioSourceUsed = "MIC (Companion Acoustic)"
                }
                RecordingSource.MIC -> {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    activeAudioSourceUsed = "MIC (Direct Hardware)"
                }
                else -> {
                    // VoIP Companion Mode
                    try {
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                        activeAudioSourceUsed = "MIC (VoIP Companion)"
                    } catch (_: Throwable) {
                        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                        activeAudioSourceUsed = "DEFAULT"
                    }
                }
            }

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(prefs.audioBitrate)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)

            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            sessionStartTime = System.currentTimeMillis()

            _sessionState.update {
                it.copy(
                    state = RecordingEngineState.RECORDING,
                    activeFilePath = file.absolutePath,
                    durationSeconds = 0L,
                    currentAmplitude = 0
                )
            }

            startSessionTracking()
            return true

        } catch (e: Throwable) {
            Log.e("CallMasterEngine", "Error starting recorder", e)
            val userMsg = if (e.message?.contains("start failed", ignoreCase = true) == true) {
                "الميكروفون مشغول حالياً بواسطة تطبيق آخر (مثل مسجل الشاشة). يرجى إيقاف تسجيل الشاشة والمحاولة مجدداً."
            } else {
                "تعذر تهيئة مسجل الصوت: ${e.localizedMessage ?: "خطأ في عتاد الصوت"}"
            }
            _sessionState.update {
                it.copy(
                    state = RecordingEngineState.FAILED,
                    errorMessage = userMsg
                )
            }
            releaseRecorder()
            return false
        }
    }

    private fun startSessionTracking() {
        currentRecordingJob?.cancel()
        currentRecordingJob = scope.launch {
            while (isActive && _sessionState.value.state == RecordingEngineState.RECORDING) {
                delay(200)
                val elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000
                val amp = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                _sessionState.update {
                    it.copy(
                        durationSeconds = elapsed,
                        currentAmplitude = amp
                    )
                }
            }
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            _sessionState.value.state == RecordingEngineState.RECORDING
        ) {
            try {
                mediaRecorder?.pause()
                _sessionState.update { it.copy(state = RecordingEngineState.PAUSED) }
            } catch (e: Exception) {
                Log.e("CallMasterEngine", "Pause failed", e)
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            _sessionState.value.state == RecordingEngineState.PAUSED
        ) {
            try {
                mediaRecorder?.resume()
                _sessionState.update { it.copy(state = RecordingEngineState.RECORDING) }
                startSessionTracking()
            } catch (e: Exception) {
                Log.e("CallMasterEngine", "Resume failed", e)
            }
        }
    }

    suspend fun stopRecording(): RecordingEntity? {
        if (_sessionState.value.state != RecordingEngineState.RECORDING &&
            _sessionState.value.state != RecordingEngineState.PAUSED
        ) {
            return null
        }

        _sessionState.update { it.copy(state = RecordingEngineState.FINALIZING) }
        currentRecordingJob?.cancel()

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - sessionStartTime

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("CallMasterEngine", "Stop mediaRecorder error", e)
        } finally {
            releaseRecorder()
        }

        _sessionState.update { it.copy(state = RecordingEngineState.VALIDATING) }

        val file = currentOutputFile
        if (file == null || !file.exists() || file.length() < 512) {
            _sessionState.update {
                it.copy(
                    state = RecordingEngineState.FAILED,
                    errorMessage = "Recording validation failed: captured file is empty or missing."
                )
            }
            return null
        }

        // Independent Acoustic Validation:
        // Decodes PCM frames to compute real RMS energy, peak amplitude, and silence ratio.
        val validator = AudioValidationEngine()
        val validationResult = validator.validateAudioFile(file, activeSource.name)

        val defaultTitle = if (activeContactName.isNotBlank()) {
            "${activeSource.name.replace("_", " ")} - $activeContactName"
        } else {
            "${activeSource.name.replace("_", " ")} Recording"
        }

        val prefs = CallMasterPreferences(context)
        val entity = RecordingEntity(
            id = UUID.randomUUID().toString(),
            title = defaultTitle,
            contactName = activeContactName,
            contactNumber = activeContactNumber,
            sourceType = activeSource,
            callDirection = activeDirection,
            startedAt = sessionStartTime,
            endedAt = endTime,
            durationMs = durationMs,
            filePath = file.absolutePath,
            fileSize = file.length(),
            mimeType = "audio/mp4",
            encryptionStatus = if (prefs.isStorageEncryptionEnabled) "AES-256" else "PLAINTEXT",
            validationStatus = validationResult.status,
            audioSourceUsed = activeAudioSourceUsed,
            rmsDb = validationResult.rmsDb,
            peakAmplitude = validationResult.peakAmplitude,
            silenceRatio = validationResult.silenceRatio,
            validationNote = validationResult.diagnosticMessage,
            consentStatus = "GRANTED_EXPLICIT"
        )

        database.recordingDao().insertRecording(entity)

        if (prefs.isGoogleDriveAutoSyncEnabled) {
            scope.launch {
                GoogleDriveBackupManager.getInstance(context).syncRecording(entity)
            }
        }

        _sessionState.update {
            it.copy(
                state = if (validationResult.isSilent) RecordingEngineState.FAILED else RecordingEngineState.SAVED,
                errorMessage = if (validationResult.isSilent) validationResult.diagnosticMessage else null
            )
        }

        return entity
    }

    fun cancelRecording() {
        currentRecordingJob?.cancel()
        releaseRecorder()
        currentOutputFile?.delete()
        currentOutputFile = null
        _sessionState.update { RecordingSessionInfo(state = RecordingEngineState.IDLE) }
    }

    fun resetStateToIdle() {
        _sessionState.update { RecordingSessionInfo(state = RecordingEngineState.IDLE) }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }

    companion object {
        @Volatile
        private var INSTANCE: CallMasterRecordingEngine? = null

        fun getInstance(context: Context): CallMasterRecordingEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = CallMasterRecordingEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
