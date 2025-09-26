package com.example.smarttodo.utils

import android.content.Context
import android.os.PowerManager
import android.util.Log

object WakeLockManager {
    private const val TAG = "WakeLockManager"

    fun acquireWakeLock(context: Context, tag: String): PowerManager.WakeLock? {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "smarttodo::$tag")
            wakeLock.acquire(60 * 1000L /* 1 minute timeout */)
            Log.d(TAG, "Acquired wake lock with tag: $tag")
            wakeLock
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException acquiring wake lock with tag: $tag - continuing without wake lock", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock with tag: $tag", e)
            null
        }
    }

    fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock.release()
                Log.d(TAG, "Released wake lock")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing wake lock", e)
            }
        }
    }
}
