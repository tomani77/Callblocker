package com.callblocker.callblocker

import android.content.Context
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class CallScreeningServiceImpl : CallScreeningService() {

    companion object {
        private const val TAG = "CallScreeningService"
        init {
            Log.e(TAG, "� CLASS LOADED INTO MEMORY - CallScreeningServiceImpl")
        }
    }

    private fun writeLog(message: String) {
        Log.d(TAG, message)
        Log.i(TAG, message)
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "📞 SERVICE CREATED - Service is ready to screen calls")
        writeLog("onCreate() called - CallScreeningService is binding")
    }

    init {
        Log.e(TAG, "📞 INSTANCE CREATED - init block executed")
    }

    override fun onDestroy() {
        super.onDestroy()
        writeLog("onDestroy() called - Service is unbinding")
    }

    override fun onScreenCall(callDetails: Call.Details) {
        Log.e(TAG, "📞 onScreenCall INVOKED - Processing incoming call 📞")
        writeLog("=== onScreenCall invoked ===")
        
        val handle = callDetails.handle
        val formattedPhoneNumber = if (handle != null) Uri.decode(handle.schemeSpecificPart) else "UNKNOWN"
        writeLog("Incoming call from: $formattedPhoneNumber")

        // Check if the number is in blocklist
        val isBlocked = isNumberBlocked(formattedPhoneNumber)

        if (isBlocked) {
            writeLog("🚫 BLOCKING CALL: $formattedPhoneNumber matches blocklist")
            BlockedCallHistoryManager.addBlockedCall(this, formattedPhoneNumber)
            
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
            
            try {
                respondToCall(callDetails, response)
                Log.e(TAG, "✓ BLOCKED: $formattedPhoneNumber - Call rejected successfully")
                writeLog("=== onScreenCall completed - CALL BLOCKED ===")
            } catch (e: Exception) {
                Log.e(TAG, "✗ ERROR in respondToCall(): ${e.message}", e)
            }
        } else {
            writeLog("✓ ALLOWING: $formattedPhoneNumber (not in blocklist)")
            // Don't respond - let the call through
            writeLog("=== onScreenCall completed - CALL ALLOWED ===")
        }
    }

    private fun isNumberBlocked(incomingNumber: String): Boolean {
        val sharedPreferences = getSharedPreferences("CallBlockerPrefs", Context.MODE_PRIVATE)
        val blocklist = sharedPreferences.getStringSet("blocklist", emptySet()) ?: emptySet()

        if (blocklist.isEmpty()) {
            writeLog("Blocklist is empty - allowing call")
            return false
        }

        for (blockedNumber in blocklist) {
            if (areNumbersMatching(incomingNumber, blockedNumber)) {
                writeLog("  Blocklist match found: '$incomingNumber' matches '$blockedNumber'")
                return true
            }
        }
        return false
    }

    private fun areNumbersMatching(incomingNumber: String, blockedNumber: String): Boolean {
        val normalizedIncoming = incomingNumber.filter { it.isDigit() }

        // Handle wildcard patterns
        if (blockedNumber.endsWith("*")) {
            val prefix = blockedNumber.dropLast(1).filter { it.isDigit() }
            val matches = normalizedIncoming.startsWith(prefix)
            writeLog("  Wildcard check: '$normalizedIncoming' starts with '$prefix' = $matches")
            return matches
        }

        // Handle exact or suffix match
        val normalizedBlocked = blockedNumber.filter { it.isDigit() }
        val matches = normalizedIncoming.endsWith(normalizedBlocked)
        writeLog("  Exact/suffix check: '$normalizedIncoming' ends with '$normalizedBlocked' = $matches")
        return matches
    }
}
