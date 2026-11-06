package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.preferences.CallMasterPreferences
import com.example.security.CallMasterSecurityManager
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGold
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceDark

@Composable
fun SettingsScreen(
    preferences: CallMasterPreferences,
    securityManager: CallMasterSecurityManager,
    onLanguageChange: (String) -> Unit
) {
    var isAutoRecord by remember { mutableStateOf(preferences.isAutoRecordEnabled) }
    var isQuietMode by remember { mutableStateOf(preferences.isQuietModeEnabled) }
    var currentLanguage by remember { mutableStateOf(preferences.appLanguage) }
    var currentTier by remember { mutableStateOf(preferences.subscriptionTier) }
    var isEncryptionEnabled by remember { mutableStateOf(preferences.isStorageEncryptionEnabled) }

    var showQuietDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

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

        // Subscription Tier Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth().clickable { showSubscriptionDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RadiantGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = RadiantGold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (currentTier == "PREMIUM") "CallMaster PRO (Active)" else "CallMaster Free Tier",
                                style = MaterialTheme.typography.titleSmall,
                                color = RadiantGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentTier == "PREMIUM") "Unlimited AI transcription & encrypted backup" else "Tap to upgrade for cloud backup & AI analysis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RadiantGold)
                }
            }
        }

        // Section: General
        item {
            SettingsSectionHeader(title = stringResource(R.string.section_general))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow(
                        title = stringResource(R.string.setting_language),
                        subtitle = if (currentLanguage == "ar") "العربية (Cairo Font)" else "English",
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
        }

        // Section: Recording Preferences
        item {
            SettingsSectionHeader(title = stringResource(R.string.section_recording))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_auto_record),
                        subtitle = "Companion auto-record when cellular call answers",
                        icon = Icons.Default.Mic,
                        checked = isAutoRecord,
                        onCheckedChange = {
                            isAutoRecord = it
                            preferences.isAutoRecordEnabled = it
                        }
                    )

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_quiet_mode),
                        subtitle = "Minimize audible clicks and redundant alerts",
                        icon = Icons.Default.Security,
                        checked = isQuietMode,
                        onCheckedChange = {
                            isQuietMode = it
                            preferences.isQuietModeEnabled = it
                            if (it) showQuietDialog = true
                        }
                    )
                }
            }
        }

        // Section: Security & Encryption
        item {
            SettingsSectionHeader(title = stringResource(R.string.section_security))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_encryption),
                        subtitle = "Hardware-backed Android Keystore AES-256",
                        icon = Icons.Default.Lock,
                        checked = isEncryptionEnabled,
                        onCheckedChange = {
                            isEncryptionEnabled = it
                            preferences.isStorageEncryptionEnabled = it
                        }
                    )

                    SettingsActionRow(
                        title = stringResource(R.string.setting_pin_lock),
                        subtitle = if (preferences.pinHash.isNotBlank()) "PIN Configured & Active" else "Not set",
                        icon = Icons.Default.Security,
                        onClick = { showPinDialog = true }
                    )
                }
            }
        }

        // Section: AI Assistant
        item {
            SettingsSectionHeader(title = "المساعد الذكي (AI Assistant)")
        }

        item {
            var showKeyInput by remember { mutableStateOf(false) }
            var currentKey by remember { mutableStateOf(preferences.geminiCustomApiKey) }

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
                                    text = if (currentKey.isNotBlank()) "المفتاح مفعل ونشط" else "اضغط لإدخال مفتاح Gemini API",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentKey.isNotBlank()) StatusSuccess else RadiantGold
                                )
                            }
                        }

                        Button(
                            onClick = { showKeyInput = !showKeyInput },
                            colors = ButtonDefaults.buttonColors(containerColor = PetroleumGreen, contentColor = LightGold),
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

        // Section: About & Privacy
        item {
            SettingsSectionHeader(title = stringResource(R.string.section_about))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CallMaster v1.0.0",
                        style = MaterialTheme.typography.titleSmall,
                        color = RadiantGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Native Android Call Recording Management & Intelligent Audio Platform.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Complies strictly with Google Play policies, Android microphone privacy guidelines, and transparent audio stream routing.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightGold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Quiet Mode Privacy Explanation Dialog (MANDATORY REQUIREMENT)
    if (showQuietDialog) {
        AlertDialog(
            onDismissRequest = { showQuietDialog = false },
            title = {
                Text("Quiet Recording Experience", color = RadiantGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "CallMaster Quiet Mode suppresses non-essential application sounds and redundant banners to keep your interface clean.\n\n" +
                            "IMPORTANT PRIVACY DISCLOSURE:\n" +
                            "• Android's operating system microphone indicator (green privacy dot) is a mandatory platform security safeguard and CANNOT and WILL NOT be hidden.\n" +
                            "• CallMaster never conceals recording activity from the device owner or enables covert surveillance.\n" +
                            "• Always notify and obtain lawful consent from all conversation participants.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showQuietDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text("I Understand & Agree")
                }
            }
        )
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configure PIN Lock", color = RadiantGold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a 4-digit security PIN to protect local recordings:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.isNotBlank()) {
                            preferences.pinHash = securityManager.hashPin(pinInput)
                            preferences.isPinLockEnabled = true
                        }
                        showPinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Subscription Comparison Dialog
    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { showSubscriptionDialog = false },
            title = { Text("CallMaster Subscription", color = RadiantGold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("FREE TIER:", fontWeight = FontWeight.Bold, color = LightGold)
                    Text("• Local recording library & audio playback\n• Device compatibility reports\n• Local Keystore AES-256 encryption")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("PREMIUM PRO TIER:", fontWeight = FontWeight.Bold, color = RadiantGold)
                    Text("• Full AI Audio Transcription (Arabic & English)\n• Executive summaries & Action Item extraction\n• Encrypted cloud backup integration\n• Bulk export & advanced search")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTier = if (currentTier == "FREE") "PREMIUM" else "FREE"
                        currentTier = newTier
                        preferences.subscriptionTier = newTier
                        showSubscriptionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantGold, contentColor = DarkGreen)
                ) {
                    Text(if (currentTier == "FREE") "Switch to PRO (Simulated)" else "Switch to FREE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubscriptionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = RadiantGold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RadiantGold, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkGreen,
                checkedTrackColor = RadiantGold
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RadiantGold, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
