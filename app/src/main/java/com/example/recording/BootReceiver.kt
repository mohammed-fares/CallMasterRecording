package com.example.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.preferences.CallMasterPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = CallMasterPreferences(context)
            Log.d("CallMasterBoot", "Device reboot completed. Auto-recording state: ${prefs.isAutoRecordEnabled}")
        }
    }
}
