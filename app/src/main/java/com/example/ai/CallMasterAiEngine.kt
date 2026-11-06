package com.example.ai

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.RecordingEntity
import com.example.preferences.CallMasterPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class AiProcessingStatus {
    IDLE,
    WAITING_CONSENT,
    WAITING_KEY,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class AiAnalysisResult(
    val transcript: String,
    val executiveSummary: String,
    val keyPoints: List<String>,
    val actionItems: List<String>,
    val detectedLanguage: String,
    val speakerCountEstimated: Int
)

data class AiAssistantUiState(
    val status: AiProcessingStatus = AiProcessingStatus.IDLE,
    val progressMessage: String = "",
    val activeRecordingId: String? = null,
    val result: AiAnalysisResult? = null,
    val errorMessage: String? = null
)

class CallMasterAiEngine(
    private val database: AppDatabase,
    private val preferences: CallMasterPreferences
) {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(): String {
        val prefKey = preferences.geminiCustomApiKey.trim()
        if (prefKey.isNotBlank()) return prefKey

        val buildKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    suspend fun analyzeRecording(
        recording: RecordingEntity,
        languagePreference: String = "ar", // "ar" or "en"
        customApiKey: String? = null,
        hasUserApprovedCloudProcessing: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasUserApprovedCloudProcessing) {
            _uiState.update {
                it.copy(
                    status = AiProcessingStatus.WAITING_CONSENT,
                    activeRecordingId = recording.id,
                    errorMessage = "الموافقة الصريحة مطلوبة قبل معالجة التسجيل بالذكاء الاصطناعي."
                )
            }
            return@withContext false
        }

        // Save custom API key if provided
        if (!customApiKey.isNullOrBlank()) {
            preferences.geminiCustomApiKey = customApiKey.trim()
        }

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    status = AiProcessingStatus.WAITING_KEY,
                    activeRecordingId = recording.id,
                    errorMessage = "يرجى إدخال مفتاح Gemini API لتشغيل التفريغ الصوتي الحقيقي للمكالمة."
                )
            }
            return@withContext false
        }

        val audioFile = File(recording.filePath)
        if (!audioFile.exists() || audioFile.length() < 512) {
            _uiState.update {
                it.copy(
                    status = AiProcessingStatus.FAILED,
                    activeRecordingId = recording.id,
                    errorMessage = "ملف التسجيل الصوتي غير موجود أو تالف على مساحة الهاتف."
                )
            }
            return@withContext false
        }

        _uiState.update {
            it.copy(
                status = AiProcessingStatus.PROCESSING,
                activeRecordingId = recording.id,
                progressMessage = if (languagePreference == "ar")
                    "جاري قراءة الملف الصوتي وإرساله إلى Gemini AI للتفريغ الحقيقي..."
                else
                    "Reading audio file and sending to Gemini AI for verbatim transcription...",
                errorMessage = null
            )
        }

        try {
            // Read audio bytes and convert to Base64
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val mimeType = if (recording.filePath.endsWith(".wav", true)) "audio/wav" else "audio/mp4"

            val promptInstruction = if (languagePreference == "ar") {
                "أنت محرك احترافي لتفريغ وتحليل المكالمات والتسجيلات الصوتية بدقة عالية. " +
                        "قم بتفريغ كل كلمة منطوقة في هذا التسجيل الصوتي بشكل حقيقي وموثق دون أي تأليف أو تزييف. " +
                        "إذا كانت المكالمة باللغة العربية (سواء كانت فصحى أو لهجة مصرية أو عامية)، اكتب النص الدقيق كما قيل. " +
                        "إذا لم يكن هناك كلام بشري واضح، اذكر ذلك صراحة في خانة transcript. " +
                        "ثم قم بتوليد ملخص تنفيذي للمكالمة، وقائمة بأهم النقاط المتفق عليها، وقائمة ببنود العمل والمهام المستخرجة.\n" +
                        "يجب أن تكون الإجابة بتنسيق JSON حصراً بهذا المخطط:\n" +
                        "{\n" +
                        "  \"transcript\": \"النص الكامل الدقيق المفرغ من الصوت مع أسماء وتوقيت المتحدثين إن أمكن\",\n" +
                        "  \"executiveSummary\": \"ملخص تنفيذي موجز لمحتوى المكالمة\",\n" +
                        "  \"keyPoints\": [\"النقطة الأولى\", \"النقطة الثانية\"],\n" +
                        "  \"actionItems\": [\"المهمة الأولى\", \"المهمة الثانية\"],\n" +
                        "  \"detectedLanguage\": \"العربية\",\n" +
                        "  \"speakerCountEstimated\": 2\n" +
                        "}"
            } else {
                "You are an expert audio transcription and call analysis engine. " +
                        "Transcribe every spoken word in this audio file verbatim without hallucinations or placeholder text. " +
                        "Provide a concise executive summary, list of key discussion points, and action items. " +
                        "Respond strictly in valid JSON format matching this schema:\n" +
                        "{\n" +
                        "  \"transcript\": \"full verbatim transcription of spoken words with speaker labels/timestamps if possible\",\n" +
                        "  \"executiveSummary\": \"concise summary of the conversation\",\n" +
                        "  \"keyPoints\": [\"point 1\", \"point 2\"],\n" +
                        "  \"actionItems\": [\"action 1\", \"action 2\"],\n" +
                        "  \"detectedLanguage\": \"English\",\n" +
                        "  \"speakerCountEstimated\": 2\n" +
                        "}"
            }

            // Construct Gemini multimodal request payload
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Part 1: Inline audio bytes
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                            // Part 2: Transcription instructions
                            put(JSONObject().apply {
                                put("text", promptInstruction)
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.2)
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            _uiState.update {
                it.copy(
                    progressMessage = if (languagePreference == "ar")
                        "جاري استلام وتحليل النص المفرغ من Gemini AI..."
                    else
                        "Receiving and parsing AI transcription..."
                )
            }

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                Log.e("CallMasterAi", "Gemini API error: $errorMsg")
                _uiState.update {
                    it.copy(
                        status = AiProcessingStatus.FAILED,
                        errorMessage = "فشل الاتصال بـ Gemini API: $errorMsg"
                    )
                }
                return@withContext false
            }

            // Parse response
            val rootObj = JSONObject(responseBody)
            val candidates = rootObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text").orEmpty()

            if (textOutput.isBlank()) {
                _uiState.update {
                    it.copy(
                        status = AiProcessingStatus.FAILED,
                        errorMessage = "استجابة خالية من خادم الذكاء الاصطناعي."
                    )
                }
                return@withContext false
            }

            val parsedJson = JSONObject(textOutput)
            val transcript = parsedJson.optString("transcript", "لم يتم العثور على صوت بشري واضح في هذا التسجيل.")
            val summary = parsedJson.optString("executiveSummary", "تمت مراجعة التسجيل الصوتي بنجاح.")

            val keyPointsList = mutableListOf<String>()
            val keyPointsArray = parsedJson.optJSONArray("keyPoints")
            if (keyPointsArray != null) {
                for (i in 0 until keyPointsArray.length()) {
                    keyPointsList.add(keyPointsArray.getString(i))
                }
            }

            val actionItemsList = mutableListOf<String>()
            val actionItemsArray = parsedJson.optJSONArray("actionItems")
            if (actionItemsArray != null) {
                for (i in 0 until actionItemsArray.length()) {
                    actionItemsList.add(actionItemsArray.getString(i))
                }
            }

            val lang = parsedJson.optString("detectedLanguage", languagePreference)
            val speakerCount = parsedJson.optInt("speakerCountEstimated", 1)

            val analysisResult = AiAnalysisResult(
                transcript = transcript,
                executiveSummary = summary,
                keyPoints = keyPointsList,
                actionItems = actionItemsList,
                detectedLanguage = lang,
                speakerCountEstimated = speakerCount
            )

            // Update database with real transcript
            val updated = recording.copy(
                transcript = analysisResult.transcript,
                aiSummary = analysisResult.executiveSummary,
                aiKeyPoints = analysisResult.keyPoints.joinToString("\n• "),
                aiActionItems = analysisResult.actionItems.joinToString("\n• "),
                aiLanguage = languagePreference
            )
            database.recordingDao().updateRecording(updated)

            _uiState.update {
                it.copy(
                    status = AiProcessingStatus.COMPLETED,
                    result = analysisResult,
                    progressMessage = if (languagePreference == "ar") "اكتمل التفريغ الحقيقي بنجاح!" else "Verbatim transcription completed!"
                )
            }
            true

        } catch (e: Exception) {
            Log.e("CallMasterAi", "Exception during transcription", e)
            _uiState.update {
                it.copy(
                    status = AiProcessingStatus.FAILED,
                    errorMessage = "خطأ أثناء التفريغ الصوتي: ${e.localizedMessage ?: "فشل الاتصال بالشبكة"}"
                )
            }
            false
        }
    }

    fun reset() {
        _uiState.update { AiAssistantUiState() }
    }
}
