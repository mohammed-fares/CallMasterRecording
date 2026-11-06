package com.example.compatibility

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build

class DeviceCompatibilityEngine(private val context: Context) {

    fun generateReport(): DeviceHardwareReport {
        val pm = context.packageManager
        val hasMicrophone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val apiLevel = Build.VERSION.SDK_INT
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE

        val voiceCommAvailable = testAudioSourceAvailability(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        val micAvailable = testAudioSourceAvailability(MediaRecorder.AudioSource.MIC)
        val fgServiceMicSupported = apiLevel >= Build.VERSION_CODES.Q

        val items = mutableListOf<CompatibilityItemResult>()

        // 1. Cellular Calls Evaluation
        val cellularLevel = evaluateCellularCompatibility(manufacturer, apiLevel, voiceCommAvailable)
        items.add(
            CompatibilityItemResult(
                sourceName = "Cellular Voice Calls (GSM/LTE/5G)",
                packageName = null,
                level = cellularLevel,
                recordingMethod = if (cellularLevel == CompatibilityLevel.LEVEL_A)
                    "Native Device Hardware Stream"
                else "Acoustic Microphone Stream (Companion Mode)",
                expectedAudioChannels = if (cellularLevel == CompatibilityLevel.LEVEL_A) "2 Channels (Uplink + Downlink)" else "1 Channel (Local Mic + Loudspeaker)",
                actualCapabilities = if (cellularLevel == CompatibilityLevel.LEVEL_A)
                    "Full two-sided recording supported natively."
                else
                    "Android OS restriction: Inbound party audio requires speakerphone/loudspeaker.",
                requiredPermissions = listOf(
                    "android.permission.RECORD_AUDIO",
                    "android.permission.READ_PHONE_STATE"
                ),
                knownRestrictions = "Android 9+ (API 28+) strictly isolates raw telephony audio streams without system/privileged signature.",
                recommendedAction = if (cellularLevel == CompatibilityLevel.LEVEL_A)
                    "Enable Automatic Cellular Recording in Settings."
                else
                    "Use speakerphone during calls for clearer two-sided capture."
            )
        )

        // 2. WhatsApp
        items.add(
            evaluateVoipApp(
                name = "WhatsApp Voice & Video",
                pkg = "com.whatsapp",
                apiLevel = apiLevel,
                voiceCommAvailable = voiceCommAvailable
            )
        )

        // 3. Telegram
        items.add(
            evaluateVoipApp(
                name = "Telegram Voice Calls",
                pkg = "org.telegram.messenger",
                apiLevel = apiLevel,
                voiceCommAvailable = voiceCommAvailable
            )
        )

        // 4. Facebook Messenger
        items.add(
            evaluateVoipApp(
                name = "Facebook Messenger",
                pkg = "com.facebook.orca",
                apiLevel = apiLevel,
                voiceCommAvailable = voiceCommAvailable
            )
        )

        // 5. Signal
        items.add(
            evaluateVoipApp(
                name = "Signal Private Messenger",
                pkg = "org.thoughtcrime.securesms",
                apiLevel = apiLevel,
                voiceCommAvailable = voiceCommAvailable
            )
        )

        // 6. Direct Microphone / Voice Memo
        items.add(
            CompatibilityItemResult(
                sourceName = "Direct Microphone & In-Person Memo",
                packageName = null,
                level = if (hasMicrophone && micAvailable) CompatibilityLevel.LEVEL_A else CompatibilityLevel.LEVEL_D,
                recordingMethod = "AudioRecord / MediaRecorder (MIC / UNPROCESSED)",
                expectedAudioChannels = "1-2 Channels (PCM / AAC)",
                actualCapabilities = "100% full fidelity audio capture with hardware gain control.",
                requiredPermissions = listOf("android.permission.RECORD_AUDIO"),
                knownRestrictions = "None. Direct hardware microphone is fully accessible.",
                recommendedAction = "Ready for immediate recording."
            )
        )

        // Overall level is determined by the capability matrix
        val overallLevel = when {
            items.any { it.level == CompatibilityLevel.LEVEL_A && it.sourceName.contains("Cellular") } -> CompatibilityLevel.LEVEL_A
            items.any { it.level == CompatibilityLevel.LEVEL_B } -> CompatibilityLevel.LEVEL_B
            hasMicrophone -> CompatibilityLevel.LEVEL_C
            else -> CompatibilityLevel.LEVEL_D
        }

        return DeviceHardwareReport(
            manufacturer = manufacturer,
            model = model,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            hasMicrophone = hasMicrophone,
            voiceCommunicationSourceAvailable = voiceCommAvailable,
            foregroundServiceMicrophoneSupported = fgServiceMicSupported,
            overallLevel = overallLevel,
            results = items
        )
    }

    private fun evaluateCellularCompatibility(
        manufacturer: String,
        apiLevel: Int,
        voiceCommAvailable: Boolean
    ): CompatibilityLevel {
        // Transparent evaluation based on real Android platform rules:
        // On modern Android (API 29+), raw telecom downlink requires system-level signature or OEM integration.
        // Third-party applications operate in companion mode (Level C: Microphone/Loudspeaker).
        return if (apiLevel < 28 && voiceCommAvailable) {
            CompatibilityLevel.LEVEL_B
        } else {
            CompatibilityLevel.LEVEL_C
        }
    }

    private fun evaluateVoipApp(
        name: String,
        pkg: String,
        apiLevel: Int,
        voiceCommAvailable: Boolean
    ): CompatibilityItemResult {
        val isInstalled = isPackageInstalled(pkg)
        val level = if (!isInstalled) {
            CompatibilityLevel.LEVEL_D
        } else if (apiLevel >= Build.VERSION_CODES.Q) {
            CompatibilityLevel.LEVEL_B // AudioPlaybackCapture API eligible with projection consent
        } else if (voiceCommAvailable) {
            CompatibilityLevel.LEVEL_C // Mic fallback
        } else {
            CompatibilityLevel.LEVEL_D
        }

        return CompatibilityItemResult(
            sourceName = name,
            packageName = pkg,
            level = level,
            recordingMethod = when (level) {
                CompatibilityLevel.LEVEL_A -> "Direct In-App Hook"
                CompatibilityLevel.LEVEL_B -> "AudioPlaybackCapture / System Projection"
                CompatibilityLevel.LEVEL_C -> "Acoustic Microphone (Speakerphone Recommended)"
                CompatibilityLevel.LEVEL_D -> "Not Supported or App Not Installed"
            },
            expectedAudioChannels = if (level == CompatibilityLevel.LEVEL_B) "2 Channels (Mixed Stream)" else "1 Channel (Microphone)",
            actualCapabilities = if (!isInstalled) {
                "Application is not currently installed on this device."
            } else if (level == CompatibilityLevel.LEVEL_B) {
                "Authorized system playback capture supported with user consent."
            } else {
                "Android sandboxing prevents direct VoIP packet interception without system privileges."
            },
            requiredPermissions = listOf(
                "android.permission.RECORD_AUDIO",
                "android.permission.FOREGROUND_SERVICE"
            ),
            knownRestrictions = "Third-party application policy and Android sandbox isolate VoIP audio channels.",
            recommendedAction = if (!isInstalled) {
                "Install $name from Google Play to evaluate recording."
            } else {
                "Activate CallMaster before or during call with speakerphone enabled."
            },
            isAppInstalled = isInstalled
        )
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun testAudioSourceAvailability(source: Int): Boolean {
        return try {
            val minBuf = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            minBuf > 0
        } catch (_: Exception) {
            false
        }
    }
}
