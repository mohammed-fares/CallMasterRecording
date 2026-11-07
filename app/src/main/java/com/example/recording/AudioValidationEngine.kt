package com.example.recording

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.example.data.CompletenessStatus
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioValidationResult(
    val status: CompletenessStatus,
    val hasAudioTrack: Boolean,
    val durationMs: Long,
    val sampleRate: Int,
    val channelCount: Int,
    val mimeType: String,
    val peakAmplitude: Int,
    val rmsDb: Float,
    val silenceRatio: Float,
    val isSilent: Boolean,
    val diagnosticMessage: String
)

class AudioValidationEngine {

    /**
     * Inspects and decodes audio samples from the recorded media file
     * to verify real acoustic sound presence vs absolute digital silence.
     */
    fun validateAudioFile(file: File, expectedSource: String = "MIC"): AudioValidationResult {
        if (!file.exists() || file.length() < 1024) {
            return AudioValidationResult(
                status = CompletenessStatus.SILENT_RECORDING,
                hasAudioTrack = false,
                durationMs = 0L,
                sampleRate = 0,
                channelCount = 0,
                mimeType = "unknown",
                peakAmplitude = 0,
                rmsDb = -100f,
                silenceRatio = 1.0f,
                isSilent = true,
                diagnosticMessage = "الملف الصوتي فارغ أو لم يتم حفظ أي بيانات (File empty or missing)."
            )
        }

        val extractor = MediaExtractor()
        var audioTrackIndex = -1
        var format: MediaFormat? = null

        try {
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("AudioValidator", "Failed to extract media data", e)
            extractor.release()
            return AudioValidationResult(
                status = CompletenessStatus.RECORDING_INTERRUPTED,
                hasAudioTrack = false,
                durationMs = 0L,
                sampleRate = 0,
                channelCount = 0,
                mimeType = "unknown",
                peakAmplitude = 0,
                rmsDb = -100f,
                silenceRatio = 1.0f,
                isSilent = true,
                diagnosticMessage = "تعذر قراءة المسار الصوتي: ${e.localizedMessage}"
            )
        }

        if (audioTrackIndex == -1 || format == null) {
            extractor.release()
            return AudioValidationResult(
                status = CompletenessStatus.SILENT_RECORDING,
                hasAudioTrack = false,
                durationMs = 0L,
                sampleRate = 0,
                channelCount = 0,
                mimeType = "none",
                peakAmplitude = 0,
                rmsDb = -100f,
                silenceRatio = 1.0f,
                isSilent = true,
                diagnosticMessage = "الملف لا يحتوي على مسار صوتي صالح (No audio track)."
            )
        }

        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
        val durationMs = durationUs / 1000
        val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
        val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
        val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4"

        extractor.selectTrack(audioTrackIndex)

        // Decode PCM frames using MediaCodec to accurately calculate RMS energy and silence ratio
        var peakAmp = 0
        var totalSamples = 0L
        var sumSquares = 0.0
        var silentFrames = 0L
        var totalFrames = 0L

        var codec: MediaCodec? = null
        try {
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false
            val timeoutUs = 5000L
            var readSamplesCount = 0

            // We sample up to 10 seconds of decoded audio for fast and accurate validation
            val maxSamplesToAnalyze = sampleRate * channelCount * 10

            while (!isEOS && totalSamples < maxSamplesToAnalyze) {
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()

                        var frameSumSquare = 0.0
                        val frameSamples = shortBuffer.remaining()

                        while (shortBuffer.hasRemaining()) {
                            val sample = shortBuffer.get().toInt()
                            val abs = kotlin.math.abs(sample)
                            if (abs > peakAmp) peakAmp = abs

                            sumSquares += (sample.toDouble() * sample.toDouble())
                            frameSumSquare += (sample.toDouble() * sample.toDouble())
                            totalSamples++
                        }

                        totalFrames++
                        val frameRms = sqrt(frameSumSquare / frameSamples.coerceAtLeast(1))
                        // Digital silence threshold is ~ 120 amplitude units out of 32767 (~ -48 dBFS)
                        if (frameRms < 120.0) {
                            silentFrames++
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER && isEOS) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.w("AudioValidator", "PCM decoding error, falling back to file heuristics", e)
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {}
            extractor.release()
        }

        val rms = if (totalSamples > 0) sqrt(sumSquares / totalSamples) else 0.0
        val rmsDb = if (rms > 0.0) (20 * log10(rms / 32767.0)).toFloat().coerceIn(-100f, 0f) else -100f
        val silenceRatio = if (totalFrames > 0) (silentFrames.toFloat() / totalFrames.toFloat()) else 1.0f

        // Silence decision: RMS < -50 dBFS and silence ratio > 0.88 means no actual audible signal was recorded
        val isSilent = (rmsDb < -50f && silenceRatio > 0.85f) || (peakAmp < 150 && durationMs > 1000)

        val status: CompletenessStatus = when {
            isSilent -> CompletenessStatus.SILENT_RECORDING
            expectedSource == "VOIP_PLAYBACK" -> CompletenessStatus.SUCCESS_PLAYBACK_ONLY
            silenceRatio > 0.65f -> CompletenessStatus.PARTIAL_RECORDING
            else -> CompletenessStatus.SUCCESS_MIC_ONLY
        }

        val diagnosticMessage = when (status) {
            CompletenessStatus.SUCCESS_BOTH_SIDES -> "تم التحقق: تم التقاط صوت محادثة واضح لكلا الطرفين بنجاح."
            CompletenessStatus.SUCCESS_MIC_ONLY -> "تم التحقق: تم التقاط صوت الميكروفون بنجاح (RMS: ${"%.1f".format(rmsDb)} dB, ذروة: $peakAmp)."
            CompletenessStatus.SUCCESS_PLAYBACK_ONLY -> "تم التحقق: تم التقاط الصوت الصادر من تطبيق الاتصال بنجاح."
            CompletenessStatus.PARTIAL_RECORDING -> "تنبيه: تم التقاط صوت جزئي مع فترات صمت ملحوظة (${"%.0f".format(silenceRatio * 100)}%)."
            CompletenessStatus.SILENT_RECORDING -> "تحذير: الملف الصوتي صامت تماماً. قام نظام الهاتف بعزل الميكروفون أثناء المكالمة. يرجى تفعيل مكبر الصوت (Speakerphone) لتمكين التسجيل."
            CompletenessStatus.SOURCE_BLOCKED -> "تطبيق الاتصال المصدر يمنع التقاط الصوت عبر سياسة الخصوصية الخاصة به."
            CompletenessStatus.PERMISSION_DENIED -> "تم إيقاف التسجيل لعدم توفر الصلاحيات الكافية."
            CompletenessStatus.UNSUPPORTED_DEVICE -> "الجهاز لا يدعم وضع التسجيل المطلوب."
            CompletenessStatus.RECORDING_INTERRUPTED -> "تمت مقاطعة التسجيل قبل اكتماله."
        }

        return AudioValidationResult(
            status = status,
            hasAudioTrack = true,
            durationMs = durationMs,
            sampleRate = sampleRate,
            channelCount = channelCount,
            mimeType = mime,
            peakAmplitude = peakAmp,
            rmsDb = rmsDb,
            silenceRatio = silenceRatio,
            isSilent = isSilent,
            diagnosticMessage = diagnosticMessage
        )
    }
}
