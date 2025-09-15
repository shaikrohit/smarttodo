package com.example.smarttodo.utils

import android.os.CountDownTimer
import android.util.Log

/**
 * A wrapper around [CountDownTimer] to provide Pomodoro timer functionality.
 * This timer ticks every second by default and allows for starting, pausing, and resetting.
 *
 * @property millisInFuture The total time in milliseconds for the countdown. Must be positive.
 * @param onTick A lambda function that is invoked every second the timer ticks.
 *               It receives the remaining time in milliseconds.
 * @param onFinish A lambda function that is invoked when the timer finishes.
 */
class PomodoroTimer(
    private val millisInFuture: Long,
    private val onTick: (millisUntilFinished: Long) -> Unit,
    private val onFinish: () -> Unit
) {
    private var countDownTimer: CountDownTimer? = null
    /**
     * Stores the remaining time in milliseconds. This value is updated on each tick
     * when the timer is running and is used to resume the timer from where it left off.
     * It is initialized with [millisInFuture] and reset to it when [reset] is called.
     */
    private var millisRemaining: Long = millisInFuture

    init {
        // Ensure that the initial duration is a positive value, as required by CountDownTimer.
        require(millisInFuture > 0) { "millisInFuture must be a positive value." }
    }

    /**
     * Starts or resumes the countdown timer.
     * If the timer has never been started, or was previously [reset], it starts a new countdown
     * from the initial `millisInFuture` (or the current `millisRemaining` if it was reset).
     * If the timer was [pause]d, it resumes the countdown from the last known `millisRemaining` time.
     * This method ensures that only one underlying [CountDownTimer] instance is active at a time for this [PomodoroTimer].
     * If `start()` is called while a timer is already actively running (i.e., `countDownTimer` is not null
     * and [pause] or [reset] hasn't been called), it will not create a new timer instance.
     */
    fun start() {
        Log.d("PomodoroTimer", "start() called. millisRemaining: $millisRemaining, Current countDownTimer: $countDownTimer")
        // Only create a new CountDownTimer if one isn't already running or if it was paused (which nullifies countDownTimer).
        if (countDownTimer == null) {
            Log.d("PomodoroTimer", "Creating and starting new CountDownTimer for $millisRemaining ms.")
            // Create a new CountDownTimer instance with the current millisRemaining and a 1-second interval.
            countDownTimer = object : CountDownTimer(millisRemaining, 1000L) { // Use 1000L for explicit Long
                /**
                 * Callback fired on regular interval (every 1000ms).
                 * @param millisUntilFinished The amount of time in milliseconds until the timer is finished.
                 */
                override fun onTick(millisUntilFinished: Long) {
                    Log.d("PomodoroTimer", "Internal CountDownTimer onTick: $millisUntilFinished ms remaining.")
                    millisRemaining = millisUntilFinished // Update remaining time
                    // Propagate the tick event to the observer lambda.
                    this@PomodoroTimer.onTick(millisUntilFinished)
                }

                /**
                 * Callback fired when the time is up and the countdown timer finishes.
                 */
                override fun onFinish() {
                    Log.d("PomodoroTimer", "Internal CountDownTimer onFinish.")
                    // Propagate the finish event to the observer lambda.
                    this@PomodoroTimer.onFinish()
                    // Reset the timer's state automatically for potential reuse.
                    reset()
                }
            }.start() // Start the newly created CountDownTimer.
        } else {
            Log.d("PomodoroTimer", "start() called but countDownTimer already exists (likely already running or not properly nulled after pause/reset). Not starting a new one.")
        }
    }

    /**
     * Pauses the currently running countdown timer.
     * The remaining time is saved internally, so the timer can be resumed from this point using [start].
     * If no timer is running (i.e., `countDownTimer` is null), this method does nothing beyond logging.
     */
    fun pause() {
        Log.d("PomodoroTimer", "pause() called. Current countDownTimer: $countDownTimer. millisRemaining: $millisRemaining")
        countDownTimer?.cancel() // Safely cancel the timer if it exists.
        countDownTimer = null // Nullify to allow a new timer to be created on next call to start(), using the saved millisRemaining.
        Log.d("PomodoroTimer", "pause() completed. countDownTimer is now null.")
    }

    /**
     * Resets the countdown timer to its initial state.
     * If a timer is running, it is cancelled. The remaining time is reset to the original `millisInFuture`.
     * The timer will be in a non-running state (countDownTimer will be null), ready to be started again from the full duration.
     */
    fun reset() {
        Log.d("PomodoroTimer", "reset() called. Current countDownTimer: $countDownTimer")
        countDownTimer?.cancel() // Safely cancel the timer if it exists.
        countDownTimer = null // Nullify the current timer instance.
        millisRemaining = millisInFuture // Reset remaining time to the initial full duration.
        Log.d("PomodoroTimer", "reset() completed. millisRemaining set to: $millisRemaining. countDownTimer is now null.")
    }
}
