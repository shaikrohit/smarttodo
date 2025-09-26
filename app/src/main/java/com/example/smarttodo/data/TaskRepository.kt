package com.example.smarttodo.data

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.smarttodo.util.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.NoSuchElementException // Import for explicit exception type

class TaskRepository(private val taskDao: TaskDao) {

    private companion object {
        private const val TAG = "TaskRepository"
    }

    /**
     * Returns LiveData of tasks. If the query is blank/empty, uses a simpler DAO query
     * that does not use LIKE and therefore performs better for large datasets.
     */
    fun getTasks(query: String, isCompleted: Boolean?): LiveData<List<Task>> {
        return if (query.isBlank()) {
            taskDao.getAllTasks(isCompleted)
        } else {
            taskDao.getTasks(query, isCompleted)
        }
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getTaskById(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "getTaskById failed for id=$taskId", e)
                null
            }
        }
    }

    suspend fun getTaskByIdNonLiveData(taskId: Int): Task? {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getTaskByIdNonLiveData(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "getTaskByIdNonLiveData failed for id=$taskId", e)
                null
            }
        }
    }

    suspend fun insert(task: Task): OperationResult<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val newRowId = taskDao.insert(task)
                if (newRowId > 0) {
                    OperationResult.Success(newRowId)
                } else {
                    val ex = Exception("DAO insert returned non-positive row ID: $newRowId")
                    Log.e(TAG, "insert failed: $newRowId", ex)
                    OperationResult.Error(ex, "Failed to save task.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during insert", e)
                OperationResult.Error(e, "An error occurred while saving the task.")
            }
        }
    }

    suspend fun update(task: Task): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.update(task)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during update for id=${task.id}", e)
                OperationResult.Error(e, "An error occurred while updating the task.")
            }
        }
    }

    suspend fun delete(task: Task): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.delete(task)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during delete for id=${task.id}", e)
                OperationResult.Error(e, "An error occurred while deleting the task.")
            }
        }
    }

    suspend fun toggleTaskCompletion(taskId: Int): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val task = taskDao.getTaskByIdNonLiveData(taskId)
                if (task != null) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    taskDao.update(updatedTask)
                    OperationResult.Success(Unit)
                } else {
                    val ex = NoSuchElementException("Task with ID $taskId not found for toggle.")
                    Log.w(TAG, "toggleTaskCompletion: task not found for id=$taskId")
                    OperationResult.Error(ex, "Task not found.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during toggleTaskCompletion for id=$taskId", e)
                OperationResult.Error(e, "Failed to update task status.")
            }
        }
    }

    suspend fun deleteCompletedTasks(): OperationResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val numberOfRowsDeleted = taskDao.deleteCompletedTasks()
                OperationResult.Success(numberOfRowsDeleted)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during deleteCompletedTasks", e)
                OperationResult.Error(e, "Failed to delete completed tasks.")
            }
        }
    }

    @Suppress("unused")
    suspend fun getCompletedTasksNonLiveData(): List<Task> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getCompletedTasksNonLiveData()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during getCompletedTasksNonLiveData", e)
                emptyList()
            }
        }
    }

    suspend fun deleteAllTasks(): OperationResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val numberOfRowsDeleted = taskDao.deleteAllTasks()
                OperationResult.Success(numberOfRowsDeleted)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during deleteAllTasks", e)
                OperationResult.Error(e, "Failed to delete all tasks.")
            }
        }
    }

    @Suppress("unused")
    suspend fun getAllTasksNonLiveData(): List<Task> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getAllTasksNonLiveData()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during getAllTasksNonLiveData", e)
                emptyList()
            }
        }
    }

    @Suppress("unused")
    suspend fun getTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getTaskCount()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during getTaskCount", e)
                0
            }
        }
    }

    @Suppress("unused")
    suspend fun getCompletedTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getCompletedTaskCount()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during getCompletedTaskCount", e)
                0
            }
        }
    }

    suspend fun getCompletedTaskIds(): List<Int> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getCompletedTaskIds()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during getCompletedTaskIds", e)
                emptyList()
            }
        }
    }

    suspend fun getAllTaskIds(): List<Int> {
        return withContext(Dispatchers.IO) {
            try {
                taskDao.getAllTaskIds()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during getAllTaskIds", e)
                emptyList()
            }
        }
    }
}
