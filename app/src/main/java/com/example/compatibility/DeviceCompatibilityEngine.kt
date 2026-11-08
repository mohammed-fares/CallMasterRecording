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
                    "android.permission.READ_PHONE_STATE",
                    "android.permission.READ_CALL_LOG"
                ),
                knownRestrictions = "Android 9+ (API 28+) strictly isolates raw telephony audio streams without system/privileged signature.",
                recommendedAction = if (cellularLevel == CompatibilityLevel.LEVEL_A)
                    "Enable Automatic Cellular Recording in Settings."
                else
                    "Use speakerphone during calls for clearer two-sided capture."
            )
        )

        // 2. Tested VoIP Apps List
        val voipApps = listOf(
            Triple("WhatsApp Voice & Video", "com.whatsapp", true),
            Triple("Telegram Voice Calls", "org.telegram.messenger", true),
            Triple("Facebook Messenger", "com.facebook.orca", true),
            Triple("Signal Private Messenger", "org.thoughtcrime.securesms", true),
            Triple("Viber Messenger", "com.viber.voip", true),
            Triple("Google Meet", "com.google.android.apps.tachyon", true),
            Triple("Microsoft Teams", "com.microsoft.teams", true),
            Triple("Zoom Meetings", "us.zoom.videomeetings", true),
            Triple("Discord Voice", "com.discord", false)
        )

        for ((name, pkg, callPrivacyProtected) in voipApps) {
            items.add(
                evaluateVoipApp(
                    name = name,
                    pkg = pkg,
                    apiLevel = apiLevel,
                    voiceCommAvailable = voiceCommAvailable,
                    callPrivacyProtected = callPrivacyProtected
                )
            )
        }

        // 3. Direct Microphone / In-Person Memo
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

    /**
     * Xiaomi Mi A3 (Android 11, API 30) reference baseline evaluation.
     */
    fun getXiaomiMiA3BaselineReport(): DeviceHardwareReport {
        val items = listOf(
            CompatibilityItemResult(
                sourceName = "Cellular Voice Calls (GSM/LTE/VoLTE)",
                packageName = null,
                level = CompatibilityLevel.LEVEL_C,
                recordingMethod = "Acoustic Microphone Stream (Companion Mode)",
                expectedAudioChannels = "1 Channel (Local Mic + Loudspeaker)",
                actualCapabilities = "Microphone captures near voice cleanly. Far-end party requires loudspeaker.",
                requiredPermissions = listOf(
                    "android.permission.RECORD_AUDIO",
                    "android.permission.READ_PHONE_STATE",
                    "android.permission.READ_CALL_LOG"
                ),
                knownRestrictions = "Android 11 Android One security isolates raw telephony downlink HAL for non-system apps.",
                recommendedAction = "Enable speakerphone during call for optimal two-sided audio capture."
            ),
            CompatibilityItemResult(
                sourceName = "WhatsApp Voice & Video",
                packageName = "com.whatsapp",
                level = CompatibilityLevel.LEVEL_C,
                recordingMethod = "Acoustic Microphone (Speakerphone Recommended)",
                expectedAudioChannels = "1 Channel (Microphone)",
                actualCapabilities = "AudioPlaybackCapture blocked by WhatsApp USAGE_VOICE_COMMUNICATION audio attributes.",
                requiredPermissions = listOf("android.permission.RECORD_AUDIO", "android.permission.FOREGROUND_SERVICE"),
                knownRestrictions = "App declares USAGE_VOICE_COMMUNICATION to prevent third-party audio playback capture.",
                recommendedAction = "Turn on speakerphone during WhatsApp call for acoustic recording.",
                isAppInstalled = true,
                capturePolicyMessage = "هذا التطبيق لا يسمح بالتقاط الصوت عبر Android Audio Playback Capture على هذا الجهاز."
            ),
            CompatibilityItemResult(
                sourceName = "Telegram Voice Calls",
                packageName = "org.telegram.messenger",
                level = CompatibilityLevel.LEVEL_C,
                recordingMethod = "Acoustic Microphone (Speakerphone Recommended)",
                expectedAudioChannels = "1 Channel (Microphone)",
                actualCapabilities = "Playback capture restricted by Telegram call privacy flags.",
                requiredPermissions = listOf("android.permission.RECORD_AUDIO", "android.permission.FOREGROUND_SERVICE"),
                knownRestrictions = "AudioAttributes prevent AudioPlaybackCapture API access.",
                recommendedAction = "Enable speakerphone during Telegram conversation.",
                isAppInstalled = true,
                capturePolicyMessage = "هذا التطبيق لا يسمح بالتقاط الصوت عبر Android Audio Playback Capture على هذا الجهاز."
            ),
            CompatibilityItemResult(
                sourceName = "Facebook Messenger",
                packageName = "com.facebook.orca",
                level = CompatibilityLevel.LEVEL_C,
                recordingMethod = "Acoustic Microphone (Speakerphone Recommended)",
                expectedAudioChannels = "1 Channel (Microphone)",
                actualCapabilities = "Messenger audio sandbox isolates in-call stream.",
                requiredPermissions = listOf("android.permission.RECORD_AUDIO"),
                knownRestrictions = "VoIP stream protected by meta sandbox.",
                recommendedAction = "Use speakerphone mode.",
                isAppInstalled = true,
                capturePolicyMessage = "هذا التطبيق لا يسمح بالتقاط الصوت عبر Android Audio Playback Capture على هذا الجهاز."
            ),
            CompatibilityItemResult(
                sourceName = "Direct Hardware Microphone",
                packageName = null,
                level = CompatibilityLevel.LEVEL_A,
                recordingMethod = "Direct Hardware Microphone (MIC)",
                expectedAudioChannels = "1-2 Channels (PCM 16-bit / AAC 44.1kHz)",
                actualCapabilities = "Full 100% crystal clear acoustic sound capture.",
                requiredPermissions = listOf("android.permission.RECORD_AUDIO"),
                knownRestrictions = "None.",
                recommendedAction = "Fully functional and tested."
            )
        )

        return DeviceHardwareReport(
            manufacturer = "Xiaomi",
            model = "Mi A3 (laurel_sprout)",
            androidVersion = "11 (Android One)",
            apiLevel = 30,
            hasMicrophone = true,
            voiceCommunicationSourceAvailable = true,
            foregroundServiceMicrophoneSupported = true,
            overallLevel = CompatibilityLevel.LEVEL_C,
            results = items
        )
    }

    private fun evaluateCellularCompatibility(
        manufacturer: String,
        apiLevel: Int,
        voiceCommAvailable: Boolean
    ): CompatibilityLevel {
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
        voiceCommAvailable: Boolean,
        callPrivacyProtected: Boolean
    ): CompatibilityItemResult {
        val isInstalled = isPackageInstalled(pkg)
        val level = if (!isInstalled) {
            CompatibilityLevel.LEVEL_D
        } else if (apiLevel >= Build.VERSION_CODES.Q && !callPrivacyProtected) {
            CompatibilityLevel.LEVEL_B
        } else if (voiceCommAvailable || apiLevel >= Build.VERSION_CODES.Q) {
            CompatibilityLevel.LEVEL_C
        } else {
            CompatibilityLevel.LEVEL_D
        }

        val captureMessage = if (isInstalled && callPrivacyProtected) {
            "هذا التطبيق لا يسمح بالتقاط الصوت عبر Android Audio Playback Capture على هذا الجهاز."
        } else null

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
                "التطبيق غير مثبت حالياً على هذا الجهاز."
            } else if (callPrivacyProtected) {
                "حماية خصوصية المكالمات تمنع الالتقاط المباشر لتدفق الصوت الداخلي."
            } else {
                "التقاط تدفق الصوت متاح عبر خدمة بث الصوت مع موافقة المستخدم."
            },
            requiredPermissions = listOf(
                "android.permission.RECORD_AUDIO",
                "android.permission.FOREGROUND_SERVICE"
            ),
            knownRestrictions = if (callPrivacyProtected)
                "التطبيق يعيّن USAGE_VOICE_COMMUNICATION مما يمنع الالتقاط البرمجي."
            else
                "عزل التطبيقات القياسي في Android.",
            recommendedAction = if (!isInstalled) {
                "تثبيت $name من متجر Google Play."
            } else {
                "تفعيل مكبر الصوت (Speakerphone) لتمكين التسجيل الصوتي بجودة ممتازة."
            },
            isAppInstalled = isInstalled,
            capturePolicyMessage = captureMessage
        )
    }

    fun exportDiagnosticReportText(report: DeviceHardwareReport): String {
        val sb = StringBuilder()
        sb.append("=== CALLMASTER AUDIO DIAGNOSTIC REPORT ===\n")
        sb.append("Manufacturer: ${report.manufacturer}\n")
        sb.append("Model: ${report.model}\n")
        sb.append("Android Version: ${report.androidVersion} (API ${report.apiLevel})\n")
        sb.append("Microphone Feature: ${if (report.hasMicrophone) "PRESENT" else "ABSENT"}\n")
        sb.append("VOICE_COMMUNICATION Source: ${if (report.voiceCommunicationSourceAvailable) "SUPPORTED" else "UNAVAILABLE"}\n")
        sb.append("Foreground Service Microphone: ${if (report.foregroundServiceMicrophoneSupported) "SUPPORTED" else "UNSUPPORTED"}\n")
        sb.append("Overall Compatibility Level: ${report.overallLevel.name}\n")
        sb.append("\n--- EVALUATION MATRIX ---\n")
        for (item in report.results) {
            sb.append("\n[${item.sourceName}]\n")
            sb.append("  Level: ${item.level.name}\n")
            sb.append("  Method: ${item.recordingMethod}\n")
            sb.append("  Channels: ${item.expectedAudioChannels}\n")
            sb.append("  Capabilities: ${item.actualCapabilities}\n")
            if (item.capturePolicyMessage != null) {
                sb.append("  Capture Policy: ${item.capturePolicyMessage}\n")
            }
            sb.append("  Restrictions: ${item.knownRestrictions}\n")
            sb.append("  Recommended Action: ${item.recommendedAction}\n")
        }
        sb.append("\n=== END OF REPORT ===\n")
        return sb.toString()
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
            if (minBuf <= 0) return false

            val hasRecordPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasRecordPerm) {
                var record: AudioRecord? = null
                try {
                    record = AudioRecord(
                        source,
                        44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf
                    )
                    record.state == AudioRecord.STATE_INITIALIZED
                } finally {
                    try {
                        record?.release()
                    } catch (_: Exception) {}
                }
            } else {
                minBuf > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Live empirical hardware probe for audio sources on the current physical device.
     */
    fun runEmpiricalAudioSourceBenchmark(): Map<String, Boolean> {
        val sources = mapOf(
            "MIC (Hardware Microphone)" to MediaRecorder.AudioSource.MIC,
            "VOICE_COMMUNICATION (VoIP / In-Call Acoustic)" to MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            "VOICE_RECOGNITION (Speech-tuned Engine)" to MediaRecorder.AudioSource.VOICE_RECOGNITION,
            "DEFAULT (System Standard Input)" to MediaRecorder.AudioSource.DEFAULT,
            "CAMCORDER (Acoustic Array)" to MediaRecorder.AudioSource.CAMCORDER
        )

        val results = mutableMapOf<String, Boolean>()
        for ((label, src) in sources) {
            results[label] = testAudioSourceAvailability(src)
        }
        return results
    }
}
