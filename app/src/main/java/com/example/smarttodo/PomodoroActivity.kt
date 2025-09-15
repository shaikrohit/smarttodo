package com.example.smarttodo

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.smarttodo.databinding.ActivityPomodoroBinding
import com.example.smarttodo.ui.PomodoroViewModel
import com.example.smarttodo.ui.PomodoroViewModelFactory
import com.example.smarttodo.ui.TimerState // Ensure this is imported for TimerState usage
import java.util.concurrent.TimeUnit

/**
 * Activity responsible for displaying and managing the Pomodoro timer UI.
 * It interacts with [PomodoroViewModel] to handle the timer's logic, state changes,
 * and updates to the UI elements like the timer text, progress bar, and control buttons.
 */
class PomodoroActivity : AppCompatActivity() {

    // ViewBinding instance for accessing views in activity_pomodoro.xml.
    // Lazily initialized and should not be accessed before setContentView.
    private lateinit var binding: ActivityPomodoroBinding

    // ViewModel instance for this Activity, obtained using a ViewModelFactory.
    // The factory ensures that the PomodoroViewModel is created with its required TaskRepository dependency.
    private val viewModel: PomodoroViewModel by viewModels {
        // Assumes the Application class is SmartTodoApplication and provides the repository.
        PomodoroViewModelFactory((application as SmartTodoApplication).repository)
    }

    /**
     * Called when the activity is first created.
     * This method initializes the activity, inflates its layout using ViewBinding,
     * sets up the ViewModel, and calls methods to observe LiveData from the ViewModel
     * and to set up click listeners for UI interactions.
     *
     * Note: Includes commented-out logic for potentially loading a task ID from the intent
     * to associate a specific task with the Pomodoro session. This is currently inactive.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied in [onSaveInstanceState]. Otherwise, it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using ViewBinding and set it as the content view.
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Commented-out: Example logic for loading a specific task into the Pomodoro timer.
        // This would typically involve passing a "TASK_ID" via an Intent when starting this Activity.
        // val taskId = intent.getIntExtra("TASK_ID", -1) // -1 or another invalid ID as default.
        // if (taskId != -1) {
        //     viewModel.loadTask(taskId) // Load the specific task.
        // } else {
        //     // If no specific task ID is provided, or for a general timer,
        //     // ensure the timer is in a known default state.
        //     viewModel.resetTimer()
        // }

        observeViewModel()    // Set up observers for LiveData from the ViewModel.
        setupClickListeners() // Set up click listeners for UI elements.
    }

    /**
     * Sets up observers on [LiveData] exposed by the [PomodoroViewModel].
     * This method is responsible for reacting to changes in the timer's state,
     * the current task, timer value, and any errors, and then updating the UI accordingly.
     */
    private fun observeViewModel() {
        // Observe changes to the associated task.
        viewModel.task.observe(this) { task ->
            if (task != null) {
                // If a task is loaded, set its title as the ActionBar title.
                supportActionBar?.title = task.title
            } else {
                // Otherwise, use a default title for the Pomodoro timer.
                supportActionBar?.title = getString(R.string.pomodoro_timer_default_title) // Assuming you have this string
            }
        }

        // Observe changes to the timer's remaining time.
        viewModel.timerValue.observe(this) { millisUntilFinished ->
            updateTimerText(millisUntilFinished)
            // Update the progress bar based on the remaining time and the total initial duration from the ViewModel.
            updateProgressBar(millisUntilFinished, viewModel.initialDuration)
        }

        // Observe changes to the timer's state (e.g., IDLE, RUNNING, PAUSED, FINISHED).
        viewModel.timerState.observe(this) { state ->
            updateButton(state) // Update button text and behavior based on the current state.
        }

        // Observe any error messages posted by the ViewModel.
        viewModel.error.observe(this) { error ->
            if (error != null) {
                // Display a short Toast message if an error occurs.
                android.widget.Toast.makeText(this, error, android.widget.Toast.LENGTH_SHORT).show()
                // Consider clearing the error in ViewModel after displaying, if appropriate:
                // viewModel.clearError()
            }
        }
    }

    /**
     * Sets up click listeners for interactive UI elements, primarily the start/pause/resume button.
     * Actions are delegated to the [PomodoroViewModel] based on the current timer state.
     */
    private fun setupClickListeners() {
        binding.startPauseButton.setOnClickListener {
            // Determine the action based on the current timer state.
            when (viewModel.timerState.value) {
                TimerState.RUNNING -> viewModel.pauseTimer()    // If running, pause the timer.
                TimerState.PAUSED -> viewModel.resumeTimer()   // If paused, resume the timer.
                TimerState.IDLE, TimerState.FINISHED -> viewModel.startTimer() // If idle or finished, start a new timer.
                null -> viewModel.startTimer() // Default action if state is unexpectedly null (should ideally not happen).
            }
        }

        // Example: If a reset button were added in the layout (e.g., binding.resetButton):
        // binding.resetButton.setOnClickListener {
        //     viewModel.resetTimer()
        // }
    }

    /**
     * Updates the timer [TextView] to display the time in MM:SS format.
     * @param millisUntilFinished The remaining time in milliseconds.
     */
    private fun updateTimerText(millisUntilFinished: Long) {
        // Calculate minutes and seconds from the total milliseconds.
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes)
        // Format the time as a two-digit minutes and two-digit seconds string.
        binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Updates the [ProgressBar]'s progress based on the remaining time.
     * @param millisUntilFinished The remaining time in milliseconds.
     * @param totalTime The total duration of the timer in milliseconds (used as the 100% mark).
     */
    private fun updateProgressBar(millisUntilFinished: Long, totalTime: Long) {
        if (totalTime > 0) { // Ensure totalTime is positive to avoid division by zero or negative progress.
            // Calculate progress as a percentage.
            val progress = (millisUntilFinished.toFloat() / totalTime.toFloat() * 100).toInt()
            binding.progressBar.progress = progress
        } else {
            // If totalTime is not positive, default to a full or empty progress bar,
            // depending on desired behavior for an invalid state. Here, showing it as full.
            binding.progressBar.progress = 100
        }
    }

    /**
     * Updates the text of the main control button (e.g., "Start", "Pause", "Resume")
     * based on the current [TimerState].
     * @param state The current [TimerState] of the timer. Can be null if LiveData hasn't emitted yet.
     */
    private fun updateButton(state: TimerState?) {
        // Set button text based on the timer's current state.
        when (state) {
            TimerState.RUNNING -> binding.startPauseButton.text = getString(R.string.button_pause) // "Pause"
            TimerState.PAUSED -> binding.startPauseButton.text = getString(R.string.button_resume) // "Resume"
            TimerState.IDLE, TimerState.FINISHED -> binding.startPauseButton.text = getString(R.string.button_start) // "Start"
            null -> binding.startPauseButton.text = getString(R.string.button_start) // Default to "Start" if state is null.
        }
    }
}
