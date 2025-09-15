package com.example.smarttodo.ui

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.R // For string resources
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.util.Event
import com.example.smarttodo.util.OperationResult
import kotlinx.coroutines.launch

/**
 * Enum representing the available filter options for the task list.
 */
enum class TaskFilter(val displayName: String) {
    ALL("All"),
    INCOMPLETE("Active"),
    COMPLETED("Completed")
}

/**
 * Represents a message to be displayed to the user, typically as a one-time event.
 */
data class UserMessage(
    @StringRes val messageResId: Int? = null,
    val customMessage: String? = null,
    val isError: Boolean = false
    // We can add actionTextResId and onAction later if needed for Snackbars
) {
    companion object {
        fun success(@StringRes resId: Int): UserMessage {
            return UserMessage(messageResId = resId, isError = false)
        }

        fun error(errorMessage: String?, @StringRes fallbackResId: Int = R.string.error_generic): UserMessage {
            return if (errorMessage != null) {
                UserMessage(customMessage = errorMessage, isError = true)
            } else {
                UserMessage(messageResId = fallbackResId, isError = true)
            }
        }
    }
}

/**
 * ViewModel for managing tasks.
 */
class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _currentFilter = MutableLiveData(TaskFilter.ALL)
    private val _searchQuery = MutableLiveData("")

    private val _tasksTrigger = MediatorLiveData<Pair<TaskFilter, String>>().apply {
        addSource(_currentFilter) { filter -> value = Pair(filter, _searchQuery.value ?: "") }
        addSource(_searchQuery) { query -> value = Pair(_currentFilter.value ?: TaskFilter.ALL, query) }
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

    // LiveData for sending one-time messages to the UI
    private val _userMessageEvent = MutableLiveData<Event<UserMessage>>()
    val userMessageEvent: LiveData<Event<UserMessage>> = _userMessageEvent

    fun insert(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        when (val result = repository.insert(task)) {
            is OperationResult.Success -> {
                _userMessageEvent.value = Event(UserMessage.success(R.string.task_created))
            }
            is OperationResult.Error -> {
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    fun update(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        when (val result = repository.update(task)) {
            is OperationResult.Success -> {
                _userMessageEvent.value = Event(UserMessage.success(R.string.task_updated))
            }
            is OperationResult.Error -> {
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    fun delete(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        when (val result = repository.delete(task)) {
            is OperationResult.Success -> {
                _userMessageEvent.value = Event(UserMessage.success(R.string.task_deleted))
            }
            is OperationResult.Error -> {
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        when (val result = repository.toggleTaskCompletion(task.id)) {
            is OperationResult.Success -> {
                // Determine if task was marked complete or incomplete for the message
                val messageRes = if (!task.isCompleted) R.string.task_completed else R.string.task_incomplete
                _userMessageEvent.value = Event(UserMessage.success(messageRes))
            }
            is OperationResult.Error -> {
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    fun deleteCompletedTasks() = viewModelScope.launch {
        _isLoading.value = true
        when (val result = repository.deleteCompletedTasks()) {
            is OperationResult.Success -> {
                _userMessageEvent.value = Event(UserMessage.success(R.string.completed_tasks_deleted))
            }
            is OperationResult.Error -> {
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    fun deleteAllTasks() = viewModelScope.launch {
        _isLoading.value = true
        when (val result = repository.deleteAllTasks()) {
            is OperationResult.Success -> {
                _userMessageEvent.value = Event(UserMessage.success(R.string.all_tasks_deleted))
            }
            is OperationResult.Error -> {
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
