package com.example.smarttodo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.data.TaskRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing task data and UI state
 * Part of MVVM architecture pattern
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    // LiveData for observing tasks
    val allTasks: LiveData<List<Task>>
    val incompleteTasks: LiveData<List<Task>>
    val completedTasks: LiveData<List<Task>>

    // Current filter state
    private val _currentFilter = MutableLiveData<TaskFilter>(TaskFilter.ALL)
    val currentFilter: LiveData<TaskFilter> = _currentFilter

    // Search query
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val taskDao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        allTasks = repository.allTasks
        incompleteTasks = repository.incompleteTasks
        completedTasks = repository.completedTasks
    }

    /**
     * Insert a new task
     */
    fun insert(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        repository.insert(task)
        _isLoading.value = false
    }

    /**
     * Update an existing task
     */
    fun update(task: Task) = viewModelScope.launch {
        repository.update(task)
    }

    /**
     * Delete a task
     */
    fun delete(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    /**
     * Toggle task completion
     */
    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        repository.toggleTaskCompletion(task)
    }

    /**
     * Delete all completed tasks
     */
    fun deleteCompletedTasks() = viewModelScope.launch {
        repository.deleteCompletedTasks()
    }

    /**
     * Delete all tasks (completed and incomplete)
     */
    fun deleteAllTasks() = viewModelScope.launch {
        _isLoading.value = true
        repository.deleteAllTasks()
        _isLoading.value = false
    }

    /**
     * Set current filter
     */
    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Get current tasks based on filter
     */
    fun getCurrentTasks(): LiveData<List<Task>> {
        return when (_currentFilter.value) {
            TaskFilter.ALL -> allTasks
            TaskFilter.INCOMPLETE -> incompleteTasks
            TaskFilter.COMPLETED -> completedTasks
            else -> allTasks
        }
    }

    /**
     * Search tasks
     */
    fun searchTasks(query: String): LiveData<List<Task>> {
        return repository.searchTasks(query)
    }
}

/**
 * Enum for task filtering options
 */
enum class TaskFilter(val displayName: String) {
    ALL("All"),
    INCOMPLETE("Active"),
    COMPLETED("Completed")
}