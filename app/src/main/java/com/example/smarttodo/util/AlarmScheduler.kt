package com.example.smarttodo.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.smarttodo.data.Task // Your Task model
import com.example.smarttodo.receiver.TaskReminderReceiver // Your BroadcastReceiver
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    const val EXTRA_IS_PRE_REMINDER = "com.example.smarttodo.EXTRA_IS_PRE_REMINDER"

    fun hasRequiredAlarmAndNotificationPermissions(context: Context): Boolean {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms: SCHEDULE_EXACT_ALARM not granted or user disabled it.")
                return false
            }
        }

        return true
    }

    fun scheduleReminder(context: Context, task: Task) {
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
                putExtra(TaskReminderReceiver.EXTRA_SOUND_URI_STRING, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString())
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

    fun cancelReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled reminder for task ID: $taskId")
        } else {
            Log.d(TAG, "No reminder found to cancel for task ID: $taskId (PendingIntent was null or did not exist)")
        }
    }

    fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}
