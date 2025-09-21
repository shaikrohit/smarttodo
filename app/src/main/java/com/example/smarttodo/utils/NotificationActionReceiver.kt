package com.example.smarttodo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.receiver.TaskReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.example.smarttodo.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.smarttodo.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val SNOOZE_DURATION_MILLIS = 5 * 60 * 1000L // 5 minutes
        private const val TAG = "NotificationActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        when (intent.action) {
            ACTION_COMPLETE -> completeTask(context, taskId)
            ACTION_SNOOZE -> snoozeTask(context, taskId)
        }
    }

    private fun completeTask(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val taskDao = TaskDatabase.getDatabase(context).taskDao()
            val task = taskDao.getTaskById(taskId) // Corrected method call
            task?.let {
                taskDao.update(it.copy(isCompleted = true))
                // Assuming NotificationHelper is in the same package or imported correctly
                NotificationHelper(context).cancelNotification(taskId)
            }
        }
    }

    private fun snoozeTask(context: Context, taskId: Int) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.cancelNotification(taskId)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = System.currentTimeMillis() + SNOOZE_DURATION_MILLIS

        val reminderIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            // Consider fetching task details here if needed for the rescheduled notification (e.g., sound URI)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId, // Use task ID as request code for uniqueness
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                    } else {
                        Log.w(TAG, "Cannot schedule exact alarms. App needs SCHEDULE_EXACT_ALARM permission or user setting enabled.")
                        // Fallback: Consider scheduling a non-exact alarm or guiding the user to settings
                        // alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent) // Example non-exact
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                }
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm. Check SCHEDULE_EXACT_ALARM permission.", se)
            // Handle the error, e.g., by informing the user or using a non-exact alarm as a fallback
        }
    }
}
