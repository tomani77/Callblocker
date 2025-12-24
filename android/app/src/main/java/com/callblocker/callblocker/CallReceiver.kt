package com.callblocker.callblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, CallScreeningService is used instead
            Log.d(TAG, "Android 10+ detected - CallScreeningService will handle this")
            return
        }

        if (intent.action == "android.intent.action.PHONE_STATE") {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (incomingNumber != null) {
                    Log.d(TAG, "Incoming call from: $incomingNumber")
                    if (isNumberBlocked(context, incomingNumber)) {
                        Log.d(TAG, "BLOCKING call from: $incomingNumber")
                        BlockedCallHistoryManager.addBlockedCall(context, incomingNumber)
                        endCall(context)
                    } else {
                        Log.d(TAG, "ALLOWING call from: $incomingNumber")
                    }
                }
            }
        }
    }

    private fun isNumberBlocked(context: Context, incomingNumber: String): Boolean {
        val sharedPreferences = context.getSharedPreferences("CallBlockerPrefs", Context.MODE_PRIVATE)
        val blocklist = sharedPreferences.getStringSet("blocklist", emptySet()) ?: emptySet()

        if (blocklist.isEmpty()) {
            Log.d(TAG, "Blocklist is empty - allowing call")
            return false
        }

        for (blockedNumber in blocklist) {
            if (areNumbersMatching(incomingNumber, blockedNumber)) {
                Log.d(TAG, "Blocklist match: '$incomingNumber' matches '$blockedNumber'")
                return true
            }
        }

        Log.d(TAG, "No blocklist match found - allowing call")
        return false
    }

    private fun areNumbersMatching(incomingNumber: String, blockedNumber: String): Boolean {
        if (blockedNumber.endsWith("*")) {
            val prefix = blockedNumber.dropLast(1)
            return incomingNumber.startsWith(prefix)
        }

        val normalizedIncoming = incomingNumber.filter { it.isDigit() }
        val normalizedBlocked = blockedNumber.filter { it.isDigit() }

        return normalizedIncoming.endsWith(normalizedBlocked)
    }

    private fun endCall(context: Context) {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val getITelephony = telephonyManager.javaClass.getDeclaredMethod("getITelephony")
            getITelephony.isAccessible = true
            val telephonyService = getITelephony.invoke(telephonyManager)

            telephonyService?.let {
                val endCall = it.javaClass.getDeclaredMethod("endCall")
                endCall.invoke(it)
                Log.d(TAG, "Call ended successfully")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error ending call: ${t.message}", t)
        }
    }
}
