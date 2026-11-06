package com.example.compatibility

data class VoipCompatibilityResult(
    val providerId: String,
    val isSupported: Boolean,
    val level: CompatibilityLevel,
    val explanation: String,
    val authorizedAlternatives: List<String>
)

data class VoipConsentResult(
    val hasConsent: Boolean,
    val consentTimestamp: Long = System.currentTimeMillis(),
    val noticePresented: String
)

data class VoipRecordingResult(
    val isSuccess: Boolean,
    val audioFilePath: String?,
    val bytesCaptured: Long,
    val errorMessage: String? = null
)

interface VoipRecordingProvider {
    val providerId: String
    val displayName: String
    val packageName: String

    suspend fun checkCompatibility(): VoipCompatibilityResult
    suspend fun checkConsent(): VoipConsentResult
    suspend fun startRecording(): VoipRecordingResult
    suspend fun stopRecording(): VoipRecordingResult
}

class BaseVoipRecordingProvider(
    override val providerId: String,
    override val displayName: String,
    override val packageName: String,
    private val isInstalled: Boolean,
    private val isAudioPlaybackCaptureAvailable: Boolean
) : VoipRecordingProvider {

    override suspend fun checkCompatibility(): VoipCompatibilityResult {
        return if (!isInstalled) {
            VoipCompatibilityResult(
                providerId = providerId,
                isSupported = false,
                level = CompatibilityLevel.LEVEL_D,
                explanation = "Direct recording is not supported on this device or application. $displayName is not installed.",
                authorizedAlternatives = listOf(
                    "Install official $displayName application from Google Play.",
                    "Use CallMaster acoustic microphone recording with speakerphone."
                )
            )
        } else if (isAudioPlaybackCaptureAvailable) {
            VoipCompatibilityResult(
                providerId = providerId,
                isSupported = true,
                level = CompatibilityLevel.LEVEL_B,
                explanation = "Direct recording is not supported on this device or application. Available authorized alternatives are shown below.",
                authorizedAlternatives = listOf(
                    "System audio projection (AudioPlaybackCapture API) with explicit user grant.",
                    "Acoustic microphone capture with speakerphone enabled.",
                    "Official in-app recording export if supported by $displayName."
                )
            )
        } else {
            VoipCompatibilityResult(
                providerId = providerId,
                isSupported = false,
                level = CompatibilityLevel.LEVEL_C,
                explanation = "Direct recording is not supported on this device or application. Available authorized alternatives are shown below.",
                authorizedAlternatives = listOf(
                    "Acoustic microphone capture with speakerphone enabled."
                )
            )
        }
    }

    override suspend fun checkConsent(): VoipConsentResult {
        return VoipConsentResult(
            hasConsent = true,
            noticePresented = "Legal consent must be obtained from all parties participating in the $displayName conversation before recording."
        )
    }

    override suspend fun startRecording(): VoipRecordingResult {
        // Will be routed through the primary RecordingEngine with transparent validation
        return VoipRecordingResult(
            isSuccess = true,
            audioFilePath = null,
            bytesCaptured = 0L
        )
    }

    override suspend fun stopRecording(): VoipRecordingResult {
        return VoipRecordingResult(
            isSuccess = true,
            audioFilePath = null,
            bytesCaptured = 0L
        )
    }
}
