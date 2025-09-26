package com.example.smarttodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri // For String?.toUri()
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.utils.NotificationHelper
import com.example.smarttodo.utils.SnoozeScheduler
import com.example.smarttodo.utils.WakeLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_TASK_REMINDER = "com.example.smarttodo.ACTION_SHOW_TASK_REMINDER"
        const val EXTRA_TASK_OBJECT = "extra_task_object_reminder"
        const val EXTRA_TASK_ID = "com.example.smarttodo.EXTRA_TASK_ID_REMINDER"
        const val EXTRA_SOUND_URI_STRING = "com.example.smarttodo.EXTRA_SOUND_URI_STRING"
        const val EXTRA_IS_PRE_REMINDER = "com.example.smarttodo.EXTRA_IS_PRE_REMINDER"
        private const val TAG = "TaskReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_TASK_REMINDER) {
            Log.d(TAG, "Received task reminder intent")

            // Use a wake lock to ensure we can show the notification even if device is dozing
            val wakeLock = WakeLockManager.acquireWakeLock(context, "taskReminder")

            try {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)
                val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI_STRING)
                val explicitSoundUri = soundUriString?.toUri()
                val snoozeTime = intent.getLongExtra("SNOOZE_TRIGGER_TIME", 0L)
                val snoozeRequestCode = intent.getIntExtra("SNOOZE_REQUEST_CODE", -1)

                if (snoozeTime > 0) {
                    Log.d(TAG, "Processing snoozed notification triggered at ${Date(snoozeTime)}, requestCode=$snoozeRequestCode")
                }

                // FIXED: Extra logging to debug potential serialization issues
                Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")

                // First try to get the Task object from intent
                var task: Task? = null
                try {
                    task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getSerializableExtra(EXTRA_TASK_OBJECT, Task::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getSerializableExtra(EXTRA_TASK_OBJECT) as? Task
                    }

                    if (task != null) {
                        Log.d(TAG, "Successfully retrieved task from intent: ${task.title} (ID: ${task.id})")
                    } else {
                        Log.w(TAG, "Task object was null in intent")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrieving task from intent: ${e.message}")
                }

                if (task != null) {
                    showNotificationSafely(context, task, isPreReminder, explicitSoundUri)
                    WakeLockManager.releaseWakeLock(wakeLock)
                } else if (taskId != -1) {
                    Log.w(TAG, "Task object was null, retrieving from database by ID: $taskId")
                    // Use coroutine with proper error handling
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val taskDao = TaskDatabase.getDatabase(context).taskDao()
                            val retrievedTask = taskDao.getTaskById(taskId)
                            if (retrievedTask != null && !retrievedTask.isCompleted) {
                                Log.d(TAG, "Successfully retrieved active task from database: ${retrievedTask.title}")
                                showNotificationSafely(context, retrievedTask, isPreReminder, explicitSoundUri)
                            } else if (retrievedTask?.isCompleted == true) {
                                Log.d(TAG, "Task $taskId is already completed, skipping notification")
                            } else {
                                Log.e(TAG, "Task with ID $taskId not found in database")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error retrieving task from database", e)
                        } finally {
                            WakeLockManager.releaseWakeLock(wakeLock)
                        }
                    }
                } else {
                    Log.e(TAG, "Both task object and task ID are invalid")
                    WakeLockManager.releaseWakeLock(wakeLock)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing task reminder", e)
                WakeLockManager.releaseWakeLock(wakeLock)
            }
        } else {
            Log.w(TAG, "Received unexpected action: ${intent.action}")
        }
    }

    private fun showNotificationSafely(context: Context, task: Task, isPreReminder: Boolean, soundUri: Uri?) {
        try {
            // Check if task is still valid and not completed
            if (task.isCompleted) {
                Log.d(TAG, "Task ${task.id} is completed, skipping notification display")
                return
            }

            val notificationHelper = NotificationHelper(context)
            notificationHelper.showTaskReminder(task, isPreReminder, soundUri)
            Log.d(TAG, "Task reminder notification shown successfully for task ${task.id}")

            // Clean up any expired snooze entries
            SnoozeScheduler.cleanupExpiredSnoozes()

            // Log debug information about current snooze state
            val debugInfo = SnoozeScheduler.getScheduledSnoozesDebugInfo()
            if (debugInfo.isNotEmpty()) {
                Log.d(TAG, "Current scheduled snoozes after showing notification: $debugInfo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification for task ${task.id}", e)
        }
    }
}
