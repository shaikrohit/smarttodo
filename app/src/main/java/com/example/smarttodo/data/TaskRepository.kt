package com.example.smarttodo.data

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.smarttodo.util.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.NoSuchElementException // Import for explicit exception type

/**
 * Repository class that abstracts access to task data sources, specifically the [TaskDao].
 * It provides a clean API for the rest of the application to interact with task data,
 * handling background execution and error mapping.
 *
 * @param taskDao The Data Access Object for tasks.
 */
class TaskRepository(private val taskDao: TaskDao) {

    private companion object {
        private const val TAG = "TaskRepository"
    }

    /**
     * Retrieves a LiveData list of tasks based on a search query and completion status.
     * Data is observed for UI updates.
     *
     * @param query The search string to filter tasks by title or description.
     * @param isCompleted Null to fetch all tasks, true for completed, false for incomplete.
     * @return LiveData list of [Task] objects.
     */
    fun getTasks(query: String, isCompleted: Boolean?): LiveData<List<Task>> {
        Log.d(TAG, "Fetching tasks with query: '$query', isCompleted: $isCompleted")
        // LiveData from Room is already asynchronous.
        return taskDao.getTasks(query, isCompleted)
    }

    /**
     * Retrieves a single task by its ID, performing the operation on an IO thread.
     *
     * @param taskId The ID of the task to retrieve.
     * @return The [Task] object if found, otherwise null.
     */
    suspend fun getTaskById(taskId: Int): Task? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching task by ID: $taskId")
                taskDao.getTaskById(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getTaskById for id: $taskId", e)
                null
            }
        }
    }

    /**
     * Retrieves a single task by its ID synchronously from the DAO, but executed on an IO thread from the repository.
     * This version does not return LiveData.
     *
     * @param taskId The ID of the task to retrieve.
     * @return The [Task] object if found, otherwise null.
     */
    suspend fun getTaskByIdNonLiveData(taskId: Int): Task? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching task by ID (non-LiveData): $taskId")
                taskDao.getTaskByIdNonLiveData(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getTaskByIdNonLiveData for id: $taskId", e)
                null
            }
        }
    }

    /**
     * Inserts a task into the database on an IO thread.
     *
     * @param task The [Task] to insert.
     * @return [OperationResult.Success] with the new row ID if successful,
     *         otherwise [OperationResult.Error].
     */
    suspend fun insert(task: Task): OperationResult<Long> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inserting task: ${task.title}")
                val newRowId = taskDao.insert(task)
                if (newRowId > 0) {
                    Log.i(TAG, "Task '${task.title}' inserted successfully with new row ID: $newRowId")
                    OperationResult.Success(newRowId)
                } else {
                    Log.e(TAG, "Insert task '${task.title}' failed, DAO returned row ID: $newRowId")
                    OperationResult.Error(Exception("DAO insert returned non-positive row ID: $newRowId"), "Failed to save task.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception inserting task '${task.title}'", e)
                OperationResult.Error(e, "An error occurred while saving the task.")
            }
        }
    }

    /**
     * Updates an existing task in the database on an IO thread.
     *
     * @param task The [Task] to update.
     * @return [OperationResult.Success] if successful, otherwise [OperationResult.Error].
     */
    suspend fun update(task: Task): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating task: ${task.title} (ID: ${task.id})")
                taskDao.update(task)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating task '${task.title}' (ID: ${task.id})", e)
                OperationResult.Error(e, "An error occurred while updating the task.")
            }
        }
    }

    /**
     * Deletes a task from the database on an IO thread.
     *
     * @param task The [Task] to delete.
     * @return [OperationResult.Success] if successful, otherwise [OperationResult.Error].
     */
    suspend fun delete(task: Task): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting task: ${task.title} (ID: ${task.id})")
                taskDao.delete(task)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting task '${task.title}' (ID: ${task.id})", e)
                OperationResult.Error(e, "An error occurred while deleting the task.")
            }
        }
    }

    /**
     * Toggles the completion status of a task by its ID on an IO thread.
     * Fetches the task, updates its status, and saves it.
     *
     * @param taskId The ID of the task to toggle.
     * @return [OperationResult.Success] if successful, otherwise [OperationResult.Error].
     */
    suspend fun toggleTaskCompletion(taskId: Int): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Toggling completion for task ID: $taskId")
                val task = taskDao.getTaskByIdNonLiveData(taskId) // Fetch non-LiveData version for update
                if (task != null) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    taskDao.update(updatedTask)
                    Log.i(TAG, "Task ID $taskId completion toggled to ${updatedTask.isCompleted}")
                    OperationResult.Success(Unit)
                } else {
                    Log.w(TAG, "Task with ID $taskId not found for toggle completion.")
                    OperationResult.Error(NoSuchElementException("Task with ID $taskId not found for toggle."), "Task not found.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception toggling task completion for ID: $taskId", e)
                OperationResult.Error(e, "Failed to update task status.")
            }
        }
    }

    /**
     * Deletes all completed tasks from the database on an IO thread.
     *
     * @return [OperationResult.Success] if successful, otherwise [OperationResult.Error].
     */
    suspend fun deleteCompletedTasks(): OperationResult<Int> { // Return number of deleted tasks if possible, or Unit
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting completed tasks.")
                val numberOfRowsDeleted = taskDao.deleteCompletedTasks() // Assuming DAO returns Int
                Log.i(TAG, "Number of completed tasks deleted: $numberOfRowsDeleted")
                OperationResult.Success(numberOfRowsDeleted)
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting completed tasks", e)
                OperationResult.Error(e, "Failed to delete completed tasks.")
            }
        }
    }

    /**
     * Retrieves all completed tasks synchronously from DAO, executed on an IO thread.
     * @return A list of completed [Task] objects, or an empty list on error.
     */
    suspend fun getCompletedTasksNonLiveData(): List<Task> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching completed tasks (non-LiveData).")
                taskDao.getCompletedTasksNonLiveData()
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getCompletedTasksNonLiveData", e)
                emptyList()
            }
        }
    }

    /**
     * Deletes all tasks from the database on an IO thread.
     *
     * @return [OperationResult.Success] if successful, otherwise [OperationResult.Error].
     */
    suspend fun deleteAllTasks(): OperationResult<Int> { // Return number of deleted tasks if possible, or Unit
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting all tasks.")
                val numberOfRowsDeleted = taskDao.deleteAllTasks() // Assuming DAO returns Int
                Log.i(TAG, "Number of all tasks deleted: $numberOfRowsDeleted")
                OperationResult.Success(numberOfRowsDeleted)
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting all tasks", e)
                OperationResult.Error(e, "Failed to delete all tasks.")
            }
        }
    }

    /**
     * Retrieves all tasks synchronously from DAO, executed on an IO thread.
     * @return A list of all [Task] objects, or an empty list on error.
     */
    suspend fun getAllTasksNonLiveData(): List<Task> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching all tasks (non-LiveData).")
                taskDao.getAllTasksNonLiveData()
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getAllTasksNonLiveData", e)
                emptyList()
            }
        }
    }

    /**
     * Gets the total count of tasks on an IO thread.
     * @return The total number of tasks, or 0 on error.
     */
    suspend fun getTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting task count.")
                taskDao.getTaskCount()
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getTaskCount", e)
                0
            }
        }
    }

    /**
     * Gets the count of completed tasks on an IO thread.
     * @return The number of completed tasks, or 0 on error.
     */
    suspend fun getCompletedTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting completed task count.")
                taskDao.getCompletedTaskCount()
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getCompletedTaskCount", e)
                0
            }
        }
    }
}
