package com.example.smarttodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.utils.NotificationHelper
import com.example.smarttodo.utils.SnoozeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val ACTION_COMPLETE = "com.example.smarttodo.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.smarttodo.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID in intent")
            return
        }

        when (intent.action) {
            ACTION_COMPLETE -> handleActionComplete(context, taskId)
            ACTION_SNOOZE -> handleActionSnooze(context, taskId)
        }
    }

    private fun handleActionComplete(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskDao = TaskDatabase.getDatabase(context).taskDao()
                taskDao.setTaskCompleted(taskId, true)
                Log.d(TAG, "Task $taskId marked as complete")

                val notificationHelper = NotificationHelper(context)
                notificationHelper.cancelNotification(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task", e)
            }
        }
    }

    private fun handleActionSnooze(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskDao = TaskDatabase.getDatabase(context).taskDao()
                val task = taskDao.getTaskById(taskId)
                if (task != null) {
                    val notificationHelper = NotificationHelper(context)
                    val snoozeTimeMinutes = notificationHelper.getSnoozeDuration()
                    val snoozeTimeMillis = TimeUnit.MINUTES.toMillis(snoozeTimeMinutes.toLong())

                    if (SnoozeScheduler.scheduleSnoozeReminder(context, task, snoozeTimeMillis)) {
                        Log.d(TAG, "Task $taskId snoozed for $snoozeTimeMinutes minutes")
                    } else {
                        Log.e(TAG, "Failed to schedule snooze for task $taskId")
                    }

                    notificationHelper.cancelNotification(taskId)
                } else {
                    Log.e(TAG, "Task not found for snoozing: $taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing task", e)
            }
        }
    }
}
