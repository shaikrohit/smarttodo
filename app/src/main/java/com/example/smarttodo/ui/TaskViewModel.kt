package com.example.smarttodo.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.R
import com.example.smarttodo.data.CategorizedTasks
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.util.AlarmScheduler
import com.example.smarttodo.util.Event
import com.example.smarttodo.util.OperationResult
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class UserMessage(
    @StringRes val messageResId: Int? = null,
    val customMessage: String? = null,
    val isError: Boolean = false
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

class TaskViewModel(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModel() {

    private companion object {
        private const val TAG = "TaskViewModel"
    }

    private val _searchQuery = MutableLiveData("")

    private val tasks: LiveData<List<Task>> = _searchQuery.switchMap { query ->
        repository.getTasks(query, null)
    }

    val categorizedTasks: LiveData<CategorizedTasks> = tasks.map { tasks ->
        categorizeTasks(tasks)
    }

    private fun categorizeTasks(tasks: List<Task>): CategorizedTasks {
        val today = mutableListOf<Task>()
        val tomorrow = mutableListOf<Task>()
        val upcoming = mutableListOf<Task>()
        val completed = mutableListOf<Task>()

        val todayCalendar = Calendar.getInstance()
        val tomorrowCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val taskDateCalendar = Calendar.getInstance()

        for (task in tasks) {
            if (task.isCompleted) {
                completed.add(task)
            } else {
                val dueDate = task.dueDate
                if (dueDate == null) {
                    upcoming.add(task)
                    continue
                }
                taskDateCalendar.time = dueDate

                when {
                    isSameDay(taskDateCalendar, todayCalendar) -> today.add(task)
                    isSameDay(taskDateCalendar, tomorrowCalendar) -> tomorrow.add(task)
                    else -> upcoming.add(task)
                }
            }
        }
        return CategorizedTasks(today, tomorrow, upcoming, completed.sortedByDescending { it.completionDate })
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userMessageEvent = MutableLiveData<Event<UserMessage>>()
    val userMessageEvent: LiveData<Event<UserMessage>> = _userMessageEvent

    fun insert(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        try {
            when (val result = repository.insert(task)) {
                is OperationResult.Success -> {
                    val newRowId = result.data
                    val insertedTask = repository.getTaskByIdNonLiveData(newRowId.toInt())
                    
                    if (insertedTask != null) {
                        if (insertedTask.hasReminder && insertedTask.dueDate != null) {
                            AlarmScheduler.scheduleReminder(application, insertedTask)
                        }
                    }
                    _userMessageEvent.value = Event(UserMessage.success(R.string.task_created))
                }
                is OperationResult.Error -> {
                    _userMessageEvent.value = Event(UserMessage.error(result.message))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun update(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        try {
            AlarmScheduler.cancelReminder(application, task.id)

            when (val result = repository.update(task)) {
                is OperationResult.Success -> {
                    if (task.hasReminder && task.dueDate != null) {
                        AlarmScheduler.scheduleReminder(application, task)
                    }
                    _userMessageEvent.value = Event(UserMessage.success(R.string.task_updated))
                }
                is OperationResult.Error -> {
                    _userMessageEvent.value = Event(UserMessage.error(result.message))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun delete(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        try {
            when (val result = repository.delete(task)) {
                is OperationResult.Success -> {
                    AlarmScheduler.cancelReminder(application, task.id)
                    _userMessageEvent.value = Event(UserMessage.success(R.string.task_deleted))
                }
                is OperationResult.Error -> {
                    _userMessageEvent.value = Event(UserMessage.error(result.message))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val isCompleted = !task.isCompleted
            val updatedTask = task.copy(
                isCompleted = isCompleted,
                completionDate = if (isCompleted) Date() else null
            )
            when (val result = repository.update(updatedTask)) {
                is OperationResult.Success -> {
                    if (updatedTask.isCompleted) {
                        AlarmScheduler.cancelReminder(application, updatedTask.id)
                    } else {
                        if (updatedTask.hasReminder && updatedTask.dueDate != null) {
                            AlarmScheduler.scheduleReminder(application, updatedTask)
                        }
                    }
                    val messageRes = if (updatedTask.isCompleted) R.string.task_completed else R.string.task_incomplete
                    _userMessageEvent.value = Event(UserMessage.success(messageRes))
                }
                is OperationResult.Error -> {
                    _userMessageEvent.value = Event(UserMessage.error(result.message))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteCompletedTasks() = viewModelScope.launch {
        _isLoading.value = true
        try {
            val completedTaskIds = repository.getCompletedTaskIds()
            completedTaskIds.forEach { AlarmScheduler.cancelReminder(application, it) }
            when (val result = repository.deleteCompletedTasks()) {
                is OperationResult.Success -> {
                    _userMessageEvent.value = Event(UserMessage.success(R.string.completed_tasks_deleted))
                }
                is OperationResult.Error -> {
                    _userMessageEvent.value = Event(UserMessage.error(result.message))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteAllTasks() = viewModelScope.launch {
        _isLoading.value = true
        try {
            val allTaskIds = repository.getAllTaskIds()
            allTaskIds.forEach { AlarmScheduler.cancelReminder(application, it) }
            when (val result = repository.deleteAllTasks()) {
                is OperationResult.Success -> {
                    _userMessageEvent.value = Event(UserMessage.success(R.string.all_tasks_deleted))
                }
                is OperationResult.Error -> {
                    _userMessageEvent.value = Event(UserMessage.error(result.message))
                }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
