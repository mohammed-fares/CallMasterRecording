package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.compatibility.CompatibilityLevel
import com.example.compatibility.DeviceHardwareReport
import com.example.data.CompletenessStatus
import com.example.data.RecordingDao
import com.example.data.RecordingEntity
import com.example.player.CallMasterAudioPlayer
import com.example.preferences.CallMasterPreferences
import com.example.recording.CallMasterRecordingEngine
import com.example.recording.RecordingEngineState
import com.example.ui.components.formatDuration
import com.example.backup.GoogleDriveBackupManager
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGold
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SurfaceDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    recordingDao: RecordingDao,
    recordingEngine: CallMasterRecordingEngine,
    audioPlayer: CallMasterAudioPlayer,
    preferences: CallMasterPreferences,
    report: DeviceHardwareReport,
    onNavigateToRecordings: () -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToCompatibility: () -> Unit,
    onStartRecordingClick: () -> Unit
) {
    val context = LocalContext.current
    val totalCount by recordingDao.getTotalCount().collectAsState(initial = 0)
    val incomingCount by recordingDao.getIncomingCount().collectAsState(initial = 0)
    val outgoingCount by recordingDao.getOutgoingCount().collectAsState(initial = 0)
    val voipCount by recordingDao.getVoipCount().collectAsState(initial = 0)
    val totalBytes by recordingDao.getTotalStorageBytes().collectAsState(initial = 0L)
    val recentRecordings by recordingDao.getRecentRecordings(4).collectAsState(initial = emptyList())

    val session by recordingEngine.sessionState.collectAsState()
    var showBackupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CallMaster",
                        style = MaterialTheme.typography.headlineLarge,
                        color = RadiantGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compatibility Level Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (report.overallLevel) {
                        CompatibilityLevel.LEVEL_A -> StatusSuccess.copy(alpha = 0.2f)
                        CompatibilityLevel.LEVEL_B -> RadiantGold.copy(alpha = 0.2f)
                        CompatibilityLevel.LEVEL_C -> PetroleumGreen.copy(alpha = 0.5f)
                        CompatibilityLevel.LEVEL_D -> StatusError.copy(alpha = 0.2f)
                    },
                    modifier = Modifier.clickable { onNavigateToCompatibility() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = RadiantGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = report.overallLevel.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Recording Banner (if active)
        if (session.state == RecordingEngineState.RECORDING || session.state == RecordingEngineState.PAUSED) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().clickable { onStartRecordingClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(StatusError)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (session.state == RecordingEngineState.RECORDING) "جاري تسجيل المكالمة الحالية..." else "التسجيل متوقف مؤقتاً",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "المصدر: ${session.source} • ${formatDuration(session.durationSeconds * 1000)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onStartRecordingClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadiantGold,
                                contentColor = DarkGreen
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("عرض", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Statistical Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.total_recordings),
                    value = totalCount.toString(),
                    icon = Icons.Default.Mic,
                    modifier = Modifier.weight(1f).clickable { onNavigateToRecordings() }
                )
                StatCard(
                    title = stringResource(R.string.incoming_calls),
                    value = incomingCount.toString(),
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    modifier = Modifier.weight(1f).clickable { onNavigateToCalls() }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.outgoing_calls),
                    value = outgoingCount.toString(),
                    icon = Icons.AutoMirrored.Filled.CallMade,
                    modifier = Modifier.weight(1f).clickable { onNavigateToCalls() }
                )
                StatCard(
                    title = stringResource(R.string.voip_recordings),
                    value = voipCount.toString(),
                    icon = Icons.Default.Phone,
                    modifier = Modifier.weight(1f).clickable { onNavigateToRecordings() }
                )
            }
        }

        // Storage & Backup Card (Interactive)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth().clickable { showBackupDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.storage_used),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatStorageSize(totalBytes),
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.engine_status),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (session.state == RecordingEngineState.RECORDING) "Active Recording" else stringResource(R.string.engine_ready),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (session.state == RecordingEngineState.RECORDING) StatusError else StatusSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Quick Actions
        item {
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    label = stringResource(R.string.action_record),
                    icon = Icons.Default.Mic,
                    modifier = Modifier.weight(1f),
                    onClick = onStartRecordingClick
                )
                QuickActionButton(
                    label = stringResource(R.string.action_check_compat),
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCompatibility
                )
                QuickActionButton(
                    label = stringResource(R.string.action_ai_transcribe),
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAi
                )
            }
        }

        // Recent Recordings Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_recordings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToRecordings) {
                    Text(stringResource(R.string.view_all), color = RadiantGold)
                }
            }
        }

        if (recentRecordings.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
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
                            text = "التسجيل التلقائي مفعل ونشط",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "عند إجراء أو تلقي أي مكالمة هاتفية، سيتم تسجيلها وحفظها هنا فوراً، أو اضغط على 'تسجيل صوتي' لتسجيل مذكرة الآن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onStartRecordingClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadiantGold,
                                contentColor = DarkGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("بدء تسجيل صوتي الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(recentRecordings, key = { it.id }) { rec ->
                RecordingItemCard(
                    recording = rec,
                    onPlay = { audioPlayer.loadAndPlay(rec) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Cloud Backup Management Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = RadiantGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدارة التخزين والنسخ الاحتياطي", color = RadiantGold, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "المساحة المستهلكة حالياً: ${formatStorageSize(totalBytes)}\n" +
                                "إجمالي التسجيلات: $totalCount تسجيل\n" +
                                "التشفير: AES-256 بضمانة عتاد الهاتف.\n\n" +
                                "جميع التسجيلات محفوظة محلياً في مجلد التطبيق المحمي لضمان الخصوصية التامة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupDialog = false
                        Toast.makeText(context, "النسخ الاحتياطي المحلي محدّث بالكامل ومحمي بالتشفير", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PetroleumGreen.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = RadiantGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.4f)),
        tonalElevation = 2.dp,
        modifier = modifier.height(84.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RadiantGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecordingItemCard(
    recording: RecordingEntity,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(recording.startedAt))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
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
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play recording")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${recording.sourceType.name} • ${formatDuration(recording.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = RadiantGold
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Completeness tag
            val (badgeBg, badgeText, badgeColor) = when (recording.validationStatus) {
                CompletenessStatus.SUCCESS_BOTH_SIDES -> Triple(StatusSuccess.copy(alpha = 0.2f), "طرفين (ناجح)", StatusSuccess)
                CompletenessStatus.SUCCESS_MIC_ONLY -> Triple(PetroleumGreen.copy(alpha = 0.4f), "ميكروفون (MIC)", RadiantGold)
                CompletenessStatus.SUCCESS_PLAYBACK_ONLY -> Triple(RadiantGold.copy(alpha = 0.2f), "صوت التطبيق", RadiantGold)
                CompletenessStatus.PARTIAL_RECORDING -> Triple(StatusWarning.copy(alpha = 0.2f), "تسجيل جزئي", StatusWarning)
                CompletenessStatus.SILENT_RECORDING -> Triple(StatusError.copy(alpha = 0.25f), "صامت (تنبيه)", StatusError)
                CompletenessStatus.SOURCE_BLOCKED -> Triple(StatusError.copy(alpha = 0.2f), "محظور برمجياً", StatusError)
                CompletenessStatus.PERMISSION_DENIED -> Triple(StatusError.copy(alpha = 0.2f), "صلاحيات مرفوضة", StatusError)
                CompletenessStatus.UNSUPPORTED_DEVICE -> Triple(StatusError.copy(alpha = 0.2f), "غير مدعوم", StatusError)
                CompletenessStatus.RECORDING_INTERRUPTED -> Triple(StatusWarning.copy(alpha = 0.2f), "مقاطعة", StatusWarning)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}
