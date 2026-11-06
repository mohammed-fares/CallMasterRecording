package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.CallDirection
import com.example.data.RecordingSource
import com.example.recording.CallMasterRecordingEngine
import com.example.recording.RecordingEngineState
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
fun CallMasterActiveRecordingDialog(
    engine: CallMasterRecordingEngine,
    onDismiss: () -> Unit
) {
    val session by engine.sessionState.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedSource by remember { mutableStateOf(RecordingSource.MIC) }
    var contactName by remember { mutableStateOf("") }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(onDismissRequest = {
        if (session.state != RecordingEngineState.RECORDING && session.state != RecordingEngineState.PAUSED) {
            engine.resetStateToIdle()
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("active_recording_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CallMaster Recording Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = RadiantGold,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            if (session.state == RecordingEngineState.RECORDING) {
                                engine.cancelRecording()
                            }
                            engine.resetStateToIdle()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Banner
                val (statusColor, statusText) = when (session.state) {
                    RecordingEngineState.IDLE -> Pair(PetroleumGreen, "Ready to Record")
                    RecordingEngineState.CHECKING_COMPATIBILITY -> Pair(RadiantGold, "Checking Hardware & Audio Routing...")
                    RecordingEngineState.RECORDING -> Pair(StatusError, "● Live Recording")
                    RecordingEngineState.PAUSED -> Pair(LightGold, "Recording Paused")
                    RecordingEngineState.FINALIZING -> Pair(RadiantGold, "Finalizing Stream...")
                    RecordingEngineState.VALIDATING -> Pair(RadiantGold, "Validating Audio Quality...")
                    RecordingEngineState.SAVED -> Pair(StatusSuccess, "Saved & Encrypted Successfully!")
                    RecordingEngineState.FAILED -> Pair(StatusError, session.errorMessage ?: "Recording Failed")
                    else -> Pair(PetroleumGreen, session.state.name)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (session.state == RecordingEngineState.IDLE || session.state == RecordingEngineState.FAILED) {
                    // Source selection
                    ExposedDropdownMenuBox(
                        expanded = sourceDropdownExpanded,
                        onExpandedChange = { sourceDropdownExpanded = !sourceDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedSource.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recording Source") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RadiantGold,
                                focusedLabelColor = RadiantGold
                            ),
                            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sourceDropdownExpanded,
                            onDismissRequest = { sourceDropdownExpanded = false }
                        ) {
                            RecordingSource.values().forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source.name.replace("_", " ")) },
                                    onClick = {
                                        selectedSource = source
                                        sourceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Contact or Subject (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantGold,
                            focusedLabelColor = RadiantGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            engine.startRecording(
                                source = selectedSource,
                                contactName = contactName,
                                direction = CallDirection.NONE
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadiantGold,
                            contentColor = DarkGreen
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("start_recording_confirm_btn")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Recording", fontWeight = FontWeight.Bold)
                    }

                } else if (session.state == RecordingEngineState.SAVED) {
                    Text(
                        text = "Audio file validated and saved to private database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            engine.resetStateToIdle()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadiantGold,
                            contentColor = DarkGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                } else {
                    // Active Recording View
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(if (session.state == RecordingEngineState.RECORDING) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(if (session.state == RecordingEngineState.RECORDING) StatusError.copy(alpha = 0.2f) else PetroleumGreen.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (session.state == RecordingEngineState.RECORDING) StatusError else RadiantGold,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val minutes = session.durationSeconds / 60
                    val seconds = session.durationSeconds % 60
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.displayMedium,
                        color = RadiantGold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Waveform indicator based on amplitude
                    val ampNormalized = (session.currentAmplitude / 32767f).coerceIn(0.1f, 1f)
                    CallMasterWaveform(
                        progress = ampNormalized,
                        barCount = 32,
                        modifier = Modifier.height(40.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Controls: Pause/Resume, Stop, Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                engine.cancelRecording()
                                onDismiss()
                            }
                        ) {
                            Text("Discard", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        FilledIconButton(
                            onClick = {
                                if (session.state == RecordingEngineState.RECORDING) {
                                    engine.pauseRecording()
                                } else {
                                    engine.resumeRecording()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = PetroleumGreen,
                                contentColor = LightGold
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = if (session.state == RecordingEngineState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pause/Resume"
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                scope.launch {
                                    engine.stopRecording()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = StatusError,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(58.dp).testTag("stop_save_recording_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop and Save",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
