package com.example.smarttodo.utils

import android.content.Context
import android.util.Log
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskDatabase
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
 * Centralized notification management system for handling task completions and snoozing
 * with optimized performance, race condition prevention, and better reliability.
 */
object NotificationManager {
    private const val TAG = "NotificationManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val operationMutex = Mutex()
    private val pendingOperations = ConcurrentHashMap<Int, String>()

    /**
     * Completes a task with optimized database operations and notification handling
     */
    fun completeTask(context: Context, taskId: Int, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            operationMutex.withLock {
                // Prevent duplicate operations
                if (pendingOperations.containsKey(taskId)) {
                    Log.w(TAG, "Task completion already in progress for task $taskId")
                    onComplete?.invoke(false)
                    return@withLock
                }

                pendingOperations[taskId] = "completing"

                try {
                    Log.d(TAG, "Starting task completion for task $taskId")

                    val taskDao = TaskDatabase.getDatabase(context).taskDao()
                    val task = taskDao.getTaskById(taskId)

                    if (task != null) {
                        // Update task as completed in a single transaction
                        val completedTask = task.copy(
                            isCompleted = true,
                            completionDate = Date()
                        )
                        taskDao.update(completedTask)

                        // Handle UI operations on main thread
                        withContext(Dispatchers.Main) {
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.cancelNotification(taskId)
                            notificationHelper.stopVibration()
                            notificationHelper.showToast("Task completed! 🎉")
                        }

                        Log.i(TAG, "Task $taskId completed successfully")
                        onComplete?.invoke(true)
                    } else {
                        Log.w(TAG, "Task not found for completion: $taskId")
                        onComplete?.invoke(false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error completing task $taskId", e)
                    onComplete?.invoke(false)
                } finally {
                    pendingOperations.remove(taskId)
                }
            }
        }
    }

    /**
     * Snoozes a task with optimized scheduling and better error recovery
     */
    fun snoozeTask(context: Context, taskId: Int, snoozeDurationMillis: Long, onSnooze: ((Boolean) -> Unit)? = null) {
        scope.launch {
            operationMutex.withLock {
                // Prevent duplicate operations
                val operationKey = "${taskId}_snooze"
                if (pendingOperations.containsKey(taskId)) {
                    Log.w(TAG, "Task operation already in progress for task $taskId")
                    onSnooze?.invoke(false)
                    return@withLock
                }

                pendingOperations[taskId] = "snoozing"

                try {
                    Log.d(TAG, "Starting task snooze for task $taskId, duration: ${snoozeDurationMillis}ms")

                    val taskDao = TaskDatabase.getDatabase(context).taskDao()
                    val task = taskDao.getTaskById(taskId)

                    if (task != null) {
                        // Cancel current notification first
                        withContext(Dispatchers.Main) {
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.cancelNotification(taskId)
                            notificationHelper.stopVibration()
                        }

                        // Schedule snoozed reminder with retry mechanism
                        val success = SnoozeScheduler.scheduleSnoozeReminder(context, task, snoozeDurationMillis)

                        if (success) {
                            withContext(Dispatchers.Main) {
                                val minutes = snoozeDurationMillis / (60 * 1000)
                                NotificationHelper(context).showToast("Task snoozed for $minutes minutes")
                            }

                            Log.i(TAG, "Task $taskId snoozed successfully for ${snoozeDurationMillis}ms")
                            onSnooze?.invoke(true)
                        } else {
                            Log.e(TAG, "Failed to schedule snooze for task $taskId")
                            onSnooze?.invoke(false)
                        }
                    } else {
                        Log.w(TAG, "Task not found for snoozing: $taskId")
                        onSnooze?.invoke(false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error snoozing task $taskId", e)
                    onSnooze?.invoke(false)
                } finally {
                    pendingOperations.remove(taskId)
                }
            }
        }
    }

    /**
     * Cancels any pending operations for a task
     */
    fun cancelPendingOperations(taskId: Int) {
        pendingOperations.remove(taskId)
        Log.d(TAG, "Cancelled pending operations for task $taskId")
    }

    /**
     * Gets the status of pending operations
     */
    fun isPendingOperation(taskId: Int): Boolean {
        return pendingOperations.containsKey(taskId)
    }
}
