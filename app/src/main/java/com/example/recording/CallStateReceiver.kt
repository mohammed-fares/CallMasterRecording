package com.example.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.CallDirection
import com.example.data.RecordingSource
import com.example.preferences.CallMasterPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        val prefs = CallMasterPreferences(context)

        Log.d("CallStateReceiver", "Phone state changed: $stateStr, number: $incomingNumber")

        val engine = CallMasterRecordingEngine.getInstance(context)

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Incoming call ringing
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered or outgoing call started
                if (prefs.isAutoRecordEnabled && engine.checkPermissions()) {
                    engine.startRecording(
                        source = RecordingSource.CELLULAR,
                        contactName = if (incomingNumber.isNotBlank()) incomingNumber else "Cellular Call",
                        contactNumber = incomingNumber,
                        direction = CallDirection.INCOMING
                    )
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended
                if (engine.sessionState.value.state == RecordingEngineState.RECORDING &&
                    engine.sessionState.value.source == RecordingSource.CELLULAR.name
                ) {
                    CoroutineScope(Dispatchers.Default).launch {
                        engine.stopRecording()
                    }
                }
            }
        }
    }
}
