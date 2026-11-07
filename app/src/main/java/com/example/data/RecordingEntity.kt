package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RecordingSource {
    CELLULAR,
    WHATSAPP,
    MESSENGER,
    TELEGRAM,
    SIGNAL,
    MIC,
    OTHER_VOIP
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    NONE
}

enum class CompletenessStatus {
    SUCCESS_BOTH_SIDES,
    SUCCESS_MIC_ONLY,
    SUCCESS_PLAYBACK_ONLY,
    PARTIAL_RECORDING,
    SILENT_RECORDING,
    SOURCE_BLOCKED,
    PERMISSION_DENIED,
    UNSUPPORTED_DEVICE,
    RECORDING_INTERRUPTED;

    val isSuccess: Boolean
        get() = this == SUCCESS_BOTH_SIDES || this == SUCCESS_MIC_ONLY || this == SUCCESS_PLAYBACK_ONLY

    val isSilent: Boolean
        get() = this == SILENT_RECORDING
}

enum class BackupSyncStatus {
    LOCAL_ONLY,
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED
}

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val contactName: String = "",
    val contactNumber: String = "",
    val sourceType: RecordingSource = RecordingSource.MIC,
    val callDirection: CallDirection = CallDirection.NONE,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val filePath: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = "audio/mp4",
    val encryptionStatus: String = "PLAINTEXT",
    val validationStatus: CompletenessStatus = CompletenessStatus.SUCCESS_MIC_ONLY,
    val audioSourceUsed: String = "MIC",
    val rmsDb: Float = -100f,
    val peakAmplitude: Int = 0,
    val silenceRatio: Float = 0f,
    val validationNote: String = "",
    val backupStatus: BackupSyncStatus = BackupSyncStatus.LOCAL_ONLY,
    val consentStatus: String = "GRANTED_EXPLICIT",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val transcript: String? = null,
    val aiSummary: String? = null,
    val aiKeyPoints: String? = null,
    val aiActionItems: String? = null,
    val aiLanguage: String? = null
)
