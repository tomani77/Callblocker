package com.callblocker.callblocker

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.getHandle().toString()
        Log.d("CallScreeningService", "Incoming call from: $phoneNumber")
        val formattedPhoneNumber = phoneNumber.removePrefix("tel:")
        Log.d("CallScreeningService", "Formatted number: $formattedPhoneNumber")


        val sharedPreferences = getSharedPreferences("BlocklistPrefs", MODE_PRIVATE)
        val blocklist = sharedPreferences.getStringSet("blocklist", emptySet()) ?: emptySet()
        Log.d("CallScreeningService", "Blocklist: $blocklist")


        var isBlocked = false
        for (blockedNumber in blocklist) {
            Log.d("CallScreeningService", "Checking against: $blockedNumber")
            if (areNumbersMatching(formattedPhoneNumber, blockedNumber)) {
                isBlocked = true
                Log.d("CallScreeningService", "Number is in blocklist. Blocking.")
                break
            }
        }

        val response = if (isBlocked) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
        } else {
            Log.d("CallScreeningService", "Number is not in blocklist. Allowing.")
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
        }
        respondToCall(callDetails, response)
    }

    private fun areNumbersMatching(incomingNumber: String, blockedNumber: String): Boolean {
        if (blockedNumber.endsWith("*")) {
            val prefix = blockedNumber.dropLast(1)
            return incomingNumber.startsWith(prefix)
        }

        val normalizedIncoming = incomingNumber.filter { it.isDigit() }
        val normalizedBlocked = blockedNumber.filter { it.isDigit() }
        Log.d("CallScreeningService", "Normalized incoming: $normalizedIncoming, normalized blocked: $normalizedBlocked")


        return normalizedIncoming.endsWith(normalizedBlocked)
    }
}
