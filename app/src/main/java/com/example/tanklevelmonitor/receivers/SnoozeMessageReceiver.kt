package com.example.tanklevelmonitor.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tanklevelmonitor.fragments.HomeFragment

class SnoozeMessageReceiver : BroadcastReceiver() {
    private val TAG = "SnoozeMessageReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: ${intent?.extras.toString()}")
        HomeFragment.snoozeMessageLiveData.postValue(true)
    }
}
