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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.CompletenessStatus
import com.example.data.RecordingDao
import com.example.data.RecordingEntity
import com.example.data.RecordingSource
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    recordingDao: RecordingDao,
    audioPlayer: CallMasterAudioPlayer,
    onNavigateToAiWithRecording: (RecordingEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val recordings by if (searchQuery.isBlank()) {
        when (selectedFilter) {
            "FAVORITES" -> recordingDao.getFavoriteRecordings().collectAsState(initial = emptyList())
            "CELLULAR" -> recordingDao.getRecordingsBySource(RecordingSource.CELLULAR).collectAsState(initial = emptyList())
            "WHATSAPP" -> recordingDao.getRecordingsBySource(RecordingSource.WHATSAPP).collectAsState(initial = emptyList())
            "TELEGRAM" -> recordingDao.getRecordingsBySource(RecordingSource.TELEGRAM).collectAsState(initial = emptyList())
            "MESSENGER" -> recordingDao.getRecordingsBySource(RecordingSource.MESSENGER).collectAsState(initial = emptyList())
            "SIGNAL" -> recordingDao.getRecordingsBySource(RecordingSource.SIGNAL).collectAsState(initial = emptyList())
            "MIC" -> recordingDao.getRecordingsBySource(RecordingSource.MIC).collectAsState(initial = emptyList())
            else -> recordingDao.getAllRecordings().collectAsState(initial = emptyList())
        }
    } else {
        recordingDao.searchRecordings(searchQuery).collectAsState(initial = emptyList())
    }

    var recordingToEditNotes by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var notesText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("recordings_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.nav_recordings),
            style = MaterialTheme.typography.headlineLarge,
            color = RadiantGold,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search recordings, contacts, notes, transcripts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RadiantGold) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RadiantGold,
                unfocusedBorderColor = PetroleumGreen
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("search_recordings_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        val filters = listOf(
            Pair("ALL", "All"),
            Pair("FAVORITES", "Favorites"),
            Pair("CELLULAR", "Cellular"),
            Pair("WHATSAPP", "WhatsApp"),
            Pair("TELEGRAM", "Telegram"),
            Pair("MESSENGER", "Messenger"),
            Pair("SIGNAL", "Signal"),
            Pair("MIC", "Voice Memo")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RadiantGold,
                        selectedLabelColor = DarkGreen,
                        containerColor = SurfaceDark,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recordings List
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = PetroleumGreen,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching recordings found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try clearing filters or search terms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                items(recordings, key = { it.id }) { item ->
                    RecordingFullItemCard(
                        recording = item,
                        onPlay = { audioPlayer.loadAndPlay(item) },
                        onToggleFavorite = {
                            scope.launch {
                                recordingDao.updateRecording(item.copy(isFavorite = !item.isFavorite))
                            }
                        },
                        onEditNotes = {
                            notesText = item.notes
                            recordingToEditNotes = item
                        },
                        onAiAnalyze = {
                            onNavigateToAiWithRecording(item)
                        },
                        onShare = {
                            val file = File(item.filePath)
                            if (file.exists()) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                                    putExtra(Intent.EXTRA_TEXT, "Recording: ${item.title}\n${item.notes}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
                            }
                        },
                        onDelete = {
                            recordingToDelete = item
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }

    // Edit Notes Dialog
    recordingToEditNotes?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToEditNotes = null },
            title = { Text("Recording Notes", color = RadiantGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes & Meeting Minutes") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            recordingDao.updateRecording(rec.copy(notes = notesText))
                            recordingToEditNotes = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text("Save Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToEditNotes = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    recordingToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Recording?", color = StatusError, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${rec.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val file = File(rec.filePath)
                            if (file.exists()) file.delete()
                            recordingDao.deleteRecording(rec)
                            recordingToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError, contentColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecordingFullItemCard(
    recording: RecordingEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNotes: () -> Unit,
    onAiAnalyze: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(recording.startedAt))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    FilledIconButton(
                        onClick = onPlay,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PetroleumGreen,
                            contentColor = RadiantGold
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (recording.contactName.isNotBlank()) {
                            Text(
                                text = "${recording.contactName} (${recording.sourceType.name})",
                                style = MaterialTheme.typography.bodySmall,
                                color = RadiantGold
                            )
                        } else {
                            Text(
                                text = recording.sourceType.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = RadiantGold
                            )
                        }
                        Text(
                            text = "$dateString • ${formatDuration(recording.durationMs)} • ${formatStorageSize(recording.fileSize)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (recording.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (recording.isFavorite) RadiantGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("AI Transcription & Summary") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RadiantGold) },
                                onClick = {
                                    menuExpanded = false
                                    onAiAnalyze()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Notes") },
                                leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditNotes()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share / Export") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = StatusError) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Bottom tags: Validation & Notes preview
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Validation tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (recording.validationStatus) {
                                CompletenessStatus.TWO_SIDED -> StatusSuccess.copy(alpha = 0.15f)
                                CompletenessStatus.AUTHORIZED_SYSTEM -> RadiantGold.copy(alpha = 0.15f)
                                CompletenessStatus.MIC_ONLY -> PetroleumGreen.copy(alpha = 0.4f)
                                CompletenessStatus.UNSUPPORTED -> StatusError.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (recording.validationStatus) {
                            CompletenessStatus.TWO_SIDED -> "Two-Sided Audio"
                            CompletenessStatus.AUTHORIZED_SYSTEM -> "System Authorized"
                            CompletenessStatus.MIC_ONLY -> "Microphone Only"
                            CompletenessStatus.UNSUPPORTED -> "Invalid Audio"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (recording.validationStatus) {
                            CompletenessStatus.TWO_SIDED -> StatusSuccess
                            CompletenessStatus.AUTHORIZED_SYSTEM -> RadiantGold
                            CompletenessStatus.MIC_ONLY -> LightGold
                            CompletenessStatus.UNSUPPORTED -> StatusError
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (recording.transcript != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RadiantGold.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AI Transcribed",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (recording.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = recording.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
