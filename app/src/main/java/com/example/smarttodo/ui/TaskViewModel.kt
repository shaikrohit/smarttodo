package com.example.smarttodo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    private val _currentFilter = MutableLiveData(TaskFilter.ALL)
    private val _searchQuery = MutableLiveData("")

    private val _tasksTrigger = MediatorLiveData<Pair<TaskFilter, String>>().apply {
        addSource(_currentFilter) { filter ->
            value = Pair(filter, _searchQuery.value ?: "")
        }
        addSource(_searchQuery) { query ->
            value = Pair(_currentFilter.value ?: TaskFilter.ALL, query)
        }
    }

    val tasks: LiveData<List<Task>> = _tasksTrigger.switchMap { (filter, query) ->
        val isCompleted = when (filter) {
            TaskFilter.ALL -> null
            TaskFilter.INCOMPLETE -> false
            TaskFilter.COMPLETED -> true
        }
        repository.getTasks(query, isCompleted)
    }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = SingleLiveEvent<String>()
    val error: LiveData<String> = _error

    fun insert(task: Task) = viewModelScope.launch {
        try {
            _isLoading.value = true
            repository.insert(task)
        } catch (e: Exception) {
            _error.value = "Failed to insert task"
        } finally {
            _isLoading.value = false
        }
    }

    fun update(task: Task) = viewModelScope.launch {
        try {
            repository.update(task)
        } catch (e: Exception) {
            _error.value = "Failed to update task"
        }
    }

    fun delete(task: Task) = viewModelScope.launch {
        try {
            repository.delete(task)
        } catch (e: Exception) {
            _error.value = "Failed to delete task"
        }
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        try {
            repository.toggleTaskCompletion(task.id)
        } catch (e: Exception) {
            _error.value = "Failed to toggle task completion"
        }
    }

    fun deleteCompletedTasks() = viewModelScope.launch {
        try {
            repository.deleteCompletedTasks()
        } catch (e: Exception) {
            _error.value = "Failed to delete completed tasks"
        }
    }

    fun deleteAllTasks() = viewModelScope.launch {
        try {
            _isLoading.value = true
            repository.deleteAllTasks()
        } catch (e: Exception) {
            _error.value = "Failed to delete all tasks"
        } finally {
            _isLoading.value = false
        }
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}