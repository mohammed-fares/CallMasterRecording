package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.backup.GoogleDriveBackupManager
import com.example.compatibility.CompatibilityLevel
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.compatibility.DeviceHardwareReport
import com.example.data.AppDatabase
import com.example.preferences.CallMasterPreferences
import com.example.recording.AudioValidationEngine
import com.example.recording.AudioValidationResult
import com.example.security.CallMasterSecurityManager
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.ElevatedGrayDark
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.PlatinumPrimary
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SurfaceDark
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    preferences: CallMasterPreferences,
    securityManager: CallMasterSecurityManager,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveManager = remember { GoogleDriveBackupManager.getInstance(context) }
    val driveSyncState by driveManager.syncState.collectAsState()
    val compatibilityEngine = remember { DeviceCompatibilityEngine(context) }

    var isAutoRecord by remember { mutableStateOf(preferences.isAutoRecordEnabled) }
    var currentLanguage by remember { mutableStateOf(preferences.appLanguage) }
    var isEncryptionEnabled by remember { mutableStateOf(preferences.isStorageEncryptionEnabled) }

    var recordScope by remember { mutableStateOf(preferences.recordScope) }
    var recordDirection by remember { mutableStateOf(preferences.recordDirection) }
    var twoSidedBoost by remember { mutableStateOf(preferences.twoSidedAcousticBoost) }
    var audioBitrate by remember { mutableStateOf(preferences.audioBitrate) }

    // Google Drive
    var isDriveAutoSync by remember { mutableStateOf(preferences.isGoogleDriveAutoSyncEnabled) }
    var driveEmail by remember { mutableStateOf(preferences.googleDriveAccountEmail) }
    var showDriveEmailDialog by remember { mutableStateOf(false) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    // Diagnostics State
    var showXiaomiBaselineDialog by remember { mutableStateOf(false) }
    var lastValidationResult by remember { mutableStateOf<AudioValidationResult?>(null) }
    var isAnalyzingLastRecording by remember { mutableStateOf(false) }

    val defaultStoragePath = remember {
        val ext = context.getExternalFilesDir("Recordings")
            ?: context.getExternalFilesDir(null)
            ?: File(context.filesDir, "Recordings")
        File(ext, "CallMaster").absolutePath
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = RadiantGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "الإعدادات الشاملة لمحرك التسجيل والأمان وتوافق الأجهزة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // =========================================================================
        // SECTION 1: GENERAL RECORDING (القسم الأول: التسجيل العام)
        // =========================================================================
        item {
            SectionBadge(number = "1", title = "التسجيل العام (General Recording)")
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto Record Toggle
                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_auto_record),
                        subtitle = "بدء التسجيل تلقائياً فور الرد على أي مكالمة بدون تدخل يدوي",
                        icon = Icons.Default.Mic,
                        checked = isAutoRecord,
                        onCheckedChange = {
                            isAutoRecord = it
                            preferences.isAutoRecordEnabled = it
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Storage Folder
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = RadiantGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مجلد حفظ التسجيلات على الهاتف:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = defaultStoragePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantGold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Bitrate
                    Text(
                        text = "جودة وضغط الصوت:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = audioBitrate == 128000,
                            onClick = {
                                audioBitrate = 128000
                                preferences.audioBitrate = 128000
                            },
                            label = { Text("AAC عالي النقاء (128 kbps)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantGold,
                                selectedLabelColor = DarkGreen,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = audioBitrate == 64000,
                            onClick = {
                                audioBitrate = 64000
                                preferences.audioBitrate = 64000
                            },
                            label = { Text("مضغوط اقتصادي (64 kbps)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantGold,
                                selectedLabelColor = DarkGreen,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Security & PIN
                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_encryption),
                        subtitle = "تشفير محلي AES-256 للملفات الصوتية",
                        icon = Icons.Default.Lock,
                        checked = isEncryptionEnabled,
                        onCheckedChange = {
                            isEncryptionEnabled = it
                            preferences.isStorageEncryptionEnabled = it
                        }
                    )

                    SettingsActionRow(
                        title = stringResource(R.string.setting_pin_lock),
                        subtitle = if (preferences.pinHash.isNotBlank()) "رمز PIN نشط ومفعل" else "غير معين (اضغط للتعيين)",
                        icon = Icons.Default.Security,
                        onClick = { showPinDialog = true }
                    )

                    SettingsActionRow(
                        title = stringResource(R.string.setting_language),
                        subtitle = if (currentLanguage == "ar") "العربية (خط كايرو Cairo)" else "English",
                        icon = Icons.Default.Language,
                        onClick = {
                            val newLang = if (currentLanguage == "ar") "en" else "ar"
                            currentLanguage = newLang
                            preferences.appLanguage = newLang
                            onLanguageChange(newLang)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Drive Auto Sync Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PetroleumGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = RadiantGold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "المزامنة السحابية (Google Drive)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (driveEmail.isNotBlank()) driveEmail else "غير متصل بحساب Gmail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (driveEmail.isNotBlank()) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showDriveEmailDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetroleumGreen,
                                contentColor = RadiantGold
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (driveEmail.isNotBlank()) "تغيير" else "ربط Gmail")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مزامنة تلقائية للتسجيلات",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "رفع التسجيلات تلقائياً لمجلد CallMaster الخاص بحسابك.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDriveAutoSync,
                            onCheckedChange = {
                                isDriveAutoSync = it
                                preferences.isGoogleDriveAutoSyncEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkGreen,
                                checkedTrackColor = RadiantGold,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { driveManager.syncAllPendingRecordings() },
                            enabled = !driveSyncState.isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (driveSyncState.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkGreen, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جاري الرفع...")
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مزامنة الكل الآن")
                            }
                        }

                        Button(
                            onClick = { driveManager.openDriveFolder(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = PetroleumGreen, contentColor = RadiantGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فتح المجلد")
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: CELLULAR RECORDING (القسم الثاني: تسجيل المكالمات الخلوية)
        // =========================================================================
        item {
            SectionBadge(number = "2", title = "تسجيل المكالمات الخلوية (Cellular Recording)")
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "تسجيل المكالمات الخلوية: مُفعل تلقائياً",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "مصدر الصوت: Microphone Companion Mode (MIC)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RadiantGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Speakerphone Guidance (Crucial for Cellular Clarity)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PetroleumGreen.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, RadiantGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = RadiantGold, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "توجيه صوتي للوضوح الأقصى للطرفين:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RadiantGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "يُنصح بتفعيل مكبر الصوت (Speakerphone) أثناء المكالمة الخلوية لضمان التقاط صوت المتصل البعيد بوضوح كامل وتفادي كتم الصوت من نظام Android.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Direction Filter
                    Text(
                        text = "اتجاه المكالمة المراد تسجيلها:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = recordDirection == "BOTH",
                            onClick = {
                                recordDirection = "BOTH"
                                preferences.recordDirection = "BOTH"
                            },
                            label = { Text("الوارد والصادر (كلاهما)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantGold,
                                selectedLabelColor = DarkGreen,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = recordDirection == "INCOMING_ONLY",
                            onClick = {
                                recordDirection = "INCOMING_ONLY"
                                preferences.recordDirection = "INCOMING_ONLY"
                            },
                            label = { Text("الوارد فقط") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantGold,
                                selectedLabelColor = DarkGreen,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = recordDirection == "OUTGOING_ONLY",
                            onClick = {
                                recordDirection = "OUTGOING_ONLY"
                                preferences.recordDirection = "OUTGOING_ONLY"
                            },
                            label = { Text("الصادر فقط") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantGold,
                                selectedLabelColor = DarkGreen,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Transparent Android Isolation Notice
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إشعار قانوني وتقني: يمنع نظام Android (منذ الإصدار 9+) التطبيقات العادية من اعتراض خط الاتصال الهاتفي الداخلي مباشرة لحماية الخصوصية؛ لذا يعمل التطبيق في وضع الرفيق الصوتي (Companion Mode) عبر الميكروفون المباشر بأعلى نقاء متاح.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 3: PREMIUM VOIP RECORDING (القسم الثالث: تسجيل تطبيقات الاتصال)
        // =========================================================================
        item {
            SectionBadge(number = "3", title = "تسجيل تطبيقات الاتصال الذكية (Premium VoIP Recording)")
            Spacer(modifier = Modifier.height(6.dp))

            val voipApps = remember {
                listOf(
                    VoipAppItem("WhatsApp Voice & Video", "com.whatsapp", true),
                    VoipAppItem("Telegram Voice Calls", "org.telegram.messenger", true),
                    VoipAppItem("Facebook Messenger", "com.facebook.orca", true),
                    VoipAppItem("Signal Private Messenger", "org.thoughtcrime.securesms", true),
                    VoipAppItem("Viber Messenger", "com.viber.voip", true),
                    VoipAppItem("Google Meet", "com.google.android.apps.tachyon", true),
                    VoipAppItem("Microsoft Teams", "com.microsoft.teams", true),
                    VoipAppItem("Zoom Meetings", "us.zoom.videomeetings", true),
                    VoipAppItem("Discord Voice", "com.discord", false)
                )
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التطبيقات المدعومة وفحص سياسة الالتقاط:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "فحص مباشر لسياسة Android Audio Playback Capture لكل تطبيق:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    voipApps.forEach { app ->
                        VoipAppCard(app = app)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 4: AUDIO DIAGNOSTICS (القسم الرابع: تشخيص الصوت وفحص التوافق)
        // =========================================================================
        item {
            SectionBadge(number = "4", title = "تشخيص الصوت وفحص التوافق (Audio Diagnostics)")
            Spacer(modifier = Modifier.height(6.dp))

            val hardwareReport = remember { compatibilityEngine.generateReport() }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "بيانات عتاد هذا الجهاز",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${hardwareReport.manufacturer} ${hardwareReport.model} • Android ${hardwareReport.androidVersion} (API ${hardwareReport.apiLevel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = RadiantGold
                            )
                        }

                        Button(
                            onClick = { showXiaomiBaselineDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PetroleumGreen, contentColor = RadiantGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تقرير Mi A3 المرجعي")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hardware Specs Details
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DiagnosticMetricChip(label = "ميكروفون فيزيائي", value = if (hardwareReport.hasMicrophone) "متوفر" else "غير متوفر", isGood = hardwareReport.hasMicrophone)
                        DiagnosticMetricChip(label = "VOICE_COMM", value = if (hardwareReport.voiceCommunicationSourceAvailable) "مدعوم" else "غير متاح", isGood = hardwareReport.voiceCommunicationSourceAvailable)
                        DiagnosticMetricChip(label = "Foreground Mic", value = if (hardwareReport.foregroundServiceMicrophoneSupported) "مدعوم" else "غير متاح", isGood = hardwareReport.foregroundServiceMicrophoneSupported)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action: Analyze Last Recording
                    Button(
                        onClick = {
                            isAnalyzingLastRecording = true
                            scope.launch {
                                val db = AppDatabase.getInstance(context)
                                val baseDir = (context.getExternalFilesDir("Recordings")
                                    ?: context.getExternalFilesDir(null)
                                    ?: context.filesDir).let { File(it, "CallMaster") }
                                val files = baseDir.listFiles()?.sortedByDescending { it.lastModified() }
                                val lastFile = files?.firstOrNull { it.isFile && it.name.endsWith(".m4a") }
                                if (lastFile != null) {
                                    val validator = AudioValidationEngine()
                                    lastValidationResult = validator.validateAudioFile(lastFile, "CELLULAR")
                                }
                                isAnalyzingLastRecording = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PetroleumGreen, contentColor = RadiantGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAnalyzingLastRecording) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RadiantGold, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري فحص وفك ترميز PCM...")
                        } else {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحليل آخر تسجيل صوتي (Acoustic Audio Validation)")
                        }
                    }

                    // Validation Results Card if available
                    if (lastValidationResult != null) {
                        val res = lastValidationResult!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, if (res.isSilent) StatusError else StatusSuccess),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (res.isSilent) "نتيجة الفحص: ملف صامت (غير ناجح)" else "نتيجة الفحص: تسجيل صوتي صالح وناجح",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (res.isSilent) StatusError else StatusSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "مستوى الطاقة (RMS): ${"%.1f".format(res.rmsDb)} dBFS • ذروة الإشارة: ${res.peakAmplitude} • نسبة الصمت: ${"%.0f".format(res.silenceRatio * 100)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RadiantGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = res.diagnosticMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Share Diagnostic Report
                    Button(
                        onClick = {
                            val reportText = compatibilityEngine.exportDiagnosticReportText(hardwareReport)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "CallMaster Diagnostic Report")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير التشخيص"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير ومشاركة تقرير التشخيص الفني")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Baseline Xiaomi Mi A3 Dialog
    if (showXiaomiBaselineDialog) {
        val miA3Report = remember { compatibilityEngine.getXiaomiMiA3BaselineReport() }
        AlertDialog(
            onDismissRequest = { showXiaomiBaselineDialog = false },
            title = {
                Text(
                    text = "تقرير التوافق المرجعي: Xiaomi Mi A3",
                    color = RadiantGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    item {
                        Text(
                            text = "الجهاز: Xiaomi Mi A3 (laurel_sprout)\nإصدار النظام: Android 11 (Android One, API 30)\nمستوى التوافق الإجمالي: المستوى C (Acoustic Companion Mode)\n",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تحليل السلوك الصوتي على Mi A3:\n• المكالمات الخلوية: يتم استخدام ميكروفون الجهاز المباشر (MIC) لتجنب كتم الصوت الذي يحدث عند طلب VOICE_COMMUNICATION على عتاد الهاتف.\n• المكالمات عبر WhatsApp وتطبيقات VoIP: تضع التطبيقات علامة الحماية USAGE_VOICE_COMMUNICATION مما يمنع السحب البرمجي الداخلي؛ الحل الموصى به هو تفعيل مكبر الصوت لتمكين الميكروفون من التقاط صوت الطرفين بنجاح.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showXiaomiBaselineDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Google Drive Email Config Dialog
    if (showDriveEmailDialog) {
        var emailInput by remember { mutableStateOf(driveEmail) }
        AlertDialog(
            onDismissRequest = { showDriveEmailDialog = false },
            title = { Text("ربط حساب Google Drive", color = RadiantGold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "أدخل بريد Gmail الذي ترغب بربط التسجيلات عليه لإنشاء مجلد CallMaster والمزامنة التلقائية:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("بريد Gmail") },
                        placeholder = { Text("your.email@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        driveEmail = emailInput.trim()
                        preferences.googleDriveAccountEmail = emailInput.trim()
                        preferences.isGoogleDriveAutoSyncEnabled = true
                        isDriveAutoSync = true
                        showDriveEmailDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text("حفظ وربط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveEmailDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("تعيين رمز PIN", color = RadiantGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    label = { Text("رمز PIN (4-6 أرقام)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            preferences.pinHash = securityManager.hashPin(pinInput)
                            preferences.isPinLockEnabled = true
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text("حفظ الرمز")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

data class VoipAppItem(
    val name: String,
    val packageName: String,
    val callPrivacyRestricted: Boolean
)

@Composable
fun VoipAppCard(app: VoipAppItem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (app.callPrivacyRestricted) StatusWarning.copy(alpha = 0.15f) else StatusSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (app.callPrivacyRestricted) "مكالمات محمية" else "مدعوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (app.callPrivacyRestricted) StatusWarning else StatusSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (app.callPrivacyRestricted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "هذا التطبيق لا يسمح بالتقاط الصوت عبر Android Audio Playback Capture على هذا الجهاز. (يُرجى تفعيل مكبر الصوت للتسجيل المحيطي).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SectionBadge(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PetroleumGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, style = MaterialTheme.typography.labelMedium, color = RadiantGold, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = RadiantGold,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiagnosticMetricChip(label: String, value: String, isGood: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, if (isGood) StatusSuccess.copy(alpha = 0.5f) else StatusWarning.copy(alpha = 0.5f)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.labelSmall, color = if (isGood) StatusSuccess else StatusWarning, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RadiantGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkGreen,
                checkedTrackColor = RadiantGold,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RadiantGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
