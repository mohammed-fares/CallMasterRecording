package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.CallMasterAiEngine
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.AppDatabase
import com.example.data.RecordingEntity
import com.example.player.CallMasterAudioPlayer
import com.example.preferences.CallMasterPreferences
import com.example.recording.CallMasterRecordingEngine
import com.example.security.CallMasterSecurityManager
import com.example.ui.components.CallMasterActiveRecordingDialog
import com.example.ui.components.CallMasterPlayerBar
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.CallHistoryScreen
import com.example.ui.screens.CompatibilityScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGold
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SurfaceDark

enum class MainNavTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    RECORDINGS(R.string.nav_recordings, Icons.Filled.Folder, Icons.Outlined.Folder),
    CALLS(R.string.nav_calls, Icons.Filled.Phone, Icons.Outlined.Phone),
    AI(R.string.nav_ai, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun CallMasterMainScreen(
    database: AppDatabase,
    recordingEngine: CallMasterRecordingEngine,
    compatibilityEngine: DeviceCompatibilityEngine,
    audioPlayer: CallMasterAudioPlayer,
    preferences: CallMasterPreferences,
    securityManager: CallMasterSecurityManager,
    aiEngine: CallMasterAiEngine,
    targetRecordingId: String? = null,
    onClearTargetRecordingId: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasCompletedOnboarding by remember { mutableStateOf(preferences.hasCompletedOnboarding) }
    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var showActiveRecordingDialog by remember { mutableStateOf(false) }
    var showCompatibilityDetail by remember { mutableStateOf(false) }
    var preselectedAiRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var appLanguage by remember { mutableStateOf(preferences.appLanguage) }

    // Switch to recordings tab immediately when opening from a post-call notification
    LaunchedEffect(targetRecordingId) {
        if (!targetRecordingId.isNullOrBlank()) {
            currentTab = MainNavTab.RECORDINGS
        }
    }

    var hasAllPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasAllPermissions = result[Manifest.permission.RECORD_AUDIO] == true &&
            result[Manifest.permission.READ_PHONE_STATE] == true
    }

    // Proactively launch permission requests right upon entering the app
    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            val perms = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    val layoutDirection = if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        if (!hasCompletedOnboarding) {
            OnboardingScreen(
                preferences = preferences,
                compatibilityEngine = compatibilityEngine,
                onFinish = {
                    hasCompletedOnboarding = true
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .background(SurfaceDark)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Persistent Audio Player Bar when loaded
                        CallMasterPlayerBar(player = audioPlayer)

                        NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("callmaster_bottom_nav")
                        ) {
                            MainNavTab.values().forEach { tab ->
                                val selected = currentTab == tab && !showCompatibilityDetail
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        showCompatibilityDetail = false
                                        currentTab = tab
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.titleRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.titleRes),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DarkGreen,
                                        selectedTextColor = RadiantGold,
                                        indicatorColor = RadiantGold,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val isWide = maxWidth > 680.dp
                    val hMargin = if (isWide) 24.dp else 0.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = hMargin)
                            .widthIn(max = 760.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        // Permission Banner if missing
                        if (!hasAllPermissions) {
                            Surface(
                                color = RadiantGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = RadiantGold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "تفعيل أذونات تسجيل المكالمات",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = RadiantGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "مطلوبة للتسجيل التلقائي فور الرد وربط أسماء جهات الاتصال.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val perms = mutableListOf(
                                                Manifest.permission.RECORD_AUDIO,
                                                Manifest.permission.READ_PHONE_STATE,
                                                Manifest.permission.READ_CALL_LOG,
                                                Manifest.permission.READ_CONTACTS
                                            )
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            permissionLauncher.launch(perms.toTypedArray())
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RadiantGold,
                                            contentColor = DarkGreen
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("تفعيل الآن")
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (showCompatibilityDetail) {
                                CompatibilityScreen(compatibilityEngine = compatibilityEngine)
                            } else {
                                Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
                                    when (tab) {
                                        MainNavTab.HOME -> HomeScreen(
                                            recordingDao = database.recordingDao(),
                                            recordingEngine = recordingEngine,
                                            audioPlayer = audioPlayer,
                                            preferences = preferences,
                                            report = compatibilityEngine.generateReport(),
                                            onNavigateToRecordings = { currentTab = MainNavTab.RECORDINGS },
                                            onNavigateToCalls = { currentTab = MainNavTab.CALLS },
                                            onNavigateToAi = { currentTab = MainNavTab.AI },
                                            onNavigateToCompatibility = { showCompatibilityDetail = true },
                                            onStartRecordingClick = { showActiveRecordingDialog = true }
                                        )
                                        MainNavTab.RECORDINGS -> RecordingsScreen(
                                            recordingDao = database.recordingDao(),
                                            audioPlayer = audioPlayer,
                                            targetRecordingId = targetRecordingId,
                                            onClearTargetRecordingId = onClearTargetRecordingId,
                                            onNavigateToAiWithRecording = { rec ->
                                                preselectedAiRecording = rec
                                                currentTab = MainNavTab.AI
                                            }
                                        )
                                        MainNavTab.CALLS -> CallHistoryScreen(
                                            recordingDao = database.recordingDao(),
                                            audioPlayer = audioPlayer,
                                            onStartRecordingClick = { showActiveRecordingDialog = true }
                                        )
                                        MainNavTab.AI -> AiAssistantScreen(
                                            aiEngine = aiEngine,
                                            recordingDao = database.recordingDao(),
                                            audioPlayer = audioPlayer,
                                            preSelectedRecording = preselectedAiRecording
                                        )
                                        MainNavTab.SETTINGS -> SettingsScreen(
                                            preferences = preferences,
                                            securityManager = securityManager,
                                            onLanguageChange = { newLang ->
                                                appLanguage = newLang
                                            }
                                        )
                                    }
                                }
                            }

                            if (showActiveRecordingDialog) {
                                CallMasterActiveRecordingDialog(
                                    engine = recordingEngine,
                                    onDismiss = { showActiveRecordingDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
