package com.example.smarttodo.utils

import android.content.Context
import android.os.PowerManager
import android.util.Log

object WakeLockManager {

    private const val TAG = "WakeLockManager"

    fun acquireWakeLock(context: Context, tag: String): PowerManager.WakeLock? {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartTodo::$tag")
            wakeLock.setReferenceCounted(false)
            wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */)
            Log.d(TAG, "WakeLock acquired with tag: $tag")
            wakeLock
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wakeLock with tag: $tag", e)
            null
        }
    }

    fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock.release()
                Log.d(TAG, "WakeLock released")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release wakeLock", e)
            }
        }
    }
}

