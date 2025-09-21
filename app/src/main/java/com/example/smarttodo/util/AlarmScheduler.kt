package com.example.smarttodo.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.smarttodo.data.Task // Your Task model
import com.example.smarttodo.receiver.TaskReminderReceiver // Your BroadcastReceiver
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    const val EXTRA_IS_PRE_REMINDER = "com.example.smarttodo.EXTRA_IS_PRE_REMINDER" // Added constant

    /**
     * Schedules a reminder for the given task.
     * If the task has no reminder flag set or no due date, the reminder is not scheduled.
     *
     * @param context The application context.
     * @param task The task for which to schedule a reminder. This parameter must be non-null by declaration.
     */
    fun scheduleReminder(context: Context, task: Task) {
        // Extremely verbose logging at the very start of the method for NPE diagnosis
        Log.i(TAG, "scheduleReminder ENTRY. Task ID: ${task?.id ?: "NULL_TASK_OBJECT"}, Title: ${task?.title ?: "NULL_TASK_OBJECT"}, Due: ${task?.dueDate ?: "NULL_TASK_OBJECT"}, Reminder: ${task?.hasReminder ?: "NULL_TASK_OBJECT"}. Task instance: ${System.identityHashCode(task)}")
        
        try {
            // The original non-null parameter check by Kotlin compiler happens before this try block technically.
            // If 'task' is null here, the NPE from parameter check already occurred.

            if (task == null) { // Explicit redundant check for logging if somehow bypassed initial check
                Log.e(TAG, "CRITICAL_ERROR: 'task' parameter is NULL inside scheduleReminder, even after non-null declaration! This should not happen.")
                // Potentially throw a custom exception here or return, though the NPE would have already happened.
                return
            }

            if (!task.hasReminder || task.dueDate == null) {
                Log.d(TAG, "Reminder not scheduled for task '${task.title}' (ID: ${task.id}) - no reminder flag or no due date.")
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Due date is checked as non-null by the condition above.
            val reminderTime = Calendar.getInstance().apply {
                time = task.dueDate!! 
                task.preReminderOffsetMinutes?.let {
                    add(Calendar.MINUTE, -it)
                }
            }

            if (reminderTime.before(Calendar.getInstance())) {
                Log.w(TAG, "Reminder time for task '${task.title}' (ID: ${task.id}) is in the past. Not scheduling.")
                return
            }

            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER // Corrected constant
                putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id, 
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // No try-catch around alarmManager calls here, as the outer try-catch will handle it.
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime.timeInMillis, pendingIntent)
                        Log.i(TAG, "Exact alarm scheduled (Android S+) for task '${task.title}' (ID: ${task.id}) at ${reminderTime.time}")
                    } else {
                        Log.w(TAG, "Cannot schedule exact alarm for task '${task.title}' (ID: ${task.id}). SCHEDULE_EXACT_ALARM permission not granted or disabled on Android S+.")
                        // Fallback: Consider scheduling a non-exact alarm or guiding the user to settings
                        // alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime.timeInMillis, pendingIntent)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime.timeInMillis, pendingIntent)
                    Log.i(TAG, "Exact alarm scheduled (Android M-R) for task '${task.title}' (ID: ${task.id}) at ${reminderTime.time}")
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.timeInMillis, pendingIntent)
                    Log.i(TAG, "Exact alarm scheduled (pre-M) for task '${task.title}' (ID: ${task.id}) at ${reminderTime.time}")
                }
            }

        } catch (e: Throwable) { // Catch Throwable to be absolutely sure, including Errors.
            // This block will only be hit if the NPE happens *after* the initial parameter check, or if other exceptions occur.
            Log.e(TAG, "EXCEPTION in scheduleReminder for task ID: ${task?.id ?: "TASK_WAS_NULL_ON_ENTRY_OR_BECAME_NULL"}, Title: ${task?.title ?: "N/A"}. Exception: ${e.javaClass.simpleName}", e)
            // Re-throw if you want it to propagate after logging, or handle it (e.g., show error to user via a different mechanism)
            // For now, let it propagate to see if TaskViewModel catches it. But if it got here, the initial NPE already happened.
            // throw e 
        }
        Log.i(TAG, "scheduleReminder EXIT. Task ID: ${task?.id ?: "NULL_TASK_OBJECT"}")
    }

    /**
     * Cancels any scheduled reminder for the given task ID.
     *
     * @param context The application context.
     * @param taskId The ID of the task for which to cancel the reminder.
     */
    fun cancelReminder(context: Context, taskId: Int) {
        // ... (cancelReminder implementation remains the same)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER // Corrected constant: Must match the action used for scheduling
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
            Log.d(TAG, "No reminder found to cancel for task ID: $taskId (PendingIntent was null)")
        }
    }
}
