package com.example.smarttodo.utils

import android.content.Context
import android.util.Log
import com.example.smarttodo.service.NotificationService
import com.example.smarttodo.utils.SnoozeScheduler

/**
 * Debug utility for monitoring and troubleshooting notification and snooze operations
 */
object NotificationDebugger {
    private const val TAG = "NotificationDebugger"

    /**
     * Performs comprehensive health check of the notification system
     */
    fun performHealthCheck(context: Context): String {
        val report = StringBuilder()
        report.appendLine("=== Notification System Health Check ===")

        // Check SnoozeScheduler state
        val scheduledSnoozes = SnoozeScheduler.getScheduledSnoozesDebugInfo()
        report.appendLine("Active Snoozes: ${scheduledSnoozes.size}")
        scheduledSnoozes.forEach { (taskId, info) ->
            report.appendLine("  - $info")
        }

        // Check for expired snoozes
        SnoozeScheduler.cleanupExpiredSnoozes()

        // Check NotificationService operations
        NotificationService.cleanupExpiredOperations()

        report.appendLine("Health check completed at ${System.currentTimeMillis()}")

        val reportString = report.toString()
        Log.i(TAG, reportString)
        return reportString
    }

    /**
     * Logs detailed information about a snooze attempt
     */
    fun logSnoozeAttempt(taskId: Int, duration: Long, success: Boolean, error: String? = null) {
        val minutes = duration / (60 * 1000)
        Log.d(TAG, "SNOOZE ATTEMPT: Task $taskId for $minutes minutes - Success: $success${error?.let { ", Error: $it" } ?: ""}")
    }

    /**
     * Verifies that a task can be snoozed
     */
    fun canSnoozeTask(taskId: Int): Pair<Boolean, String> {
        return try {
            val hasExisting = SnoozeScheduler.hasScheduledSnooze(taskId)
            if (hasExisting) {
                true to "Task has existing snooze (will be replaced)"
            } else {
                true to "Task ready for snooze"
            }
        } catch (e: Exception) {
            false to "Error checking snooze status: ${e.message}"
        }
    }
}
