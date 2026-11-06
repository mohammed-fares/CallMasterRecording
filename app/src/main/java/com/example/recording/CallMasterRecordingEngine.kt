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
                File(prefs.customStorageFolder)
            } else {
                val ext = context.getExternalFilesDir(android.os.Environment.DIRECTORY_RECORDINGS)
                if (ext != null) {
                    File(ext, "CallMaster")
                } else {
                    File(context.filesDir, "recordings")
                }
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

            // Two-party clarity: VOICE_COMMUNICATION enables acoustic echo cancellation and AGC
            if (prefs.twoSidedAcousticBoost) {
                try {
                    recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                } catch (_: Exception) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
            } else {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
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

        } catch (e: Exception) {
            Log.e("CallMasterEngine", "Error starting recorder", e)
            _sessionState.update {
                it.copy(
                    state = RecordingEngineState.FAILED,
                    errorMessage = "Failed to initialize audio recorder: ${e.localizedMessage ?: "Unknown hardware error"}"
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

        // Truth in recording:
        // Distinguish accurately between two-sided and mic-only
        val completeness = when (activeSource) {
            RecordingSource.CELLULAR -> CompletenessStatus.MIC_ONLY // Honest Android policy mapping
            RecordingSource.MIC -> CompletenessStatus.MIC_ONLY
            RecordingSource.WHATSAPP,
            RecordingSource.MESSENGER,
            RecordingSource.TELEGRAM,
            RecordingSource.SIGNAL,
            RecordingSource.OTHER_VOIP -> CompletenessStatus.MIC_ONLY
        }

        val defaultTitle = if (activeContactName.isNotBlank()) {
            "${activeSource.name.replace("_", " ")} - $activeContactName"
        } else {
            "${activeSource.name.replace("_", " ")} Recording"
        }

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
            encryptionStatus = "PLAINTEXT",
            validationStatus = completeness,
            consentStatus = "GRANTED_EXPLICIT"
        )

        database.recordingDao().insertRecording(entity)

        val prefs = CallMasterPreferences(context)
        if (prefs.isGoogleDriveAutoSyncEnabled) {
            scope.launch {
                GoogleDriveBackupManager.getInstance(context).syncRecording(entity)
            }
        }

        _sessionState.update {
            it.copy(
                state = RecordingEngineState.SAVED,
                errorMessage = null
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
