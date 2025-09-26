package com.example.smarttodo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smarttodo.databinding.ActivityTaskDetailBinding

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val title = intent.getStringExtra("task_title")
        val description = intent.getStringExtra("task_description")

        binding.textViewTitle.text = title
        binding.textViewDescription.text = description
        supportActionBar?.title = title

        // Set the transition name dynamically
        val transitionName = intent.getStringExtra("transition_name")
        binding.taskDetailCard.transitionName = transitionName
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
