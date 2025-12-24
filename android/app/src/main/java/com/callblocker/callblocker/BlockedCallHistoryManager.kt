package com.callblocker.callblocker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BlockedCallHistoryManager {
    private const val PREFS_NAME = "CallBlockerPrefs"
    private const val KEY_HISTORY = "blocked_history"
    private const val MAX_HISTORY = 100

    fun addBlockedCall(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = prefs.getString(KEY_HISTORY, "[]")
        val historyArray = try {
            JSONArray(historyJson)
        } catch (e: Exception) {
            JSONArray()
        }

        val newEntry = JSONObject()
        newEntry.put("number", number)
        newEntry.put("timestamp", System.currentTimeMillis())

        val newArray = JSONArray()
        newArray.put(newEntry)

        for (i in 0 until historyArray.length()) {
            if (newArray.length() >= MAX_HISTORY) break
            newArray.put(historyArray.get(i))
        }

        prefs.edit().putString(KEY_HISTORY, newArray.toString()).apply()
    }

    fun getHistory(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HISTORY, "[]") ?: "[]"
    }
}