package com.example.smarttodo.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.smarttodo.SmartTodoApplication
import com.example.smarttodo.data.Task // Ensure Task is imported
import com.example.smarttodo.util.AlarmScheduler // For EXTRA_IS_PRE_REMINDER
import com.example.smarttodo.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class SnoozeTaskReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE_TASK = "com.example.smarttodo.ACTION_SNOOZE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "com.example.smarttodo.EXTRA_SNOOZE_NOTIFICATION_ID"
        const val EXTRA_SOUND_URI_STRING = "com.example.smarttodo.EXTRA_SOUND_URI_STRING" // New constant
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE_TASK) {
            val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            val isPreReminder = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
            val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI_STRING) // Retrieve sound URI string

            if (taskId != -1 && notificationId != -1) {
                NotificationManagerCompat.from(context).cancel(notificationId)

                val repository = (context.applicationContext as SmartTodoApplication).repository
                CoroutineScope(Dispatchers.IO).launch {
                    val task = repository.getTaskById(taskId)
                    task?.let {
                        scheduleSnoozedNotification(context, it, isPreReminder, notificationId, soundUriString)
                    }
                }
            }
        }
    }

    private fun scheduleSnoozedNotification(
        context: Context,
        task: Task,
        isPreReminder: Boolean,
        originalNotificationId: Int,
        soundUriString: String? // Added parameter
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationHelper = NotificationHelper(context)
        val snoozeDurationMinutes = notificationHelper.getSnoozeDuration()

        val snoozeFiredIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_OBJECT, task)
            putExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, isPreReminder)
            putExtra(TaskReminderReceiver.EXTRA_SOUND_URI_STRING, soundUriString) // Pass sound URI string along
        }

        val requestCode = ("snooze_" + originalNotificationId).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            snoozeFiredIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeDurationMinutes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                 alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                // Fallback for when exact alarms are denied by the user after targeting S+
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            // For versions prior to S, canScheduleExactAlarms() doesn't exist
            alarmManager.setExactAndAllowWhileIdle( // Or setAndAllowWhileIdle depending on your needs
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
