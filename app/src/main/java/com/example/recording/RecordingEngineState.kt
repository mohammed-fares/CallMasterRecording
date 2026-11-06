package com.example.recording

enum class RecordingEngineState {
    IDLE,
    DETECTED,
    CHECKING_COMPATIBILITY,
    WAITING_FOR_CONSENT,
    RECORDING,
    PAUSED,
    FINALIZING,
    VALIDATING,
    SAVED,
    FAILED,
    UNSUPPORTED
}

data class RecordingSessionInfo(
    val state: RecordingEngineState = RecordingEngineState.IDLE,
    val durationSeconds: Long = 0L,
    val currentAmplitude: Int = 0,
    val source: String = "MIC",
    val contactName: String = "",
    val contactNumber: String = "",
    val errorMessage: String? = null,
    val activeFilePath: String? = null
)
