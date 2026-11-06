package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.AiProcessingStatus
import com.example.ai.CallMasterAiEngine
import com.example.data.RecordingDao
import com.example.data.RecordingEntity
import com.example.player.CallMasterAudioPlayer
import com.example.ui.components.formatDuration
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGold
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    aiEngine: CallMasterAiEngine,
    recordingDao: RecordingDao,
    audioPlayer: CallMasterAudioPlayer,
    preSelectedRecording: RecordingEntity? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val uiState by aiEngine.uiState.collectAsState()
    val allRecordings by recordingDao.getAllRecordings().collectAsState(initial = emptyList())

    var selectedRecording by remember(preSelectedRecording, allRecordings) {
        mutableStateOf(preSelectedRecording ?: allRecordings.firstOrNull())
    }
    var pickerExpanded by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("ar") } // "ar" or "en"
    var apiKeyInput by remember { mutableStateOf(aiEngine.getEffectiveApiKey()) }
    var showApiKeyField by remember { mutableStateOf(aiEngine.getEffectiveApiKey().isBlank()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("ai_assistant_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = RadiantGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.ai_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = RadiantGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real Multimodal Gemini Audio Transcription",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // API Key Status Banner
        item {
            val hasKey = aiEngine.getEffectiveApiKey().isNotBlank()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (hasKey) StatusSuccess else RadiantGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasKey) "Gemini API Engine Active" else "Gemini API Key Required",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (hasKey) StatusSuccess else RadiantGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (showApiKeyField) "Hide Key" else "Configure Key",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantGold,
                            modifier = Modifier.clickable { showApiKeyField = !showApiKeyField }
                        )
                    }

                    if (showApiKeyField) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RadiantGold,
                                unfocusedBorderColor = PetroleumGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (allRecordings.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = PetroleumGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد تسجيلات حتى الآن",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "قم بإجراء أو تسجيل مكالمة هاتفية أو مذكرة صوتية أولاً ليتم تفريغ محتواها الحقيقي بالذكاء الاصطناعي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Recording Selector
            item {
                Text(
                    text = "اختر التسجيل المراد تفريغه (Select Recording)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = pickerExpanded,
                    onExpandedChange = { pickerExpanded = !pickerExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRecording?.let { "${it.title} (${formatDuration(it.durationMs)})" } ?: "No recording selected",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickerExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantGold,
                            unfocusedBorderColor = PetroleumGreen
                        ),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = pickerExpanded,
                        onDismissRequest = { pickerExpanded = false }
                    ) {
                        allRecordings.forEach { rec ->
                            DropdownMenuItem(
                                text = { Text("${rec.title} • ${rec.sourceType.name} (${formatDuration(rec.durationMs)})") },
                                onClick = {
                                    selectedRecording = rec
                                    pickerExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Language Choice
            item {
                Text(
                    text = "لغة المكالمة والتفريغ (Transcription Language)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "ar",
                            onClick = { selectedLanguage = "ar" },
                            colors = RadioButtonDefaults.colors(selectedColor = RadiantGold)
                        )
                        Text(
                            text = stringResource(R.string.ai_language_ar),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "en",
                            onClick = { selectedLanguage = "en" },
                            colors = RadioButtonDefaults.colors(selectedColor = RadiantGold)
                        )
                        Text(
                            text = stringResource(R.string.ai_language_en),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Action Trigger Button
            item {
                Button(
                    onClick = {
                        selectedRecording?.let { rec ->
                            scope.launch {
                                aiEngine.analyzeRecording(
                                    recording = rec,
                                    languagePreference = selectedLanguage,
                                    customApiKey = apiKeyInput.ifBlank { null },
                                    hasUserApprovedCloudProcessing = true
                                )
                            }
                        }
                    },
                    enabled = selectedRecording != null && uiState.status != AiProcessingStatus.PROCESSING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantGold,
                        contentColor = DarkGreen,
                        disabledContainerColor = PetroleumGreen.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("trigger_ai_analysis_btn")
                ) {
                    if (uiState.status == AiProcessingStatus.PROCESSING) {
                        CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(uiState.progressMessage, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ai_process_now), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Error display if failed
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Display Results
            val analysis = uiState.result
            val activeRec = selectedRecording

            if (analysis != null) {
                // Executive Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RadiantGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.ai_summary),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RadiantGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = analysis.executiveSummary,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Key Points
                if (analysis.keyPoints.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = stringResource(R.string.ai_key_points),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RadiantGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                analysis.keyPoints.forEach { point ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("• ", color = RadiantGold, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Items
                if (analysis.actionItems.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = stringResource(R.string.ai_action_items),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RadiantGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                analysis.actionItems.forEach { item ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Transcription View
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_transcription),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RadiantGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    FilledIconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(analysis.transcript))
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = PetroleumGreen,
                                            contentColor = LightGold
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledIconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "CallMaster AI Transcript")
                                                putExtra(Intent.EXTRA_TEXT, "${analysis.executiveSummary}\n\nTranscript:\n${analysis.transcript}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Transcript"))
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = PetroleumGreen,
                                            contentColor = LightGold
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = analysis.transcript,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            } else if (activeRec?.transcript != null) {
                // Previously saved real transcript
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "الملخص التنفيذي المحفوظ",
                                style = MaterialTheme.typography.titleMedium,
                                color = RadiantGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeRec.aiSummary ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "التفريغ الصوتي المحفوظ",
                                style = MaterialTheme.typography.titleMedium,
                                color = RadiantGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeRec.transcript ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
