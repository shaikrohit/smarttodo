package com.example.smarttodo.service

import android.content.Context
import android.util.Log
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.utils.NotificationHelper
import com.example.smarttodo.utils.SnoozeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Scalable, efficient, and reusable notification service that handles all notification operations.
 * This service is designed to be fast, thread-safe, and highly maintainable.
 */
object NotificationService {
    private const val TAG = "NotificationService"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val operationMutex = Mutex()
    private val activeOperations = ConcurrentHashMap<String, Long>()

    /**
     * Completes a task with optimized database operations and UI feedback
     */
    fun completeTask(context: Context, taskId: Int, onResult: ((Boolean, String?) -> Unit)? = null) {
        val operationKey = "complete_$taskId"

        scope.launch {
            operationMutex.withLock {
                // Prevent duplicate operations
                val currentTime = System.currentTimeMillis()
                val lastOperation = activeOperations[operationKey]
                if (lastOperation != null && (currentTime - lastOperation) < 2000) {
                    Log.w(TAG, "Complete operation for task $taskId is already in progress, skipping")
                    onResult?.invoke(false, "Operation already in progress")
                    return@withLock
                }

                activeOperations[operationKey] = currentTime

                try {
                    Log.d(TAG, "Starting task completion for task $taskId")

                    val taskDao = TaskDatabase.getDatabase(context).taskDao()
                    val task = taskDao.getTaskById(taskId)

                    if (task != null) {
                        // Update task in a single atomic operation
                        val completedTask = task.copy(
                            isCompleted = true,
                            completionDate = Date()
                        )
                        taskDao.update(completedTask)

                        // Handle UI feedback on main thread
                        withContext(Dispatchers.Main) {
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.cancelNotification(taskId)
                            notificationHelper.stopVibration()
                            notificationHelper.showToast("Task completed! 🎉")
                        }

                        Log.i(TAG, "Task $taskId completed successfully")
                        onResult?.invoke(true, "Task completed successfully")
                    } else {
                        Log.w(TAG, "Task not found for completion: $taskId")
                        onResult?.invoke(false, "Task not found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error completing task $taskId", e)
                    onResult?.invoke(false, "Error: ${e.message}")
                } finally {
                    activeOperations.remove(operationKey)
                }
            }
        }
    }

    /**
     * Snoozes a task with efficient scheduling and comprehensive error handling
     */
    fun snoozeTask(context: Context, taskId: Int, durationMillis: Long, onResult: ((Boolean, String?) -> Unit)? = null) {
        val operationKey = "snooze_$taskId"

        scope.launch {
            operationMutex.withLock {
                // Check for recent operations but allow snooze retries after 500ms (much shorter than before)
                val currentTime = System.currentTimeMillis()
                val lastOperation = activeOperations[operationKey]
                if (lastOperation != null && (currentTime - lastOperation) < 500) {
                    Log.w(TAG, "Snooze operation for task $taskId is too recent, skipping")
                    onResult?.invoke(false, "Operation too recent")
                    return@withLock
                }

                activeOperations[operationKey] = currentTime

                try {
                    Log.d(TAG, "Starting snooze for task $taskId, duration: ${durationMillis}ms")

                    val taskDao = TaskDatabase.getDatabase(context).taskDao()
                    val task = taskDao.getTaskById(taskId)

                    if (task != null && !task.isCompleted) {
                        // Cancel current notification immediately and stop vibration
                        withContext(Dispatchers.Main) {
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.cancelNotification(taskId)
                            notificationHelper.stopVibration()
                        }

                        // Small delay to ensure notification is cancelled before scheduling new one
                        kotlinx.coroutines.delay(100)

                        // Use the enhanced SnoozeScheduler with guaranteed PendingIntent uniqueness
                        val success = com.example.smarttodo.utils.SnoozeScheduler.scheduleSnoozeReminder(context, task, durationMillis)

                        withContext(Dispatchers.Main) {
                            if (success) {
                                val minutes = durationMillis / (60 * 1000)
                                NotificationHelper(context).showToast("Task snoozed for $minutes minutes ⏰")
                                Log.i(TAG, "Task $taskId snoozed successfully for $minutes minutes")
                                onResult?.invoke(true, "Task snoozed for $minutes minutes")
                            } else {
                                NotificationHelper(context).showToast("Failed to snooze task ❌")
                                Log.e(TAG, "Failed to schedule snooze for task $taskId")
                                onResult?.invoke(false, "Failed to schedule snooze")
                            }
                        }

                        // Debug: Log current scheduled snoozes
                        val debugInfo = com.example.smarttodo.utils.SnoozeScheduler.getScheduledSnoozesDebugInfo()
                        Log.d(TAG, "Currently scheduled snoozes: $debugInfo")

                    } else if (task?.isCompleted == true) {
                        Log.w(TAG, "Cannot snooze completed task: $taskId")
                        withContext(Dispatchers.Main) {
                            NotificationHelper(context).showToast("Cannot snooze completed task")
                        }
                        onResult?.invoke(false, "Cannot snooze completed task")
                    } else {
                        Log.w(TAG, "Task not found for snoozing: $taskId")
                        withContext(Dispatchers.Main) {
                            NotificationHelper(context).showToast("Task not found")
                        }
                        onResult?.invoke(false, "Task not found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error snoozing task $taskId", e)
                    withContext(Dispatchers.Main) {
                        NotificationHelper(context).showToast("Error snoozing task: ${e.message}")
                    }
                    onResult?.invoke(false, "Error: ${e.message}")
                } finally {
                    // Remove operation after a brief delay to prevent immediate re-execution
                    kotlinx.coroutines.delay(300)
                    activeOperations.remove(operationKey)
                }
            }
        }
    }

    /**
     * Cancels any active notification for a task
     */
    fun cancelTaskNotification(context: Context, taskId: Int) {
        try {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.cancelNotification(taskId)
            notificationHelper.stopVibration()
            Log.d(TAG, "Cancelled notification for task $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification for task $taskId", e)
        }
    }

    /**
     * Checks if there's an active operation for a task
     */
    fun hasActiveOperation(taskId: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        return activeOperations.values.any { currentTime - it < 2000 }
    }

    /**
     * Cleans up expired operations (housekeeping)
     */
    fun cleanupExpiredOperations() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = activeOperations.filterValues { currentTime - it > 5000 }.keys
        expiredKeys.forEach { activeOperations.remove(it) }

        if (expiredKeys.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${expiredKeys.size} expired operations")
        }
    }
}
