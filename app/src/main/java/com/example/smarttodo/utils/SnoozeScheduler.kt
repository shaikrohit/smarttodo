package com.example.smarttodo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.example.smarttodo.data.Task
import com.example.smarttodo.receiver.TaskReminderReceiver
import com.example.smarttodo.util.AlarmScheduler
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Optimized snooze scheduling system with proper PendingIntent management
 * and guaranteed reliability for repeated snooze operations.
 */
object SnoozeScheduler {
    private const val TAG = "SnoozeScheduler"
    private const val MAX_RETRY_ATTEMPTS = 3
    private val scheduledSnoozes = ConcurrentHashMap<Int, SnoozeInfo>()
    private val requestCodeCounter = AtomicInteger(10000) // Start from 10000 to avoid conflicts

    data class SnoozeInfo(
        val triggerTime: Long,
        val requestCode: Int,
        val pendingIntent: PendingIntent?
    )

    /**
     * Schedules a snooze reminder with guaranteed reliability for repeated use
     */
    fun scheduleSnoozeReminder(context: Context, task: Task, snoozeDurationMillis: Long): Boolean {
        Log.d(TAG, "Starting snooze scheduling for task ${task.id}, duration: ${snoozeDurationMillis}ms")

        // Cancel any existing snooze for this task first
        cancelExistingSnooze(context, task.id)

        var attempt = 0
        while (attempt < MAX_RETRY_ATTEMPTS) {
            attempt++

            try {
                if (performSnoozeScheduling(context, task, snoozeDurationMillis)) {
                    Log.i(TAG, "Successfully scheduled snooze for task ${task.id} on attempt $attempt")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt failed to schedule snooze for task ${task.id}", e)
            }

            // Brief pause before retry (exponential backoff)
            if (attempt < MAX_RETRY_ATTEMPTS) {
                Thread.sleep(50L * attempt)
            }
        }

        Log.e(TAG, "Failed to schedule snooze for task ${task.id} after $MAX_RETRY_ATTEMPTS attempts")
        return false
    }

    private fun cancelExistingSnooze(context: Context, taskId: Int) {
        val existingSnooze = scheduledSnoozes[taskId]
        if (existingSnooze != null) {
            try {
                // Cancel the existing PendingIntent
                existingSnooze.pendingIntent?.cancel()

                // Also try to cancel via AlarmManager
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                existingSnooze.pendingIntent?.let { alarmManager.cancel(it) }

                scheduledSnoozes.remove(taskId)
                Log.d(TAG, "Cancelled existing snooze for task $taskId")
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling existing snooze for task $taskId", e)
            }
        }
    }

    private fun performSnoozeScheduling(context: Context, task: Task, snoozeDurationMillis: Long): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: run {
                Log.e(TAG, "AlarmManager not available")
                return false
            }

        // Validate inputs
        if (snoozeDurationMillis <= 0) {
            Log.e(TAG, "Invalid snooze duration: $snoozeDurationMillis")
            return false
        }

        // Check alarm permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms - using inexact alarm as fallback")
        }

        // Get sound settings safely
        val notificationHelper = NotificationHelper(context)
        val (soundMode, customSoundUri) = try {
            notificationHelper.getNotificationSoundSettings()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get sound settings, using default", e)
            com.example.smarttodo.util.SoundMode.DEFAULT to null
        }

        val soundUriToPass = when (soundMode) {
            com.example.smarttodo.util.SoundMode.CUSTOM ->
                customSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            com.example.smarttodo.util.SoundMode.DEFAULT ->
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            com.example.smarttodo.util.SoundMode.SILENT -> null
        }

        val triggerTime = System.currentTimeMillis() + snoozeDurationMillis

        // Validate trigger time
        if (triggerTime <= System.currentTimeMillis()) {
            Log.e(TAG, "Invalid trigger time: $triggerTime")
            return false
        }

        // Generate guaranteed unique request code
        val requestCode = generateGuaranteedUniqueRequestCode(task.id)

        // Create intent with comprehensive data
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_SHOW_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_OBJECT, task)
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_SOUND_URI_STRING, soundUriToPass?.toString())
            putExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
            putExtra("SNOOZE_TRIGGER_TIME", triggerTime)
            putExtra("SNOOZE_SOURCE", "user_snooze")
            putExtra("SNOOZE_REQUEST_CODE", requestCode) // For debugging
        }

        // Create PendingIntent with FLAG_CANCEL_CURRENT to ensure no conflicts
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "Scheduling snooze for task ${task.id} at ${Date(triggerTime)} with requestCode $requestCode")

        // Schedule alarm with version-appropriate method
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        Log.d(TAG, "Scheduled exact alarm (S+) for task ${task.id}")
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        Log.d(TAG, "Scheduled inexact alarm (S+) for task ${task.id}")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "Scheduled exact alarm (M+) for task ${task.id}")
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "Scheduled exact alarm (legacy) for task ${task.id}")
                }
            }

            // Store snooze info for proper management
            scheduledSnoozes[task.id] = SnoozeInfo(triggerTime, requestCode, pendingIntent)
            true

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm for task ${task.id}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception scheduling alarm for task ${task.id}", e)
            false
        }
    }

    private fun generateGuaranteedUniqueRequestCode(taskId: Int): Int {
        // Use atomic counter to guarantee uniqueness across all snooze operations
        val counter = requestCodeCounter.getAndIncrement()
        // Combine taskId with counter for complete uniqueness
        return (taskId * 100000) + (counter % 100000)
    }

    /**
     * Cancels a scheduled snooze if it exists
     */
    fun cancelScheduledSnooze(context: Context, taskId: Int): Boolean {
        return try {
            val snoozeInfo = scheduledSnoozes.remove(taskId)
            if (snoozeInfo != null) {
                snoozeInfo.pendingIntent?.cancel()
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                snoozeInfo.pendingIntent?.let { alarmManager.cancel(it) }
                Log.d(TAG, "Successfully cancelled scheduled snooze for task $taskId")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling snooze for task $taskId", e)
            false
        }
    }

    /**
     * Checks if a task has a scheduled snooze
     */
    fun hasScheduledSnooze(taskId: Int): Boolean {
        val snoozeInfo = scheduledSnoozes[taskId]
        return snoozeInfo != null && snoozeInfo.triggerTime > System.currentTimeMillis()
    }

    /**
     * Cleans up expired snooze entries
     */
    fun cleanupExpiredSnoozes() {
        val currentTime = System.currentTimeMillis()
        val expiredTasks = scheduledSnoozes.filterValues { it.triggerTime <= currentTime }.keys.toList()
        expiredTasks.forEach { taskId ->
            scheduledSnoozes.remove(taskId)
        }
        if (expiredTasks.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${expiredTasks.size} expired snooze entries")
        }
    }

    /**
     * Gets debug information about scheduled snoozes
     */
    fun getScheduledSnoozesDebugInfo(): Map<Int, String> {
        return scheduledSnoozes.mapValues { (taskId, snoozeInfo) ->
            "Task $taskId: triggers at ${Date(snoozeInfo.triggerTime)}, requestCode=${snoozeInfo.requestCode}"
        }
    }
}
