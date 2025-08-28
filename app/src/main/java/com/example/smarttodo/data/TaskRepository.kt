package com.example.smarttodo.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks()
    val incompleteTasks: LiveData<List<Task>> = taskDao.getIncompleteTasks()
    val completedTasks: LiveData<List<Task>> = taskDao.getCompletedTasks()

    fun searchTasks(query: String): LiveData<List<Task>> = taskDao.searchTasks(query)

    suspend fun getTaskById(taskId: Int): Task? {
        return withContext(Dispatchers.IO) {
            taskDao.getTaskById(taskId)
        }
    }

    suspend fun insert(task: Task): Long {
        return withContext(Dispatchers.IO) {
            taskDao.insert(task)
        }
    }

    suspend fun update(task: Task) {
        withContext(Dispatchers.IO) {
            taskDao.update(task)
        }
    }

    suspend fun delete(task: Task) {
        withContext(Dispatchers.IO) {
            taskDao.delete(task)
        }
    }

    suspend fun toggleTaskCompletion(taskId: Int) {
        withContext(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val updatedTask = it.copy(isCompleted = !it.isCompleted)
                taskDao.update(updatedTask)
            }
        }
    }

    suspend fun deleteCompletedTasks() {
        withContext(Dispatchers.IO) {
            taskDao.deleteCompletedTasks()
        }
    }

    suspend fun deleteAllTasks() {
        withContext(Dispatchers.IO) {
            taskDao.deleteAllTasks()
        }
    }

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