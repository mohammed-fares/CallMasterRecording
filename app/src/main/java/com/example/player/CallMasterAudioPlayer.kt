package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.example.data.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerState(
    val currentRecording: RecordingEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null
)

class CallMasterAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun loadAndPlay(recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            _playerState.update {
                it.copy(
                    currentRecording = recording,
                    isPlaying = false,
                    errorMessage = "Audio file not found on device storage."
                )
            }
            return
        }

        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _playerState.update { it.copy(isPlaying = false, currentPositionMs = duration) }
                    stopProgressTracking()
                }
                start()
            }

            val duration = mediaPlayer?.duration ?: recording.durationMs.toInt()
            _playerState.update {
                it.copy(
                    currentRecording = recording,
                    isPlaying = true,
                    durationMs = duration,
                    currentPositionMs = 0,
                    errorMessage = null
                )
            }
            startProgressTracking()

        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error starting playback", e)
            _playerState.update {
                it.copy(
                    currentRecording = recording,
                    isPlaying = false,
                    errorMessage = "Playback error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playerState.update { it.copy(isPlaying = false) }
            stopProgressTracking()
        } else {
            player.start()
            _playerState.update { it.copy(isPlaying = true) }
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipForward(seconds: Int = 10) {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition + seconds * 1000).coerceAtMost(player.duration)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 10) {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition - seconds * 1000).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun setSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                    _playerState.update { it.copy(playbackSpeed = speed) }
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error setting speed", e)
            }
        }
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playerState.update { it.copy(isPlaying = false) }
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val pos = mediaPlayer?.currentPosition ?: 0
                _playerState.update { it.copy(currentPositionMs = pos) }
                delay(250)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    companion object {
        @Volatile
        private var INSTANCE: CallMasterAudioPlayer? = null

        fun getInstance(context: Context): CallMasterAudioPlayer {
            return INSTANCE ?: synchronized(this) {
                val instance = CallMasterAudioPlayer(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
