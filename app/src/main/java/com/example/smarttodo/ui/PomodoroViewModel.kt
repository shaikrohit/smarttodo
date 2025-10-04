package com.example.smarttodo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.util.AlarmScheduler
import com.example.smarttodo.utils.PomodoroTimer
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

class PomodoroViewModel(application: Application, private val repository: TaskRepository) : AndroidViewModel(application) {

    private val _timerValue = MutableLiveData<Long>()
    val timerValue: LiveData<Long> = _timerValue

    private val _timerState = MutableLiveData<TimerState>()
    val timerState: LiveData<TimerState> = _timerState

    private var pomodoroTimer: PomodoroTimer? = null

    private var _initialDuration: Long = TimeUnit.MINUTES.toMillis(25)
    val initialDuration: Long
        get() = _initialDuration

    init {
        _timerValue.value = _initialDuration
        _timerState.value = TimerState.IDLE
    }

    fun setTimerDuration(durationInMinutes: Long) {
        _initialDuration = TimeUnit.MINUTES.toMillis(durationInMinutes)
        if (_timerState.value == TimerState.IDLE || _timerState.value == TimerState.FINISHED) {
            _timerValue.value = _initialDuration
        }
    }

    fun startTimer() {
        val currentState = _timerState.value
        if (currentState == TimerState.RUNNING) return

        val startTime = if (currentState == TimerState.PAUSED) _timerValue.value ?: _initialDuration else _initialDuration

        _timerState.value = TimerState.RUNNING
        pomodoroTimer?.reset()
        pomodoroTimer = PomodoroTimer(
            millisInFuture = startTime,
            onTick = { millisUntilFinished ->
                _timerValue.postValue(millisUntilFinished)
            },
            onFinish = {
                _timerState.postValue(TimerState.FINISHED)
                _timerValue.postValue(_initialDuration)
                // Vibrate on completion
                AlarmScheduler.vibrate(getApplication<Application>().applicationContext)
            }
        )
        pomodoroTimer?.start()
    }

    fun pauseTimer() {
        if (_timerState.value == TimerState.RUNNING) {
            pomodoroTimer?.pause()
            _timerState.value = TimerState.PAUSED
        }
    }

    fun resetTimer() {
        pomodoroTimer?.reset()
        _timerState.value = TimerState.IDLE
        _timerValue.value = _initialDuration
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroTimer?.reset()
    }
}
