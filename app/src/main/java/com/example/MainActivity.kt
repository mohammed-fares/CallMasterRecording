package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ai.CallMasterAiEngine
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.AppDatabase
import com.example.player.CallMasterAudioPlayer
import com.example.preferences.CallMasterPreferences
import com.example.recording.CallMasterRecordingEngine
import com.example.security.CallMasterSecurityManager
import com.example.ui.CallMasterMainScreen
import com.example.ui.theme.CallMasterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(this)
        val recordingEngine = CallMasterRecordingEngine.getInstance(this)
        val compatibilityEngine = DeviceCompatibilityEngine(this)
        val audioPlayer = CallMasterAudioPlayer.getInstance(this)
        val preferences = CallMasterPreferences(this)
        val securityManager = CallMasterSecurityManager()
        val aiEngine = CallMasterAiEngine(database, preferences)

        setContent {
            CallMasterTheme(darkTheme = preferences.isDarkTheme) {
                CallMasterMainScreen(
                    database = database,
                    recordingEngine = recordingEngine,
                    compatibilityEngine = compatibilityEngine,
                    audioPlayer = audioPlayer,
                    preferences = preferences,
                    securityManager = securityManager,
                    aiEngine = aiEngine
                )
            }
        }
    }
}
