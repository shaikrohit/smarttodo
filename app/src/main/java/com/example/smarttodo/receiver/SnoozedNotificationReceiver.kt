package com.example.smarttodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.utils.NotificationActionReceiver
import com.example.smarttodo.utils.NotificationHelper
import com.example.smarttodo.utils.WakeLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enhanced receiver for handling snoozed task notifications when they're due to reappear
 * Now supports continuous vibration pattern until user takes action
 */
class SnoozedNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SnoozedNotifReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(NotificationActionReceiver.EXTRA_TASK_ID, -1)
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID received in snoozed notification")
            return
        }

        Log.d(TAG, "Received snoozed notification alarm for task $taskId")

        // Acquire wake lock to ensure we complete processing
        val wakeLock = WakeLockManager.acquireWakeLock(context, "snoozed:$taskId")

        try {
            processSnoozedNotification(context, taskId)
        } finally {
            WakeLockManager.releaseWakeLock(wakeLock)
        }
    }

    private fun processSnoozedNotification(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskDao = TaskDatabase.getDatabase(context).taskDao()
                val task = taskDao.getTaskById(taskId)

                if (task != null && !task.isCompleted) {
                    // Switch to the main thread to show the notification
                    withContext(Dispatchers.Main) {
                        val notificationHelper = NotificationHelper(context)
                        // Use the enhanced notification with continuous vibration
                        notificationHelper.showTaskReminder(task, isPreReminder = false)
                        Log.d(TAG, "Showing snoozed notification for task: ${task.title}")
                    }
                } else if (task == null) {
                    Log.e(TAG, "Task with ID $taskId not found in database")
                } else {
                    Log.d(TAG, "Task $taskId is already completed, not showing notification")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing snoozed notification", e)
            }
        }
    }
}
