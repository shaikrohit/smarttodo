package com.example.smarttodo.data

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.smarttodo.util.OperationResult // Added import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class that abstracts access to task data sources.
 * It provides a clean API for the rest of the application to interact with task data,
 * handling the underlying data operations, typically by delegating to a [TaskDao].
 * All suspend functions in this repository perform database operations on the IO dispatcher.
 * Write operations return [OperationResult] to indicate success or failure.
 *
 * @property taskDao The Data Access Object for tasks, used to perform database operations.
 */
class TaskRepository(private val taskDao: TaskDao) {

    private val TAG = "TaskRepository" // For logging

    /**
     * Retrieves a list of tasks, optionally filtered by completion status and a search query.
     */
    fun getTasks(query: String, isCompleted: Boolean?): LiveData<List<Task>> {
        return taskDao.getTasks(query, isCompleted)
    }

    /**
     * Retrieves a single task by its unique ID from the database.
     */
    suspend fun getTaskById(taskId: Int): Task? {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getTaskById(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getTaskById for id: $taskId", e)
                null // Return null on error, consistent with original behavior
            }
        }
    }

    /**
     * Inserts a new task into the database.
     * @return [OperationResult.Success] with the new row ID, or [OperationResult.Error].
     */
    suspend fun insert(task: Task): OperationResult<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val newRowId = taskDao.insert(task)
                if (newRowId > 0) {
                    OperationResult.Success(newRowId)
                } else {
                    // This case might indicate an issue not throwing an exception but failing to insert.
                    Log.e(TAG, "Insert task failed, DAO returned row ID: $newRowId for task: ${task.title}")
                    OperationResult.Error(Exception("Insert failed, DAO returned $newRowId"), "Failed to save task.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception inserting task: ${task.title}", e)
                OperationResult.Error(e, "Failed to save task.")
            }
        }
    }

    /**
     * Updates an existing task in the database.
     * @return [OperationResult.Success] if successful, or [OperationResult.Error].
     */
    suspend fun update(task: Task): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.update(task)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating task: ${task.title}", e)
                OperationResult.Error(e, "Failed to update task.")
            }
        }
    }

    /**
     * Deletes a specific task from the database.
     * @return [OperationResult.Success] if successful, or [OperationResult.Error].
     */
    suspend fun delete(task: Task): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.delete(task)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting task: ${task.title}", e)
                OperationResult.Error(e, "Failed to delete task.")
            }
        }
    }

    /**
     * Toggles the completion status of a task identified by its ID.
     * @return [OperationResult.Success] if successful, or [OperationResult.Error]
     *         if the task is not found or an error occurs.
     */
    suspend fun toggleTaskCompletion(taskId: Int): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val task = taskDao.getTaskById(taskId)
                if (task != null) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    taskDao.update(updatedTask)
                    OperationResult.Success(Unit)
                } else {
                    Log.w(TAG, "Task with ID $taskId not found for toggle completion.")
                    OperationResult.Error(NoSuchElementException("Task with ID $taskId not found"), "Task not found.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception toggling task completion for ID: $taskId", e)
                OperationResult.Error(e, "Failed to update task status.")
            }
        }
    }

    /**
     * Deletes all tasks that are marked as completed from the database.
     * @return [OperationResult.Success] if successful, or [OperationResult.Error].
     */
    suspend fun deleteCompletedTasks(): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.deleteCompletedTasks()
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting completed tasks", e)
                OperationResult.Error(e, "Failed to delete completed tasks.")
            }
        }
    }

    /**
     * Deletes all tasks from the database.
     * @return [OperationResult.Success] if successful, or [OperationResult.Error].
     */
    suspend fun deleteAllTasks(): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.deleteAllTasks()
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting all tasks", e)
                OperationResult.Error(e, "Failed to delete all tasks.")
            }
        }
    }

    /**
     * Gets the total number of tasks currently in the database.
     */
    suspend fun getTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getTaskCount()
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getTaskCount", e)
                0 // Return 0 on error
            }
        }
    }

    /**
     * Gets the number of tasks that are marked as completed.
     */
    suspend fun getCompletedTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getCompletedTaskCount()
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getCompletedTaskCount", e)
                0 // Return 0 on error
            }
        }
    }
}
