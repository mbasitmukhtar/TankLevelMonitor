package com.example.tanklevelmonitor.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.example.tanklevelmonitor.utils.Constants.PREFERENCES_APP_STATE
import com.example.tanklevelmonitor.utils.Constants.PREFERENCES_FIRST_TIME
import com.example.tanklevelmonitor.utils.Constants.PREFERENCES_SETTINGS
import com.example.tanklevelmonitor.utils.Constants.PREFERENCES_USER_INFO

class SharedPrefs {
    companion object {
        private const val TAG = "SharedPrefs"

        private lateinit var prefs: SharedPreferences
        private lateinit var spEditor: SharedPreferences.Editor

        fun storeAppState(context: Context, currentActivityName: String?, swapMode: String?) {
            Log.d(TAG, "storeAppState: ")
            prefs = context.getSharedPreferences(PREFERENCES_APP_STATE, Context.MODE_PRIVATE)
            spEditor = prefs.edit()
            spEditor.putString("currentActivity", currentActivityName)
            spEditor.putString("swapMode", swapMode)
            spEditor.apply()
        }

        fun restoreAppState(context: Context): Bundle {
            Log.d(TAG, "restoreAppState: ")
            prefs = context.getSharedPreferences(PREFERENCES_APP_STATE, Context.MODE_PRIVATE)
            val bundle = Bundle()
            if (prefs.getString("currentActivity", null) != null) {
                bundle.putString("currentActivity", prefs.getString("currentActivity", null))
            }
            if (prefs.getString("swapMode", null) != null) {
                bundle.putString("swapMode", prefs.getString("swapMode", null))
            }
            return bundle
        }

        fun getUserData(context: Context, key: String): String {
            Log.d(TAG, "getUserData: ")
            prefs = context.getSharedPreferences(PREFERENCES_USER_INFO, Context.MODE_PRIVATE)
            val response = prefs.getString(key, "").toString()
            return response
        }

        fun storeUserData(context: Context, key: String?, value: String?) {
            Log.d(TAG, "storeUserData: ")
            prefs = context.getSharedPreferences(PREFERENCES_USER_INFO, Context.MODE_PRIVATE)
            spEditor = prefs.edit()
            spEditor.putString(key, value)
            spEditor.apply()
        }

        fun storeFirstTimeFlag(context: Context, key: String, value: Boolean) {
            Log.d(TAG, "storeFirstTimeFlag: ")
            prefs = context.getSharedPreferences(PREFERENCES_FIRST_TIME, Context.MODE_PRIVATE)
            spEditor = prefs.edit()
            spEditor.putBoolean(key, value)
            spEditor.apply()
        }

        fun getFirstTimeFlag(context: Context, key: String): Boolean {
            Log.d(TAG, "getFirstTimeFlag: ")
            prefs = context.getSharedPreferences(PREFERENCES_FIRST_TIME, Context.MODE_PRIVATE)
            return prefs.getBoolean(key, true)
        }

        fun clearSharedPrefs(context: Context) {
            Log.d(TAG, "clearSharedPrefs: ")
            prefs = context.getSharedPreferences(PREFERENCES_USER_INFO, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            prefs = context.getSharedPreferences(PREFERENCES_APP_STATE, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            prefs = context.getSharedPreferences(PREFERENCES_SETTINGS, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
//            prefs = context.getSharedPreferences(PREFERENCES_FIRST_TIME, Context.MODE_PRIVATE)
//            prefs.edit().clear().apply()
        }

    }
}
