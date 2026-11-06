package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
    aiEngine: CallMasterAiEngine
) {
    var hasCompletedOnboarding by remember { mutableStateOf(preferences.hasCompletedOnboarding) }
    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var showActiveRecordingDialog by remember { mutableStateOf(false) }
    var showCompatibilityDetail by remember { mutableStateOf(false) }
    var preselectedAiRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var appLanguage by remember { mutableStateOf(preferences.appLanguage) }

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
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
