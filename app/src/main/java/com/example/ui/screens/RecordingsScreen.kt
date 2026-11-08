package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.CallDirection
import com.example.data.RecordingDao
import com.example.data.RecordingEntity
import com.example.data.RecordingSource
import com.example.player.CallMasterAudioPlayer
import com.example.ui.components.formatDuration
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.PlatinumPrimary
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceDark
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ContactGroup(
    val key: String,
    val displayName: String,
    val phoneNumber: String,
    val recordings: List<RecordingEntity>,
    val incomingCount: Int,
    val outgoingCount: Int,
    val totalDurationMs: Long,
    val latestTimestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    recordingDao: RecordingDao,
    audioPlayer: CallMasterAudioPlayer,
    onNavigateToAiWithRecording: (RecordingEntity) -> Unit,
    targetRecordingId: String? = null,
    onClearTargetRecordingId: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var viewMode by remember { mutableStateOf(0) } // 0 = All list, 1 = Grouped by Contact
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

    // Auto-focus and auto-play when triggered from post-call notification
    LaunchedEffect(targetRecordingId, recordings) {
        if (!targetRecordingId.isNullOrBlank() && recordings.isNotEmpty()) {
            val matching = recordings.firstOrNull { it.id == targetRecordingId }
            if (matching != null) {
                audioPlayer.loadAndPlay(matching)
                onClearTargetRecordingId()
            }
        }
    }

    // Compute automatic contact grouping and categorization
    val contactGroups = remember(recordings) {
        recordings.groupBy { rec ->
            rec.contactNumber.ifBlank { rec.contactName.ifBlank { "مكالمة غير محددة" } }
        }.map { (key, list) ->
            val displayName = list.firstOrNull { it.contactName.isNotBlank() }?.contactName
                ?: list.firstOrNull { it.contactNumber.isNotBlank() }?.contactNumber
                ?: "مكالمة هاتفية"
            val number = list.firstOrNull { it.contactNumber.isNotBlank() }?.contactNumber ?: ""
            val incoming = list.count { it.callDirection == CallDirection.INCOMING }
            val outgoing = list.count { it.callDirection == CallDirection.OUTGOING }
            val totalDuration = list.sumOf { it.durationMs }
            val latest = list.maxOfOrNull { it.startedAt } ?: 0L
            ContactGroup(
                key = key,
                displayName = displayName,
                phoneNumber = number,
                recordings = list.sortedByDescending { it.startedAt },
                incomingCount = incoming,
                outgoingCount = outgoing,
                totalDurationMs = totalDuration,
                latestTimestamp = latest
            )
        }.sortedByDescending { it.latestTimestamp }
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

        Spacer(modifier = Modifier.height(10.dp))

        // View Mode Switcher: All Recordings vs Grouped By Contact
        TabRow(
            selectedTabIndex = viewMode,
            containerColor = SurfaceDark,
            contentColor = RadiantGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[viewMode]),
                    color = RadiantGold
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = viewMode == 0,
                onClick = { viewMode = 0 },
                text = {
                    Text(
                        "قائمة التسجيلات (${recordings.size})",
                        fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = viewMode == 1,
                onClick = { viewMode = 1 },
                text = {
                    Text(
                        "حسب الأشخاص والأرقام (${contactGroups.size})",
                        fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في المكالمات، الأسماء، الملاحظات...") },
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

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row (Shown in All Recordings view)
        if (viewMode == 0) {
            val filters = listOf(
                Pair("ALL", "الكل"),
                Pair("FAVORITES", "المفضلة"),
                Pair("CELLULAR", "مكالمات خلوية"),
                Pair("WHATSAPP", "واتساب"),
                Pair("TELEGRAM", "تيليجرام"),
                Pair("MESSENGER", "ماسنجر"),
                Pair("SIGNAL", "سيجنال"),
                Pair("MIC", "ملاحظة صوتية")
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
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Content Area
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
                        text = "لا توجد تسجيلات مطابقة",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ستظهر التسجيلات تلقائياً بمجرد إجراء أو استقبال أي مكالمة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (viewMode == 1) {
            // Grouped By Contact / Number View
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                items(contactGroups, key = { it.key }) { group ->
                    ContactGroupCard(
                        group = group,
                        onPlay = { audioPlayer.loadAndPlay(it) },
                        onNavigateToAi = { onNavigateToAiWithRecording(it) },
                        onShare = { shareRecordingAudio(context, it) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        } else {
            // All Flat Chronological List View
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
                        onShare = { shareRecordingAudio(context, item) },
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
            title = { Text("ملاحظات المكالمة", color = RadiantGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("أدخل ملخصك الخاص أو ملاحظات المحادثة") },
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
                    Text("حفظ الملاحظة")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToEditNotes = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    recordingToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("حذف التسجيل", color = RadiantGold, fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف هذا التسجيل؟ سيتم حذف الملف الصوتي نهائياً من ذاكرة الجهاز.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val f = File(rec.filePath)
                            if (f.exists()) f.delete()
                            recordingDao.deleteRecording(rec)
                            recordingToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ContactGroupCard(
    group: ContactGroup,
    onPlay: (RecordingEntity) -> Unit,
    onNavigateToAi: (RecordingEntity) -> Unit,
    onShare: (RecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PetroleumGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = group.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (group.phoneNumber.isNotBlank() && group.phoneNumber != group.displayName) {
                            Text(
                                text = group.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = RadiantGold
                            )
                        }
                        Text(
                            text = "${group.recordings.size} تسجيلات • ${group.incomingCount} وارد • ${group.outgoingCount} صادر • ${formatDuration(group.totalDurationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (group.phoneNumber.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${group.phoneNumber}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Dial",
                                tint = StatusSuccess
                            )
                        }
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    group.recordings.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FilledIconButton(
                                        onClick = { onPlay(rec) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = PetroleumGreen,
                                            contentColor = RadiantGold
                                        ),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        val dateFmt = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
                                        Text(
                                            text = dateFmt.format(Date(rec.startedAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val dirText = when (rec.callDirection) {
                                            CallDirection.INCOMING -> "مكالمة واردة"
                                            CallDirection.OUTGOING -> "مكالمة صادرة"
                                            CallDirection.NONE -> rec.sourceType.name
                                        }
                                        Text(
                                            text = "$dirText • ${formatDuration(rec.durationMs)} • ${formatStorageSize(rec.fileSize)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onNavigateToAi(rec) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "AI",
                                            tint = RadiantGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onShare(rec) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
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
                        val dirText = when (recording.callDirection) {
                            CallDirection.INCOMING -> "واردة"
                            CallDirection.OUTGOING -> "صادرة"
                            CallDirection.NONE -> ""
                        }
                        val dirLabel = if (dirText.isNotBlank()) "$dirText • " else ""
                        Text(
                            text = "$dirLabel$dateString • ${formatDuration(recording.durationMs)} • ${formatStorageSize(recording.fileSize)}",
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
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تفريغ بالذكاء الاصطناعي (AI Analysis)") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RadiantGold) },
                                onClick = {
                                    menuExpanded = false
                                    onAiAnalyze()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تعديل الملاحظات") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditNotes()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("مشاركة / تصدير الصوت") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف التسجيل", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Notes preview if present
            if (recording.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📝 ${recording.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // AI transcript preview if present
            if (!recording.transcript.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PetroleumGreen.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RadiantGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مفرغة بالذكاء الاصطناعي: ${recording.transcript.take(75)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun shareRecordingAudio(context: android.content.Context, item: RecordingEntity) {
    val file = File(item.filePath)
    if (!file.exists()) return

    val isEncrypted = item.encryptionStatus.contains("AES", ignoreCase = true) || file.name.endsWith(".enc", ignoreCase = true)
    val shareFile: File
    if (isEncrypted) {
        val sec = com.example.security.CallMasterSecurityManager()
        val tempShare = File(context.cacheDir, "share_${item.id}.m4a")
        val ok = sec.decryptFile(file, tempShare)
        shareFile = if (ok && tempShare.exists() && tempShare.length() > 0L) tempShare else file
    } else {
        shareFile = file
    }

    val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
    } catch (_: Exception) {
        null
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        putExtra(Intent.EXTRA_TEXT, "CallMaster Recording: ${item.title}")
        if (uri != null) {
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Audio Recording"))
}
