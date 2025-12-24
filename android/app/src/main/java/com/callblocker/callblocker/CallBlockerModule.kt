package com.callblocker.callblocker

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import org.json.JSONArray

class CallBlockerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "CallBlockerModule"
    }

    @ReactMethod
    fun setBlocklist(blocklist: ReadableArray, promise: Promise) {
        val set = mutableSetOf<String>()
        for (i in 0 until blocklist.size()) {
            blocklist.getString(i)?.let { set.add(it) }
        }

        val sharedPreferences = reactApplicationContext.getSharedPreferences("CallBlockerPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("blocklist", set)
            apply()
        }
        promise.resolve(null)
    }

    @ReactMethod
    fun requestRole(promise: Promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = reactApplicationContext.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                reactApplicationContext.currentActivity?.startActivityForResult(intent, 123, null)
            }
            promise.resolve(true)
        } else {
            promise.resolve(false)
        }
    }

    @ReactMethod
    fun getBlockedHistory(promise: Promise) {
        try {
            val historyJson = BlockedCallHistoryManager.getHistory(reactApplicationContext)
            val jsonArray = JSONArray(historyJson)
            val writableArray = Arguments.createArray()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val map = Arguments.createMap()
                map.putString("number", jsonObject.getString("number"))
                map.putDouble("timestamp", jsonObject.getLong("timestamp").toDouble())
                writableArray.pushMap(map)
            }
            promise.resolve(writableArray)
        } catch (e: Exception) {
            promise.reject("ERROR_GETTING_HISTORY", e)
        }
    }
}
