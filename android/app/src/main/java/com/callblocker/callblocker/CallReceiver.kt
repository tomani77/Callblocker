package com.callblocker.callblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return // On Android 10+, CallScreeningService is used.
        }

        if (intent.action == "android.intent.action.PHONE_STATE") {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (incomingNumber != null) {
                    Log.d("CallReceiver", "Incoming call from: $incomingNumber")
                    if (isBlocked(context, incomingNumber)) {
                        endCall(context)
                    }
                }
            }
        }
    }

    private fun isBlocked(context: Context, incomingNumber: String): Boolean {
        val sharedPreferences = context.getSharedPreferences("BlocklistPrefs", Context.MODE_PRIVATE)
        val blocklist = sharedPreferences.getStringSet("blocklist", emptySet()) ?: emptySet()

        for (blockedNumber in blocklist) {
            if (blockedNumber.endsWith("*")) {
                if (incomingNumber.startsWith(blockedNumber.substring(0, blockedNumber.length - 1))) {
                    return true
                }
            } else if (blockedNumber == incomingNumber) {
                return true
            }
        }
        return false
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
            }

            Log.d("CallReceiver", "Call ended")
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
