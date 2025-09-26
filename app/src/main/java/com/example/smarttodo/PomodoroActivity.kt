package com.example.smarttodo

import android.animation.ObjectAnimator
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.example.smarttodo.databinding.ActivityPomodoroBinding
import com.example.smarttodo.ui.PomodoroViewModel
import com.example.smarttodo.ui.PomodoroViewModelFactory
import com.example.smarttodo.ui.TimerState
import java.util.concurrent.TimeUnit

class PomodoroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroBinding
    private val viewModel: PomodoroViewModel by viewModels {
        PomodoroViewModelFactory((application as SmartTodoApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewModel.task.observe(this) { task ->
            supportActionBar?.title = task?.title ?: getString(R.string.pomodoro_timer_default_title)
        }

        viewModel.timerValue.observe(this) { millisUntilFinished ->
            updateTimerText(millisUntilFinished)
            updateProgressBar(millisUntilFinished, viewModel.initialDuration)
        }

        viewModel.timerState.observe(this) { state ->
            updateButton(state)
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                android.widget.Toast.makeText(this, error, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.startPauseButton.setOnClickListener {
            when (viewModel.timerState.value) {
                TimerState.RUNNING -> viewModel.pauseTimer()
                TimerState.PAUSED -> viewModel.resumeTimer()
                TimerState.IDLE, TimerState.FINISHED -> viewModel.startTimer()
                null -> viewModel.startTimer()
            }
        }

        binding.resetButton.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_out))
            viewModel.resetTimer()
        }
    }

    private fun updateTimerText(millisUntilFinished: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes)
        binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgressBar(millisUntilFinished: Long, totalTime: Long) {
        val progress = if (totalTime > 0) {
            (millisUntilFinished.toFloat() / totalTime.toFloat() * 100).toInt()
        } else {
            100
        }
        val animator = ObjectAnimator.ofInt(binding.progressBar, "progress", binding.progressBar.progress, progress)
        animator.duration = 1000 // Animate over 1 second
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    private fun updateButton(state: TimerState?) {
        val button = binding.startPauseButton
        val drawableRes = when (state) {
            TimerState.RUNNING -> R.drawable.avd_play_to_pause
            TimerState.PAUSED -> R.drawable.avd_pause_to_play
            else -> R.drawable.ic_play
        }
        button.setImageResource(drawableRes)
        val drawable = button.drawable
        if (drawable is AnimatedVectorDrawable) {
            drawable.start()
        } else if (drawable is AnimatedVectorDrawableCompat) {
            drawable.start()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
