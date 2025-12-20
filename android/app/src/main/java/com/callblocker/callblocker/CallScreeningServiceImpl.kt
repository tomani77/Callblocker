package com.callblocker.callblocker

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.getHandle().toString()
        val formattedPhoneNumber = phoneNumber.removePrefix("tel:")

        val sharedPreferences = getSharedPreferences("BlocklistPrefs", MODE_PRIVATE)
        val blocklist = sharedPreferences.getStringSet("blocklist", emptySet()) ?: emptySet()

        var isBlocked = false
        for (blockedNumber in blocklist) {
            if (blockedNumber.endsWith("*")) {
                if (formattedPhoneNumber.startsWith(blockedNumber.substring(0, blockedNumber.length - 1))) {
                    isBlocked = true
                    break
                }
            } else if (blockedNumber == formattedPhoneNumber) {
                isBlocked = true
                break
            }
        }

        val response = if (isBlocked) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
        }
        respondToCall(callDetails, response)
    }
}
