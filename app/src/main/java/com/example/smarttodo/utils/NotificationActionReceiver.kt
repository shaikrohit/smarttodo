package com.example.smarttodo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.receiver.TaskReminderReceiver
import com.example.smarttodo.ui.NotificationActionActivity
import com.example.smarttodo.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fixed and optimized notification action receiver with working vibration and snooze
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.example.smarttodo.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.smarttodo.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SNOOZE_DURATION = "SNOOZE_DURATION"
        const val TAG = "NotificationAction"

        private val snoozeRequestCodeCounter = AtomicInteger(50000)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val action = intent.action

        Log.d(TAG, "=== RECEIVED ACTION: $action for task ID: $taskId ===")

        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID received in notification action")
            return
        }

        when (action) {
            ACTION_COMPLETE -> {
                Log.d(TAG, "Processing COMPLETE action for task $taskId")
                handleCompleteAction(context, taskId)
            }
            ACTION_SNOOZE -> {
                val snoozeDuration = intent.getLongExtra(EXTRA_SNOOZE_DURATION, -1L)
                if (snoozeDuration > 0) {
                    Log.d(TAG, "Processing DIRECT SNOOZE for task $taskId, duration: $snoozeDuration ms")
                    handleDirectSnooze(context, taskId, snoozeDuration)
                } else {
                    Log.d(TAG, "Showing SNOOZE DIALOG for task $taskId")
                    showSnoozeDialog(context, taskId)
                }
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action")
            }
        }
    }

    private fun handleCompleteAction(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskDao = TaskDatabase.getDatabase(context).taskDao()
                val task = taskDao.getTaskById(taskId)

                if (task != null) {
                    // Update task as completed
                    taskDao.update(task.copy(isCompleted = true, completionDate = Date()))

                    // Cancel notification and stop vibration on main thread
                    withContext(Dispatchers.Main) {
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.stopVibration()
                        notificationHelper.cancelNotification(taskId)
                        notificationHelper.showToast("Task completed! 🎉")
                    }

                    Log.i(TAG, "✅ Task $taskId completed successfully")
                } else {
                    Log.w(TAG, "❌ Task not found for completion: $taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error completing task $taskId", e)
            }
        }
    }

    private fun handleDirectSnooze(context: Context, taskId: Int, snoozeDurationMillis: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskDao = TaskDatabase.getDatabase(context).taskDao()
                val task = taskDao.getTaskById(taskId)

                if (task != null && !task.isCompleted) {
                    // IMMEDIATELY stop vibration and cancel notification
                    withContext(Dispatchers.Main) {
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.stopVibration()
                        notificationHelper.cancelNotification(taskId)
                        Log.d(TAG, "🔇 Stopped vibration and cancelled notification for task $taskId")
                    }

                    // Small delay to ensure cleanup is complete
                    kotlinx.coroutines.delay(200)

                    // Schedule new snooze alarm with GUARANTEED unique request code
                    val success = scheduleSnoozeAlarm(context, task, snoozeDurationMillis)

                    withContext(Dispatchers.Main) {
                        if (success) {
                            val minutes = snoozeDurationMillis / (60 * 1000)
                            NotificationHelper(context).showToast("⏰ Task snoozed for $minutes minutes")
                            Log.i(TAG, "✅ Task $taskId snoozed successfully for $minutes minutes")
                        } else {
                            NotificationHelper(context).showToast("❌ Failed to snooze task")
                            Log.e(TAG, "❌ Failed to snooze task $taskId")
                        }
                    }
                } else if (task?.isCompleted == true) {
                    Log.w(TAG, "❌ Cannot snooze completed task: $taskId")
                } else {
                    Log.w(TAG, "❌ Task not found for snoozing: $taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error snoozing task $taskId", e)
            }
        }
    }

    private fun scheduleSnoozeAlarm(context: Context, task: Task, snoozeDurationMillis: Long): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Generate absolutely unique request code
            val requestCode = snoozeRequestCodeCounter.getAndIncrement()
            val triggerTime = System.currentTimeMillis() + snoozeDurationMillis

            Log.d(TAG, "🔔 Scheduling snooze alarm for task ${task.id} at ${Date(triggerTime)} with requestCode $requestCode")

            // Get sound settings
            val notificationHelper = NotificationHelper(context)
            val (soundMode, customSoundUri) = notificationHelper.getNotificationSoundSettings()
            val soundUriToPass = when (soundMode) {
                com.example.smarttodo.util.SoundMode.CUSTOM ->
                    customSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                com.example.smarttodo.util.SoundMode.DEFAULT ->
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                com.example.smarttodo.util.SoundMode.SILENT -> null
            }

            // Create intent for TaskReminderReceiver
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER
                putExtra(TaskReminderReceiver.EXTRA_TASK_OBJECT, task)
                putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
                putExtra(TaskReminderReceiver.EXTRA_SOUND_URI_STRING, soundUriToPass?.toString())
                putExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
                putExtra("SNOOZE_TRIGGER_TIME", triggerTime)
                putExtra("SNOOZE_REQUEST_CODE", requestCode)
            }

            // Create PendingIntent with FLAG_CANCEL_CURRENT to avoid conflicts
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule alarm based on Android version
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        Log.d(TAG, "✅ Scheduled exact alarm (S+) for task ${task.id}")
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        Log.d(TAG, "✅ Scheduled inexact alarm (S+) for task ${task.id}")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "✅ Scheduled exact alarm (M+) for task ${task.id}")
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "✅ Scheduled alarm (legacy) for task ${task.id}")
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling snooze alarm for task ${task.id}", e)
            false
        }
    }

    private fun showSnoozeDialog(context: Context, taskId: Int) {
        try {
            // Get the task title from database first before cancelling notification
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val taskDao = TaskDatabase.getDatabase(context).taskDao()
                    val task = taskDao.getTaskById(taskId)

                    if (task != null) {
                        val taskTitle = task.title

                        withContext(Dispatchers.Main) {
                            // Only stop vibration first, but keep notification until dialog is visible
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.stopVibration()

                            // Create intent with task title
                            val snoozeIntent = Intent(context, NotificationActionActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra(NotificationActionActivity.EXTRA_ACTION, NotificationActionActivity.ACTION_SNOOZE)
                                putExtra(EXTRA_TASK_ID, taskId)
                                putExtra("TASK_TITLE", taskTitle)
                            }

                            // Add small delay to ensure proper activity launch
                            Handler(Looper.getMainLooper()).postDelayed({
                                context.startActivity(snoozeIntent)
                                Log.d(TAG, "✅ Launched snooze dialog for task $taskId with title: $taskTitle")

                                // Now cancel notification after dialog is visible
                                Handler(Looper.getMainLooper()).postDelayed({
                                    notificationHelper.cancelNotification(taskId)
                                }, 500)
                            }, 100)
                        }
                    } else {
                        Log.e(TAG, "❌ Task not found for snooze dialog: $taskId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error retrieving task for snooze dialog: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch snooze dialog for task $taskId: ${e.message}")
        }
    }
}
