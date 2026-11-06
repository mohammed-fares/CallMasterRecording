package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.player.CallMasterAudioPlayer
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGold
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SurfaceDark

@Composable
fun CallMasterPlayerBar(
    player: CallMasterAudioPlayer,
    modifier: Modifier = Modifier
) {
    val state by player.playerState.collectAsState()
    val recording = state.currentRecording ?: return

    var showFullDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { showFullDialog = true }
            .testTag("audio_player_bar"),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.6f)),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = RadiantGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (recording.contactName.isNotBlank()) recording.contactName else recording.sourceType.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { player.toggleSpeakerphone() },
                        modifier = Modifier.size(34.dp).testTag("player_speaker_mini")
                    ) {
                        Icon(
                            imageVector = if (state.isSpeakerphoneOn) Icons.Default.VolumeUp else Icons.Default.Hearing,
                            contentDescription = if (state.isSpeakerphoneOn) "مكبر الصوت" else "سماعة المكالمة",
                            tint = if (state.isSpeakerphoneOn) RadiantGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.skipBackward(10) },
                        modifier = Modifier.size(34.dp).testTag("player_skip_back_mini")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Skip -10s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = { player.togglePlayPause() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RadiantGold,
                            contentColor = DarkGreen
                        ),
                        modifier = Modifier.size(38.dp).testTag("player_play_pause_mini")
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.skipForward(10) },
                        modifier = Modifier.size(34.dp).testTag("player_skip_fwd_mini")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Skip +10s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.close() },
                        modifier = Modifier.size(32.dp).testTag("player_close_mini")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close player",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = state.currentPositionMs.toFloat(),
                onValueChange = { player.seekTo(it.toInt()) },
                valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = RadiantGold,
                    activeTrackColor = RadiantGold,
                    inactiveTrackColor = PetroleumGreen
                ),
                modifier = Modifier.fillMaxWidth().height(20.dp).testTag("player_mini_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(state.currentPositionMs.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (state.isSpeakerphoneOn) "🔊 مكبر الصوت" else "👂 سماعة المكالمة",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.isSpeakerphoneOn) RadiantGold else LightGold,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDuration(state.durationMs.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showFullDialog) {
        CallMasterPlayerDetailDialog(
            player = player,
            onDismiss = { showFullDialog = false }
        )
    }
}

@Composable
fun CallMasterPlayerDetailDialog(
    player: CallMasterAudioPlayer,
    onDismiss: () -> Unit
) {
    val state by player.playerState.collectAsState()
    val recording = state.currentRecording ?: run {
        onDismiss()
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("player_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.6f))
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PetroleumGreen)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = recording.sourceType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = LightGold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RadiantGold.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = recording.validationStatus.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                if (recording.contactName.isNotBlank()) {
                    Text(
                        text = recording.contactName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RadiantGold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val progress = if (state.durationMs > 0) {
                    (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                CallMasterWaveform(
                    progress = progress,
                    barCount = 36,
                    modifier = Modifier.height(40.dp),
                    seed = recording.id.hashCode()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = state.currentPositionMs.toFloat(),
                    onValueChange = { player.seekTo(it.toInt()) },
                    valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = RadiantGold,
                        activeTrackColor = RadiantGold,
                        inactiveTrackColor = PetroleumGreen
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("player_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(state.currentPositionMs.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(state.durationMs.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val nextSpeed = when (state.playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            player.setSpeed(nextSpeed)
                        }
                    ) {
                        Text(
                            text = "${state.playbackSpeed}x",
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { player.skipBackward(10) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Skip -10s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = { player.togglePlayPause() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RadiantGold,
                            contentColor = DarkGreen
                        ),
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.skipForward(10) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Skip +10s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(36.dp))
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)
}
