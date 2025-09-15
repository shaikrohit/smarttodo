package com.example.smarttodo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.utils.PomodoroTimer
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Enum representing the various states of the Pomodoro timer.
 */
enum class TimerState {
    /** The timer is idle, ready to be started, or has been reset. */
    IDLE,
    /** The timer is actively counting down. */
    RUNNING,
    /** The timer has been paused by the user. */
    PAUSED,
    /** The timer has completed its countdown. */
    FINISHED
}

/**
 * ViewModel for managing the Pomodoro timer logic and state for the PomodoroActivity.
 * It interacts with the [TaskRepository] to update task details (e.g., completed Pomodoros)
 * and uses a [PomodoroTimer] instance to handle the actual countdown mechanism.
 *
 * @property repository The [TaskRepository] for data operations.
 */
class PomodoroViewModel(private val repository: TaskRepository) : ViewModel() {

    // Backing property for the current task being worked on.
    private val _task = MutableLiveData<Task?>()
    /** LiveData holding the current [Task] associated with this Pomodoro session, if any. */
    val task: LiveData<Task?> = _task

    // Backing property for the current timer value in milliseconds.
    private val _timerValue = MutableLiveData<Long>()
    /** LiveData holding the current remaining time of the timer in milliseconds. */
    val timerValue: LiveData<Long> = _timerValue

    // Backing property for the current state of the timer.
    private val _timerState = MutableLiveData<TimerState>()
    /** LiveData holding the current [TimerState] of the Pomodoro timer. */
    val timerState: LiveData<TimerState> = _timerState

    // Instance of the PomodoroTimer utility.
    private var pomodoroTimer: PomodoroTimer? = null

    // Backing property for error messages.
    private val _error = MutableLiveData<String?>()
    /** LiveData for exposing error messages to the UI. Null if there's no error. */
    val error: LiveData<String?> = _error

    /**
     * The initial duration for a Pomodoro session in milliseconds.
     * Defaults to 25 minutes.
     */
    val initialDuration: Long = TimeUnit.MINUTES.toMillis(25)

    init {
        // Initialize the timer value to the full duration and state to IDLE.
        _timerValue.value = initialDuration
        _timerState.value = TimerState.IDLE
    }

    /**
     * Loads a specific task by its ID and associates it with this Pomodoro session.
     * If the task is found, it resets the timer to its initial state.
     * If not found, an error message is posted.
     *
     * @param taskId The ID of the task to load.
     */
    fun loadTask(taskId: Int) {
        viewModelScope.launch {
            val loadedTask = repository.getTaskById(taskId)
            if (loadedTask != null) {
                _task.value = loadedTask
                resetTimer() // Reset timer state when a new task context is loaded.
            } else {
                _error.value = "Task not found"
            }
        }
    }

    /**
     * Starts a new Pomodoro timer session.
     * This function is intended to be called when the timer is [TimerState.IDLE] or [TimerState.FINISHED].
     * It resets any existing timer instance and starts a new one with the [initialDuration].
     * The timer state is set to [TimerState.RUNNING].
     */
    fun startTimer() {
        // Only start if the timer is idle or finished to prevent re-starting a running/paused timer.
        if (_timerState.value == TimerState.IDLE || _timerState.value == TimerState.FINISHED) {
            _timerValue.value = initialDuration // Ensure timer value is reset to full duration.

            // Explicitly reset the previous timer instance, if it exists, before creating a new one.
            if (pomodoroTimer != null) {
                pomodoroTimer!!.reset()
            }

            // Create and configure a new PomodoroTimer instance.
            pomodoroTimer = PomodoroTimer(
                millisInFuture = initialDuration,
                onTick = { millisUntilFinished ->
                    // Update LiveData on each tick.
                    _timerValue.value = millisUntilFinished
                },
                onFinish = {
                    // Update state to FINISHED.
                    _timerState.value = TimerState.FINISHED
                    // Increment completed Pomodoros for the current task, if any.
                    viewModelScope.launch {
                        _task.value?.let { currentTask ->
                            val updatedTask = currentTask.copy(completedPomodoros = currentTask.completedPomodoros + 1)
                            repository.update(updatedTask)
                            _task.value = updatedTask // Update LiveData with the modified task.
                        }
                    }
                    // Reset timer display value for the next potential start.
                    _timerValue.value = initialDuration
                }
            )
            pomodoroTimer?.start() // Start the new timer.
            _timerState.value = TimerState.RUNNING // Set state to RUNNING.
        }
    }

    /**
     * Pauses the currently running Pomodoro timer.
     * If the timer is in the [TimerState.RUNNING] state, it will be paused,
     * and its state will be updated to [TimerState.PAUSED].
     * Does nothing if the timer is not currently running.
     */
    fun pauseTimer() {
        if (_timerState.value == TimerState.RUNNING) {
            pomodoroTimer?.pause()
            _timerState.value = TimerState.PAUSED
        }
    }

    /**
     * Resumes a previously paused Pomodoro timer.
     * If the timer is in the [TimerState.PAUSED] state, it will resume counting down,
     * and its state will be updated to [TimerState.RUNNING].
     * Does nothing if the timer is not currently paused.
     */
    fun resumeTimer() {
        if (_timerState.value == TimerState.PAUSED) {
            // The PomodoroTimer's start() method handles resuming from its internally stored millisRemaining.
            pomodoroTimer?.start()
            _timerState.value = TimerState.RUNNING
        }
    }

    /**
     * Resets the Pomodoro timer to its initial state ([TimerState.IDLE]).
     * Any running or paused timer is stopped and reset.
     * The timer value is set back to the [initialDuration].
     */
    fun resetTimer() {
        // Explicitly reset the timer instance if it exists.
        if (pomodoroTimer != null) {
            pomodoroTimer!!.reset()
        }
        // Reset LiveData to initial values.
        _timerValue.value = initialDuration
        _timerState.value = TimerState.IDLE
    }

    /**
     * Called when the ViewModel is about to be destroyed.
     * Ensures that the [PomodoroTimer] is reset (and thus its underlying [CountDownTimer] is cancelled)
     * to prevent leaks or unwanted behavior.
     */
    override fun onCleared() {
        super.onCleared()
        // Safely reset the timer; if pomodoroTimer is null, this does nothing.
        pomodoroTimer?.reset()
    }
}
