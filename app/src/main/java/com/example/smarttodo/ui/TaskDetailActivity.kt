@file:Suppress("unused", "UNUSED_PARAMETER")

package com.example.smarttodo.ui

import android.os.Bundle
import android.transition.Fade
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.smarttodo.R
import com.example.smarttodo.data.Priority
import com.example.smarttodo.data.Task
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    private var taskId: Int = -1
    private var task: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Use a platform fade transition for shared element transitions
        window.sharedElementEnterTransition = Fade().apply { duration = 320L }
        window.sharedElementReturnTransition = Fade().apply { duration = 320L }
        // Postpone the enter transition until the task is loaded
        postponeEnterTransition()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        // Toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Extract intent extras
        taskId = intent.getIntExtra("task_id", -1)

        // Shared element transition name (optional)
        val transitionName = intent.getStringExtra("transition_name")
        findViewById<View>(R.id.task_detail_card)?.transitionName = transitionName

        if (taskId == -1) {
            // No valid task id; show an error and finish
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error_generic)
                .setMessage(getString(R.string.error_task_not_found_for_toggle))
                .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                .setOnCancelListener { finish() }
                .show()
            return
        }

        loadTaskAndPopulate()

        // Mark complete button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonMarkComplete).setOnClickListener {
            task?.let { t ->
                toggleCompleteAndUpdate(t)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadTaskAndPopulate() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repo = (application as com.example.smarttodo.SmartTodoApplication).repository
            val loaded = repo.getTaskById(taskId)
            task = loaded
            launch(Dispatchers.Main) {
                if (loaded == null) {
                    MaterialAlertDialogBuilder(this@TaskDetailActivity)
                        .setTitle(R.string.error_generic)
                        .setMessage(getString(R.string.error_task_not_found_for_toggle))
                        .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                        .show()
                    return@launch
                }
                populateViews(loaded)
                // Start the postponed transition now that the data is loaded
                startPostponedEnterTransition()
            }
        }
    }

    private fun populateViews(t: Task) {
        findViewById<TextView>(R.id.textViewTitle).text = t.title
        val descView = findViewById<TextView>(R.id.textViewDescription)
        if (t.description.isNotEmpty()) {
            descView.text = t.description
            descView.visibility = View.VISIBLE
        } else descView.visibility = View.GONE

        val dueView = findViewById<TextView>(R.id.textDetailDue)
        t.dueDate?.let { date ->
            val fmt = try {
                SimpleDateFormat(getString(R.string.date_time_format_display), Locale.getDefault())
            } catch (e: Exception) {
                // Log the exception so it's not unused and we have traceability
                android.util.Log.w("TaskDetailActivity", "Failed to load date format resource, using fallback", e)
                SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            }
            dueView.text = getString(R.string.due_date_prefix_display, fmt.format(date))
            dueView.visibility = View.VISIBLE
        } ?: run { dueView.visibility = View.GONE }

        val chip = findViewById<Chip>(R.id.chipPriority)
        chip.text = when (t.priority) {
            Priority.HIGH -> getString(R.string.priority_high)
            Priority.MEDIUM -> getString(R.string.priority_medium)
            Priority.LOW -> getString(R.string.priority_low)
        }
        val colorRes = when (t.priority) {
            Priority.HIGH -> R.color.priority_high
            Priority.MEDIUM -> R.color.priority_medium
            Priority.LOW -> R.color.priority_low
        }
        chip.chipBackgroundColor = ContextCompat.getColorStateList(this, colorRes)

        // Update button text
        val btn = findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonMarkComplete)
        btn.text = if (t.isCompleted) getString(R.string.task_incomplete) else getString(R.string.mark_complete)
    }

    private fun toggleCompleteAndUpdate(t: Task) {
        lifecycleScope.launch(Dispatchers.IO) {
            val repo = (application as com.example.smarttodo.SmartTodoApplication).repository
            val updated = t.copy(isCompleted = !t.isCompleted, completionDate = if (!t.isCompleted) Date() else null)
            repo.update(updated)
            task = updated // Update the local task object
            launch(Dispatchers.Main) {
                // Re-populate the views to reflect the change
                populateViews(updated)
            }
        }
    }
}
