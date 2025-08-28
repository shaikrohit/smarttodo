package com.example.smarttodo.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository(private val taskDao: TaskDao) {

    // Observe all tasks
    fun getAllTasks(): LiveData<List<Task>> = taskDao.getAllTasks()

    // Observe incomplete tasks
    fun getIncompleteTasks(): LiveData<List<Task>> = taskDao.getIncompleteTasks()

    // Observe completed tasks
    fun getCompletedTasks(): LiveData<List<Task>> = taskDao.getCompletedTasks()

    // Search tasks
    fun searchTasks(query: String): LiveData<List<Task>> = taskDao.searchTasks(query)

    // Get single task
    suspend fun getTaskById(taskId: Int): Task? {
        return withContext(Dispatchers.IO) {
            taskDao.getTaskById(taskId)
        }
    }

    // Insert task
    suspend fun insertTask(task: Task): Long {
        return withContext(Dispatchers.IO) {
            taskDao.insert(task)
        }
    }

    // Update task
    suspend fun updateTask(task: Task) {
        withContext(Dispatchers.IO) {
            taskDao.update(task)
        }
    }

    // Delete task
    suspend fun deleteTask(task: Task) {
        withContext(Dispatchers.IO) {
            taskDao.delete(task)
        }
    }

    // Delete completed tasks
    suspend fun deleteCompletedTasks() {
        withContext(Dispatchers.IO) {
            taskDao.deleteCompletedTasks()
        }
    }

    // Delete all tasks
    suspend fun deleteAllTasks() {
        withContext(Dispatchers.IO) {
            taskDao.deleteAllTasks()
        }
    }

    // Get statistics
    suspend fun getTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            taskDao.getTaskCount()
        }
    }

    suspend fun getCompletedTaskCount(): Int {
        return withContext(Dispatchers.IO) {
            taskDao.getCompletedTaskCount()
        }
    }
}