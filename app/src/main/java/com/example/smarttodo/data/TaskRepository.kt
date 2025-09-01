package com.example.smarttodo.data

import androidx.lifecycle.LiveData
class TaskRepository(private val taskDao: TaskDao) {

    fun getTasks(query: String, isCompleted: Boolean?): LiveData<List<Task>> {
        return taskDao.getTasks(query, isCompleted)
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun insert(task: Task): Long {
        return taskDao.insert(task)
    }

    suspend fun update(task: Task) {
        taskDao.update(task)
    }

    suspend fun delete(task: Task) {
        taskDao.delete(task)
    }

    suspend fun toggleTaskCompletion(taskId: Int) {
        val task = taskDao.getTaskById(taskId)
        task?.let {
            val updatedTask = it.copy(isCompleted = !it.isCompleted)
            taskDao.update(updatedTask)
        }
    }

    suspend fun deleteCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }

    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }

    suspend fun getTaskCount(): Int {
        return taskDao.getTaskCount()
    }

    suspend fun getCompletedTaskCount(): Int {
        return taskDao.getCompletedTaskCount()
    }
}