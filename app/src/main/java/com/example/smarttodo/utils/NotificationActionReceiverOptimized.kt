package com.example.smarttodo.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.smarttodo.ui.NotificationActionActivity

/**
 * Optimized NotificationActionReceiver with improved reliability and performance.
 * Uses the new NotificationManager for better error handling and race condition prevention.
 */
class NotificationActionReceiverOptimized : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.example.smarttodo.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.smarttodo.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SNOOZE_DURATION = "SNOOZE_DURATION"
        const val TAG = "NotificationActionOpt"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val action = intent.action

        Log.d(TAG, "Received action: $action for task ID: $taskId")

        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID received in notification action")
            return
        }

        // Check if there's already a pending operation for this task to prevent race conditions
        if (NotificationManager.isPendingOperation(taskId)) {
            Log.w(TAG, "Operation already in progress for task $taskId, ignoring duplicate request")
            return
        }

        when (action) {
            ACTION_COMPLETE -> {
                Log.d(TAG, "Processing complete action for task $taskId")
                NotificationManager.completeTask(context, taskId) { success ->
                    if (!success) {
                        Log.w(TAG, "Failed to complete task $taskId")
                    }
                }
            }
            ACTION_SNOOZE -> {
                val snoozeDuration = intent.getLongExtra(EXTRA_SNOOZE_DURATION, -1L)
                if (snoozeDuration > 0) {
                    Log.d(TAG, "Processing snooze action for task $taskId with duration ${snoozeDuration}ms")
                    NotificationManager.snoozeTask(context, taskId, snoozeDuration) { success ->
                        if (!success) {
                            Log.w(TAG, "Failed to snooze task $taskId")
                        }
                    }
                } else {
                    Log.d(TAG, "Showing snooze dialog for task $taskId")
                    showSnoozeDialog(context, taskId)
                }
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action")
            }
        }
    }

    private fun showSnoozeDialog(context: Context, taskId: Int) {
        try {
            val snoozeIntent = Intent(context, NotificationActionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(NotificationActionActivity.EXTRA_ACTION, NotificationActionActivity.ACTION_SNOOZE)
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startActivity(snoozeIntent)
            Log.d(TAG, "Successfully launched snooze dialog for task $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch snooze dialog for task $taskId", e)
        }
    }
}
