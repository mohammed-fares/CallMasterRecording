package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
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
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.CallDirection
import com.example.data.RecordingDao
import com.example.data.RecordingEntity
import com.example.data.RecordingSource
import com.example.player.CallMasterAudioPlayer
import com.example.ui.components.formatDuration
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceCallLogItem(
    val name: String,
    val number: String,
    val type: Int,
    val date: Long,
    val durationSeconds: Long
)

@Composable
fun CallHistoryScreen(
    recordingDao: RecordingDao,
    audioPlayer: CallMasterAudioPlayer,
    onStartRecordingClick: () -> Unit
) {
    val context = LocalContext.current
    val recordings by recordingDao.getAllRecordings().collectAsState(initial = emptyList())
    val callRecordings = recordings.filter { it.sourceType == RecordingSource.CELLULAR }

    var deviceCallLogs by remember { mutableStateOf<List<DeviceCallLogItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            deviceCallLogs = readDeviceCallLogs(context)
        }
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("call_history_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.nav_calls),
            style = MaterialTheme.typography.headlineLarge,
            color = RadiantGold,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Companion Mode Information Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PetroleumGreen.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = RadiantGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CallMaster Companion Dialer & Call Log",
                        style = MaterialTheme.typography.titleSmall,
                        color = RadiantGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "التسجيل التلقائي مفعل لجميع المكالمات الصادرة والواردة. عند استخدام مكبر الصوت (Speakerphone)، يلتقط الميكروفون طرفي المحادثة بوضوح فائق.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (callRecordings.isEmpty() && deviceCallLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = PetroleumGreen,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "جاهز لتسجيل المكالمات تلقائياً",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ستظهر أي مكالمة هاتفية صادرة أو واردة هنا تلقائياً فور انتهاء المكالمة.",
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
                // If there are recorded calls
                if (callRecordings.isNotEmpty()) {
                    item {
                        Text(
                            text = "المكالمات المسجلة (Recorded Calls)",
                            style = MaterialTheme.typography.titleSmall,
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(callRecordings, key = { it.id }) { rec ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth()
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
                                    val (dirIcon, dirColor) = when (rec.callDirection) {
                                        CallDirection.INCOMING -> Pair(Icons.AutoMirrored.Filled.CallReceived, StatusSuccess)
                                        CallDirection.OUTGOING -> Pair(Icons.AutoMirrored.Filled.CallMade, RadiantGold)
                                        CallDirection.NONE -> Pair(Icons.Default.Phone, PetroleumGreen)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(dirColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = dirIcon,
                                            contentDescription = null,
                                            tint = dirColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = if (rec.contactName.isNotBlank()) rec.contactName else rec.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (rec.contactNumber.isNotBlank()) {
                                            Text(
                                                text = rec.contactNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = RadiantGold
                                            )
                                        }
                                        Text(
                                            text = "${dateFormat.format(Date(rec.startedAt))} • ${formatDuration(rec.durationMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (rec.contactNumber.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${rec.contactNumber}"))
                                                context.startActivity(dialIntent)
                                            }
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Dial", tint = RadiantGold)
                                        }
                                    }

                                    FilledIconButton(
                                        onClick = { audioPlayer.loadAndPlay(rec) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = PetroleumGreen,
                                            contentColor = RadiantGold
                                        ),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play recording")
                                    }
                                }
                            }
                        }
                    }
                }

                // If device call log is available
                if (deviceCallLogs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "سجل مكالمات الهاتف (Device Call History)",
                            style = MaterialTheme.typography.titleSmall,
                            color = RadiantGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(deviceCallLogs) { logItem ->
                        val matchingRecording = callRecordings.find {
                            it.contactNumber.isNotBlank() && logItem.number.contains(it.contactNumber)
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth()
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
                                    val (icon, color) = when (logItem.type) {
                                        CallLog.Calls.INCOMING_TYPE -> Pair(Icons.AutoMirrored.Filled.CallReceived, StatusSuccess)
                                        CallLog.Calls.OUTGOING_TYPE -> Pair(Icons.AutoMirrored.Filled.CallMade, RadiantGold)
                                        CallLog.Calls.MISSED_TYPE -> Pair(Icons.AutoMirrored.Filled.CallMissed, StatusError)
                                        else -> Pair(Icons.Default.Phone, PetroleumGreen)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = if (logItem.name.isNotBlank()) logItem.name else logItem.number,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = logItem.number,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RadiantGold
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(logItem.date))} • ${formatDuration(logItem.durationSeconds * 1000)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${logItem.number}"))
                                            context.startActivity(dialIntent)
                                        }
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Dial", tint = RadiantGold)
                                    }

                                    if (matchingRecording != null) {
                                        FilledIconButton(
                                            onClick = { audioPlayer.loadAndPlay(matchingRecording) },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = PetroleumGreen,
                                                contentColor = RadiantGold
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play recording")
                                        }
                                    }
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
    }
}

private fun readDeviceCallLogs(context: Context): List<DeviceCallLogItem> {
    val logs = mutableListOf<DeviceCallLogItem>()
    try {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC LIMIT 30"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                val name = if (nameIdx != -1) it.getString(nameIdx) ?: "" else ""
                val number = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                val type = if (typeIdx != -1) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                val dur = if (durIdx != -1) it.getLong(durIdx) else 0L

                if (number.isNotBlank()) {
                    logs.add(DeviceCallLogItem(name, number, type, date, dur))
                }
            }
        }
    } catch (_: Exception) {}
    return logs
}
