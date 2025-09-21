package com.example.smarttodo.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.R
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.util.AlarmScheduler
import com.example.smarttodo.util.Event
import com.example.smarttodo.util.OperationResult
import kotlinx.coroutines.launch

/**
 * Enum representing the different ways tasks can be filtered in the UI.
 */
enum class TaskFilter(val displayName: String) {
    ALL("All"),
    INCOMPLETE("Active"),
    COMPLETED("Completed")
}

/**
 * A data class to represent messages (success or error) to be displayed to the user.
 * Can hold either a string resource ID or a custom string message.
 *
 * @property messageResId The string resource ID for the message. Null if using a custom message.
 * @property customMessage The custom string message. Null if using a resource ID.
 * @property isError True if the message represents an error, false otherwise.
 */
data class UserMessage(
    @StringRes val messageResId: Int? = null,
    val customMessage: String? = null,
    val isError: Boolean = false
) {
    companion object {
        /** Creates a success UserMessage with a string resource. */
        fun success(@StringRes resId: Int): UserMessage {
            return UserMessage(messageResId = resId, isError = false)
        }

        /** Creates an error UserMessage, preferring a custom message if provided, otherwise using a fallback resource. */
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
 * ViewModel for the main task list screen.
 * Handles business logic related to task operations, filtering, and search.
 *
 * @param application The application context, used for services like AlarmScheduler.
 * @param repository The repository for accessing task data.
 */
class TaskViewModel(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModel() {

    private companion object {
        private const val TAG = "TaskViewModel"
    }

    private val _currentFilter = MutableLiveData(TaskFilter.ALL)
    private val _searchQuery = MutableLiveData("")

    // Trigger for observing tasks, combines filter and search query.
    private val _tasksTrigger = MediatorLiveData<Pair<TaskFilter, String>>().apply {
        addSource(_currentFilter) { filter -> value = Pair(filter, _searchQuery.value ?: "") }
        addSource(_searchQuery) { query -> value = Pair(_currentFilter.value ?: TaskFilter.ALL, query) }
    }

    /**
     * LiveData emitting the list of tasks based on the current filter and search query.
     */
    val tasks: LiveData<List<Task>> = _tasksTrigger.switchMap { (filter, query) ->
        Log.d(TAG, "Tasks updated. Filter: ${filter.name}, Query: '$query'")
        val isCompleted = when (filter) {
            TaskFilter.ALL -> null
            TaskFilter.INCOMPLETE -> false
            TaskFilter.COMPLETED -> true
        }
        repository.getTasks(query, isCompleted)
    }

    private val _isLoading = MutableLiveData(false)
    /** LiveData indicating if a background operation (e.g., database access) is in progress. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userMessageEvent = MutableLiveData<Event<UserMessage>>()
    /** LiveData for sending one-time messages (success/error) to the UI. */
    val userMessageEvent: LiveData<Event<UserMessage>> = _userMessageEvent

    /**
     * Inserts a new task into the repository.
     * Schedules a reminder if the task has one.
     * Posts a [UserMessage] event on completion or error.
     */
    fun insert(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        Log.d(TAG, "Attempting to insert task: ${task.title}")
        when (val result = repository.insert(task)) {
            is OperationResult.Success -> {
                val newRowId = result.data // This is the Long ID from Room insert
                val insertedTask = repository.getTaskByIdNonLiveData(newRowId.toInt()) // Assuming ID fits Int
                
                if (insertedTask != null) {
                    Log.i(TAG, "Task fetched successfully post-insert (ID: ${insertedTask.id}, Title: '${insertedTask.title}', Due: ${insertedTask.dueDate}, Reminder: ${insertedTask.hasReminder}). Preparing to schedule reminder.")
                    if (insertedTask.hasReminder && insertedTask.dueDate != null) {
                        Log.i(TAG, "PRE-CALL AlarmScheduler.scheduleReminder for Task ID ${insertedTask.id}: hasReminder=${insertedTask.hasReminder}, dueDate=${insertedTask.dueDate}, TaskObjectInstance=${System.identityHashCode(insertedTask)}")
                        try {
                            AlarmScheduler.scheduleReminder(application, insertedTask)
                            Log.i(TAG, "POST-CALL AlarmScheduler.scheduleReminder for Task ID ${insertedTask.id} - successful call.")
                        } catch (e: Exception) {
                            Log.e(TAG, "CRITICAL EXCEPTION during AlarmScheduler.scheduleReminder call for task ID ${insertedTask.id}", e)
                            _userMessageEvent.value = Event(UserMessage.error("Error during reminder scheduling: ${e.message}"))
                        }
                    } else {
                        Log.d(TAG, "Reminder not scheduled for task ID ${insertedTask.id}: hasReminder=${insertedTask.hasReminder}, dueDate=${insertedTask.dueDate}")
                    }
                } else {
                    Log.e(TAG, "Failed to fetch task (ID: $newRowId) immediately after insert. Reminder might not be scheduled.")
                    _userMessageEvent.value = Event(UserMessage.error(null, R.string.error_scheduling_reminder_after_insert))
                }
                _userMessageEvent.value = Event(UserMessage.success(R.string.task_created))
            }
            is OperationResult.Error -> {
                Log.e(TAG, "Error inserting task: ${result.message}", result.exception)
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    /**
     * Updates an existing task in the repository.
     * Cancels any existing reminder and schedules a new one if needed.
     * Posts a [UserMessage] event on completion or error.
     */
    fun update(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        Log.d(TAG, "Attempting to update task ID: ${task.id}, Title: ${task.title}")
        AlarmScheduler.cancelReminder(application, task.id) 

        when (val result = repository.update(task)) {
            is OperationResult.Success -> {
                Log.d(TAG, "Task ID: ${task.id} updated successfully. Rescheduling reminder if needed.")
                if (task.hasReminder && task.dueDate != null) {
                     try {
                        AlarmScheduler.scheduleReminder(application, task)
                        Log.i(TAG, "POST-CALL AlarmScheduler.scheduleReminder for updated Task ID ${task.id} - successful call.")
                    } catch (e: Exception) {
                        Log.e(TAG, "CRITICAL EXCEPTION during AlarmScheduler.scheduleReminder call for updated task ID ${task.id}", e)
                        _userMessageEvent.value = Event(UserMessage.error("Error during reminder update: ${e.message}"))
                    }
                }
                _userMessageEvent.value = Event(UserMessage.success(R.string.task_updated))
            }
            is OperationResult.Error -> {
                Log.e(TAG, "Error updating task ID: ${task.id}: ${result.message}", result.exception)
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    /**
     * Deletes a task from the repository.
     * Cancels any associated reminder.
     * Posts a [UserMessage] event on completion or error.
     */
    fun delete(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        Log.d(TAG, "Attempting to delete task ID: ${task.id}, Title: ${task.title}")
        when (val result = repository.delete(task)) {
            is OperationResult.Success -> {
                Log.d(TAG, "Task ID: ${task.id} deleted successfully. Cancelling reminder.")
                AlarmScheduler.cancelReminder(application, task.id)
                _userMessageEvent.value = Event(UserMessage.success(R.string.task_deleted))
            }
            is OperationResult.Error -> {
                Log.e(TAG, "Error deleting task ID: ${task.id}: ${result.message}", result.exception)
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    /**
     * Toggles the completion status of a task.
     * Updates reminders accordingly (cancels if completed, schedules if marked incomplete).
     * Posts a [UserMessage] event on completion or error.
     */
    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        _isLoading.value = true
        Log.d(TAG, "Attempting to toggle completion for task ID: ${task.id}, Current isCompleted: ${task.isCompleted}")
        val currentTaskState = repository.getTaskByIdNonLiveData(task.id)
        
        if (currentTaskState != null) {
            val newCompletedState = !currentTaskState.isCompleted
            when (val result = repository.update(currentTaskState.copy(isCompleted = newCompletedState))) { 
                is OperationResult.Success -> {
                    Log.d(TAG, "Task ID: ${currentTaskState.id} completion toggled to $newCompletedState. Updating reminder.")
                    if (newCompletedState) { 
                        AlarmScheduler.cancelReminder(application, currentTaskState.id)
                    } else { 
                        if (currentTaskState.hasReminder && currentTaskState.dueDate != null) {
                            try {
                                AlarmScheduler.scheduleReminder(application, currentTaskState) 
                                Log.i(TAG, "POST-CALL AlarmScheduler.scheduleReminder for toggled Task ID ${currentTaskState.id} - successful call.")
                            } catch (e: Exception) {
                                Log.e(TAG, "CRITICAL EXCEPTION during AlarmScheduler.scheduleReminder call for toggled task ID ${currentTaskState.id}", e)
                                _userMessageEvent.value = Event(UserMessage.error("Error during reminder update for toggle: ${e.message}"))
                            }
                        }
                    }
                    val messageRes = if (newCompletedState) R.string.task_completed else R.string.task_incomplete
                    _userMessageEvent.value = Event(UserMessage.success(messageRes))
                }
                is OperationResult.Error -> {
                    Log.e(TAG, "Error toggling task completion for ID: ${currentTaskState.id}: ${result.message}", result.exception)
                    _userMessageEvent.value = Event(UserMessage.error(result.message))
                }
            }
        } else {
             Log.w(TAG, "Task not found (ID: ${task.id}) for toggling completion. Cannot proceed.")
             _userMessageEvent.value = Event(UserMessage.error(application.getString(R.string.error_task_not_found_for_toggle, task.id)))
        }
        _isLoading.value = false
    }

    /**
     * Deletes all completed tasks from the repository.
     * Cancels reminders for all deleted tasks.
     * Posts a [UserMessage] event on completion or error.
     */
    fun deleteCompletedTasks() = viewModelScope.launch {
        _isLoading.value = true
        Log.d(TAG, "Attempting to delete all completed tasks.")
        val completedTasks = repository.getCompletedTasksNonLiveData() 
        completedTasks.forEach { taskToCancel ->
            AlarmScheduler.cancelReminder(application, taskToCancel.id)
        }
        when (val result = repository.deleteCompletedTasks()) {
            is OperationResult.Success -> {
                Log.d(TAG, "Successfully deleted ${result.data ?: "unknown number of"} completed tasks.")
                _userMessageEvent.value = Event(UserMessage.success(R.string.completed_tasks_deleted))
            }
            is OperationResult.Error -> {
                Log.e(TAG, "Error deleting completed tasks: ${result.message}", result.exception)
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    /**
     * Deletes all tasks from the repository.
     * Cancels reminders for all deleted tasks.
     * Posts a [UserMessage] event on completion or error.
     */
    fun deleteAllTasks() = viewModelScope.launch {
        _isLoading.value = true
        Log.d(TAG, "Attempting to delete all tasks.")
        val allTasks = repository.getAllTasksNonLiveData() 
        allTasks.forEach { taskToCancel ->
            AlarmScheduler.cancelReminder(application, taskToCancel.id)
        }
        when (val result = repository.deleteAllTasks()) {
            is OperationResult.Success -> {
                Log.d(TAG, "Successfully deleted all (${result.data ?: "unknown number of"}) tasks.")
                _userMessageEvent.value = Event(UserMessage.success(R.string.all_tasks_deleted))
            }
            is OperationResult.Error -> {
                Log.e(TAG, "Error deleting all tasks: ${result.message}", result.exception)
                _userMessageEvent.value = Event(UserMessage.error(result.message))
            }
        }
        _isLoading.value = false
    }

    /** Sets the current filter for displaying tasks. */
    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
        Log.d(TAG, "Filter set to: ${filter.name}")
    }

    /** Sets the current search query for filtering tasks. */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        Log.d(TAG, "Search query set to: '$query'")
    }
}
