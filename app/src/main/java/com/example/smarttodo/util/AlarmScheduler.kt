package com.example.smarttodo.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.TaskStackBuilder
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.smarttodo.MainActivity
import com.example.smarttodo.R
import com.example.smarttodo.data.Task // Your Task model
import com.example.smarttodo.receiver.TaskReminderReceiver // Your BroadcastReceiver
import java.util.Calendar

object AlarmScheduler {

    /**
     * HIGH-LEVEL FLOW (Scheduling Pipeline)
     * 1. User creates/updates a Task (with optional dueDate + hasReminder + optional preReminderOffsetMinutes).
     * 2. UI / ViewModel calls AlarmScheduler.scheduleReminder(context, task) after persistence.
     * 3. We validate permissions + task state. If invalid → existing alarm is cancelled.
     * 4. We compute the reminder fire time:
     *      - If task.preReminderOffsetMinutes != null -> we schedule ONLY the pre-reminder time here (fire earlier)
     *      - The TaskReminderReceiver is responsible for distinguishing pre vs main reminder and potentially chaining.
     * 5. We embed: task object, sound config (resolved via NotificationHelper prefs), and a boolean EXTRA_IS_PRE_REMINDER.
     * 6. AlarmManager schedules an exact alarm (when allowed) or falls back.
     * 7. When the alarm fires TaskReminderReceiver constructs and shows the notification (vibration, actions, etc.).
     * 8. User may Snooze (SnoozeTaskReceiver / NotificationActionReceiver) or Complete (NotificationActionReceiver) which cancels or reschedules.
     *
     * SAFETY / RESILIENCE NOTES
     * - We guard every external call with try/catch.
     * - We never keep stale alarms: if reminder conditions become invalid (no dueDate / hasReminder false) we cancel.
     * - We do NOT attempt to schedule in the past; those are ignored + cancelled.
     * - Permissions (POST_NOTIFICATIONS, exact alarm privilege) are validated before actual scheduling.
     */

    private const val TAG = "AlarmScheduler"
    // Ensure this constant matches the one in TaskReminderReceiver.kt
    const val EXTRA_IS_PRE_REMINDER = "com.example.smarttodo.EXTRA_IS_PRE_REMINDER" 

    // New helper to check required permissions/state for notifications and exact alarms
    fun hasRequiredAlarmAndNotificationPermissions(context: Context): Boolean {
        // Check POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificationGranted) {
                Log.w(TAG, "Missing POST_NOTIFICATIONS permission (Android 13+).")
                return false
            }
        }

        // Check exact alarm scheduling availability (Android S+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms: SCHEDULE_EXACT_ALARM not granted or user disabled it.")
                return false
            }
        }

        return true
    }

    /**
     * Schedules a reminder for the given task.
     * If the task has no reminder flag set or no due date, the reminder is not scheduled.
     *
     * @param context The application context.
     * @param task The task for which to schedule a reminder. This parameter must be non-null by declaration.
     */
    fun scheduleReminder(context: Context, task: Task) {
        // Removed impossible null checks (task is non-null at signature level)
        Log.i(TAG, "scheduleReminder ENTRY id=${task.id} title='${task.title}' due=${task.dueDate} hasReminder=${task.hasReminder} preOffset=${task.preReminderOffsetMinutes}")
        try {
            if (!hasRequiredAlarmAndNotificationPermissions(context)) {
                Log.w(TAG, "Permissions missing – skipping schedule for taskId=${task.id}")
                return
            }
            if (!task.hasReminder || task.dueDate == null) {
                Log.d(TAG, "No reminder conditions – cancelling (if existed) taskId=${task.id}")
                cancelReminder(context, task.id)
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Compute trigger time: if preOffset provided, schedule PRE reminder earlier; TaskReminderReceiver will know.
            val triggerCal = Calendar.getInstance().apply {
                time = task.dueDate
                task.preReminderOffsetMinutes?.let { add(Calendar.MINUTE, -it) }
            }

            if (triggerCal.before(Calendar.getInstance())) {
                Log.w(TAG, "Computed trigger in past – cancelling existing & skipping. taskId=${task.id}")
                cancelReminder(context, task.id)
                return
            }

            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER
                putExtra(TaskReminderReceiver.EXTRA_TASK_OBJECT, task)
                val notificationHelper = com.example.smarttodo.utils.NotificationHelper(context)
                val (soundMode, customSoundUri) = notificationHelper.getNotificationSoundSettings()
                val soundUriToPass: Uri? = when (soundMode) {
                    SoundMode.CUSTOM -> customSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    SoundMode.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    SoundMode.SILENT -> null
                }
                putExtra(TaskReminderReceiver.EXTRA_SOUND_URI_STRING, soundUriToPass?.toString())
                putExtra(EXTRA_IS_PRE_REMINDER, task.preReminderOffsetMinutes != null && task.preReminderOffsetMinutes > 0)
                putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerMillis = triggerCal.timeInMillis
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if ((context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                        Log.i(TAG, "Exact alarm (S+) scheduled @${triggerCal.time} taskId=${task.id}")
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                        Log.w(TAG, "Fallback inexact alarm (missing exact permission) @${triggerCal.time} taskId=${task.id}")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    Log.i(TAG, "Exact alarm (M-R) scheduled @${triggerCal.time} taskId=${task.id}")
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    Log.i(TAG, "Exact alarm (pre-M) scheduled @${triggerCal.time} taskId=${task.id}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "scheduleReminder exception taskId=${task.id} ${e.javaClass.simpleName}:${e.message}", e)
        } finally {
            Log.i(TAG, "scheduleReminder EXIT id=${task.id}")
        }
    }

    /**
     * Cancels any scheduled reminder for the given task ID.
     *
     * @param context The application context.
     * @param taskId The ID of the task for which to cancel the reminder.
     */
    fun cancelReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER // Must match the action used for scheduling
        }
        // Use FLAG_NO_CREATE to check if a PendingIntent exists without creating one
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE 
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Also cancel the PendingIntent itself
            Log.i(TAG, "Cancelled reminder for task ID: $taskId")
        } else {
            Log.d(TAG, "No reminder found to cancel for task ID: $taskId (PendingIntent was null or did not exist)")
        }
    }
}
