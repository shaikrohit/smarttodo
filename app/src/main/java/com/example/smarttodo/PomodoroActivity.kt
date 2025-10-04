package com.example.smarttodo

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.smarttodo.databinding.ActivityPomodoroBinding
import com.example.smarttodo.ui.PomodoroViewModel
import com.example.smarttodo.ui.PomodoroViewModelFactory
import com.example.smarttodo.ui.TimerState
import java.util.concurrent.TimeUnit

class PomodoroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroBinding
    private val viewModel: PomodoroViewModel by viewModels {
        PomodoroViewModelFactory(application, (application as SmartTodoApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupDurationSelector()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupDurationSelector() {
        ArrayAdapter.createFromResource(
            this,
            R.array.pomodoro_durations,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.durationSelector.adapter = adapter
            binding.durationSelector.setSelection(1) // Default to 25 minutes
        }

        binding.durationSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val durationInMinutes = when (position) {
                    0 -> 15L
                    1 -> 25L
                    2 -> 45L
                    else -> 25L
                }
                viewModel.setTimerDuration(durationInMinutes)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun observeViewModel() {
        viewModel.timerValue.observe(this) { millisUntilFinished ->
            updateTimerText(millisUntilFinished)
            updateProgressBar(millisUntilFinished, viewModel.initialDuration)
        }

        viewModel.timerState.observe(this) { state ->
            updateButton(state)
            binding.durationSelector.isEnabled = state == TimerState.IDLE || state == TimerState.FINISHED
        }
    }

    private fun setupClickListeners() {
        binding.startPauseButton.setOnClickListener {
            when (viewModel.timerState.value) {
                TimerState.RUNNING -> viewModel.pauseTimer()
                else -> viewModel.startTimer()
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
        ObjectAnimator.ofInt(binding.progressBar, "progress", binding.progressBar.progress, progress).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun updateButton(state: TimerState?) {
        val button = binding.startPauseButton
        when (state) {
            TimerState.RUNNING -> {
                button.setImageResource(android.R.drawable.ic_media_pause)
            }
            else -> {
                button.setImageResource(android.R.drawable.ic_media_play)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
