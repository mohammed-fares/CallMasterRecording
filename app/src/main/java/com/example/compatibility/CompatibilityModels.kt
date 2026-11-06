package com.example.compatibility

enum class CompatibilityLevel {
    LEVEL_A, // Native Supported Recording (Full 2-sided)
    LEVEL_B, // Authorized Alternative Recording (System Audio Capture with consent)
    LEVEL_C, // Partial Recording (Microphone audio only)
    LEVEL_D  // Unsupported
}

data class CompatibilityItemResult(
    val sourceName: String,
    val packageName: String?,
    val level: CompatibilityLevel,
    val recordingMethod: String,
    val expectedAudioChannels: String,
    val actualCapabilities: String,
    val requiredPermissions: List<String>,
    val knownRestrictions: String,
    val recommendedAction: String,
    val isAppInstalled: Boolean = true
)

data class DeviceHardwareReport(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val hasMicrophone: Boolean,
    val voiceCommunicationSourceAvailable: Boolean,
    val foregroundServiceMicrophoneSupported: Boolean,
    val overallLevel: CompatibilityLevel,
    val results: List<CompatibilityItemResult>,
    val generatedTimestamp: Long = System.currentTimeMillis()
)
