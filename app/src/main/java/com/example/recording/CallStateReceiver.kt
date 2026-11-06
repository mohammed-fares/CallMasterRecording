package com.example.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.CallDirection
import com.example.data.RecordingSource
import com.example.preferences.CallMasterPreferences

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val rawNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        val prefs = CallMasterPreferences(context)

        Log.d("CallStateReceiver", "Phone state changed: $stateStr, number: $rawNumber")

        val engine = CallMasterRecordingEngine.getInstance(context)

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Incoming call ringing
                isIncomingCall = true
                if (rawNumber.isNotBlank()) {
                    savedNumber = rawNumber
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered or outgoing call started
                if (prefs.isAutoRecordEnabled && engine.checkPermissions()) {
                    // Check recording scope
                    if (prefs.recordScope == "VOIP_ONLY") {
                        return
                    }

                    val direction = if (isIncomingCall) CallDirection.INCOMING else CallDirection.OUTGOING

                    // Check recording direction
                    if (prefs.recordDirection == "INCOMING_ONLY" && direction != CallDirection.INCOMING) {
                        return
                    }
                    if (prefs.recordDirection == "OUTGOING_ONLY" && direction != CallDirection.OUTGOING) {
                        return
                    }

                    val numberToUse = if (savedNumber.isNotBlank()) savedNumber else rawNumber
                    val contactName = resolveContactName(context, numberToUse)

                    CallMasterRecordingService.start(
                        context = context,
                        source = RecordingSource.CELLULAR,
                        contactName = contactName,
                        contactNumber = numberToUse,
                        direction = direction
                    )
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended
                CallMasterRecordingService.stop(context)
                isIncomingCall = false
                savedNumber = ""
            }
        }
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String {
        if (phoneNumber.isBlank()) return "Cellular Call"
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return it.getString(nameIndex)
                    }
                }
            }
            phoneNumber
        } catch (_: Exception) {
            phoneNumber
        }
    }

    companion object {
        private var isIncomingCall = false
        private var savedNumber = ""
    }
}
