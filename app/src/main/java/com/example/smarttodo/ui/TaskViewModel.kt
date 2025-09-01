package com.example.smarttodo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

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

    fun insert(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        repository.insert(task)
        _isLoading.value = false
    }

    fun update(task: Task) = viewModelScope.launch {
        repository.update(task)
    }

    fun delete(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        repository.toggleTaskCompletion(task.id)
    }

    fun deleteCompletedTasks() = viewModelScope.launch {
        repository.deleteCompletedTasks()
    }

    fun deleteAllTasks() = viewModelScope.launch {
        _isLoading.value = true
        repository.deleteAllTasks()
        _isLoading.value = false
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

enum class TaskFilter(val displayName: String) {
    ALL("All"),
    INCOMPLETE("Active"),
    COMPLETED("Completed")
}