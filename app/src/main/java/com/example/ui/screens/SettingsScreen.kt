package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
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
import com.example.preferences.CallMasterPreferences
import com.example.security.CallMasterSecurityManager
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.ElevatedGrayDark
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.PlatinumPrimary
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceDark
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
    val driveManager = remember { GoogleDriveBackupManager.getInstance(context) }
    val driveSyncState by driveManager.syncState.collectAsState()

    var isAutoRecord by remember { mutableStateOf(preferences.isAutoRecordEnabled) }
    var isQuietMode by remember { mutableStateOf(preferences.isQuietModeEnabled) }
    var currentLanguage by remember { mutableStateOf(preferences.appLanguage) }
    var currentTier by remember { mutableStateOf(preferences.subscriptionTier) }
    var isEncryptionEnabled by remember { mutableStateOf(preferences.isStorageEncryptionEnabled) }

    // Recording scope & direction
    var recordScope by remember { mutableStateOf(preferences.recordScope) }
    var recordDirection by remember { mutableStateOf(preferences.recordDirection) }
    var twoSidedBoost by remember { mutableStateOf(preferences.twoSidedAcousticBoost) }
    var audioBitrate by remember { mutableStateOf(preferences.audioBitrate) }

    // Google Drive
    var isDriveAutoSync by remember { mutableStateOf(preferences.isGoogleDriveAutoSyncEnabled) }
    var driveEmail by remember { mutableStateOf(preferences.googleDriveAccountEmail) }
    var showDriveEmailDialog by remember { mutableStateOf(false) }

    var showQuietDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    val defaultStoragePath = remember {
        val ext = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
        if (ext != null) File(ext, "CallMaster").absolutePath else File(context.filesDir, "recordings").absolutePath
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = RadiantGold,
                fontWeight = FontWeight.Bold
            )
        }

        // ==========================================
        // 1. Google Drive Automatic Cloud Backup Card
        // ==========================================
        item {
            SettingsSectionHeader(title = "المزامنة السحابية (Google Drive)")
            Spacer(modifier = Modifier.height(6.dp))

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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PetroleumGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = RadiantGold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Drive Backup",
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
                            Text(if (driveEmail.isNotBlank()) "تغيير الحساب" else "ربط Gmail")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-sync switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مزامنة تلقائية للمكالمات على Google Drive",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "إنشاء مجلد CallMaster وتخزين التسجيلات الصادرة والواردة تلقائياً فور انتهائها.",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Folder & Manual Sync Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { driveManager.syncAllPendingRecordings() },
                            enabled = !driveSyncState.isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadiantGold,
                                contentColor = DarkGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (driveSyncState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = DarkGreen,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الرفع (${driveSyncState.syncedFiles}/${driveSyncState.totalFiles})")
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مزامنة الكل الآن")
                            }
                        }

                        Button(
                            onClick = { driveManager.openDriveFolder(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PetroleumGreen,
                                contentColor = RadiantGold
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح المجلد")
                        }
                    }

                    if (driveSyncState.lastSyncTime > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val dateFmt = SimpleDateFormat("MMM d • hh:mm a", Locale.getDefault())
                        Text(
                            text = "آخر مزامنة ناجحة: ${dateFmt.format(Date(driveSyncState.lastSyncTime))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. Recording Scope & Source Settings
        // ==========================================
        item {
            SettingsSectionHeader(title = "نطاق واتجاه تسجيل المكالمات (Recording Scope)")
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نطاق التسجيل المدعوم:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = recordScope == "ALL",
                            onClick = {
                                recordScope = "ALL"
                                preferences.recordScope = "ALL"
                            },
                            label = { Text("جميع الاتصالات (كلاهما)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantGold,
                                selectedLabelColor = DarkGreen,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = recordScope == "CELLULAR_ONLY",
                            onClick = {
                                recordScope = "CELLULAR_ONLY"
                                preferences.recordScope = "CELLULAR_ONLY"
                            },
                            label = { Text("الشبكة فقط") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantGold,
                                selectedLabelColor = DarkGreen,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = recordScope == "VOIP_ONLY",
                            onClick = {
                                recordScope = "VOIP_ONLY"
                                preferences.recordScope = "VOIP_ONLY"
                            },
                            label = { Text("تطبيقات المراسلة") },
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

                    Text(
                        text = "اتجاه المكالمة المراد تسجيلها:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Two-sided voice clarity boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تسجيل صوت الطرفين (المتحدث والمستقبل)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تفعيل المعالجة الصوتية VOICE_COMMUNICATION لإلغاء الصدى والتقاط كلا الطرفين بأعلى نقاء ووضوح.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = twoSidedBoost,
                            onCheckedChange = {
                                twoSidedBoost = it
                                preferences.twoSidedAcousticBoost = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkGreen,
                                checkedTrackColor = RadiantGold,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. Dedicated Folder & Storage Management
        // ==========================================
        item {
            SettingsSectionHeader(title = "تخصيص مجلد التخزين وصيغة الصوت")
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = RadiantGold)
                        Spacer(modifier = Modifier.width(10.dp))
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
                }
            }
        }

        // ==========================================
        // 4. AI Transcription Configuration
        // ==========================================
        item {
            SettingsSectionHeader(title = "محرك التفريغ بالذكاء الاصطناعي (Gemini AI)")
            Spacer(modifier = Modifier.height(6.dp))

            var showKeyInput by remember { mutableStateOf(false) }
            var currentKey by remember { mutableStateOf(preferences.geminiCustomApiKey) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RadiantGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "مفتاح Gemini API للتفريغ الحقيقي",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (currentKey.isNotBlank()) "المفتاح مفعل ونشط" else "اضغط لإدخال أو تعديل المفتاح",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentKey.isNotBlank()) StatusSuccess else RadiantGold
                                )
                            }
                        }

                        Button(
                            onClick = { showKeyInput = !showKeyInput },
                            colors = ButtonDefaults.buttonColors(containerColor = PetroleumGreen, contentColor = RadiantGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (showKeyInput) "إغلاق" else "تعديل")
                        }
                    }

                    if (showKeyInput) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = currentKey,
                            onValueChange = {
                                currentKey = it
                                preferences.geminiCustomApiKey = it.trim()
                            },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // ==========================================
        // 5. General & Security
        // ==========================================
        item {
            SettingsSectionHeader(title = "التفضيلات العامة والأمان")
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrayDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
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

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_auto_record),
                        subtitle = "بدء التسجيل تلقائياً فور الرد على أي مكالمة",
                        icon = Icons.Default.Mic,
                        checked = isAutoRecord,
                        onCheckedChange = {
                            isAutoRecord = it
                            preferences.isAutoRecordEnabled = it
                        }
                    )

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_encryption),
                        subtitle = "تشفير AES-256 للتسجيلات في ذاكرة الجهاز",
                        icon = Icons.Default.Lock,
                        checked = isEncryptionEnabled,
                        onCheckedChange = {
                            isEncryptionEnabled = it
                            preferences.isStorageEncryptionEnabled = it
                        }
                    )

                    SettingsActionRow(
                        title = stringResource(R.string.setting_pin_lock),
                        subtitle = if (preferences.pinHash.isNotBlank()) "رمز PIN مفعل ونشط" else "غير معين",
                        icon = Icons.Default.Security,
                        onClick = { showPinDialog = true }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
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

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = RadiantGold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
