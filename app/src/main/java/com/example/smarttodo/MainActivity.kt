package com.example.smarttodo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttodo.data.Task
import com.example.smarttodo.databinding.ActivityMainBinding
import com.example.smarttodo.ui.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * Main activity for the Smart To-Do app
 * Handles the main UI and user interactions
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupTabs()
        setupSearch()
        setupSwipeRefresh()
        observeViewModel()

        // Apply saved theme
        applySavedTheme()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Smart To-Do"
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task -> editTask(task) },
            onTaskLongClick = { task -> showTaskOptions(task) },
            onCompleteClick = { task -> toggleTaskCompletion(task) }
        )

        binding.recyclerViewTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)

            // Add item decoration for spacing
            addItemDecoration(TaskItemDecoration(resources.getDimensionPixelSize(R.dimen.task_item_spacing)))
        }

        // Setup swipe gestures
        setupSwipeGestures()
    }

    private fun setupSwipeGestures() {
        val swipeGestureHelper = SwipeGestureHelper(
            onSwipeRight = { position ->
                val task = taskAdapter.getTaskAt(position)
                toggleTaskCompletion(task)
            },
            onSwipeLeft = { position ->
                val task = taskAdapter.getTaskAt(position)
                showDeleteConfirmation(task)
            }
        )

        val itemTouchHelper = ItemTouchHelper(swipeGestureHelper)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> taskViewModel.setFilter(TaskFilter.ALL)
                    1 -> taskViewModel.setFilter(TaskFilter.INCOMPLETE)
                    2 -> taskViewModel.setFilter(TaskFilter.COMPLETED)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isEmpty()) {
                // Show filtered tasks based on current tab
                observeCurrentTasks()
            } else {
                // Show search results
                taskViewModel.searchTasks(query).observe(this) { tasks ->
                    taskAdapter.submitList(tasks)
                    updateEmptyState(tasks.isEmpty())
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Refresh data (in this case, just stop the loading animation)
            binding.swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, "Tasks refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Observe loading state
        taskViewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        // Observe current filter and show appropriate tasks
        taskViewModel.currentFilter.observe(this) {
            observeCurrentTasks()
        }

        // Initial load
        observeCurrentTasks()
    }

    private fun observeCurrentTasks() {
        // Remove previous observers to prevent multiple subscriptions
        taskViewModel.allTasks.removeObservers(this)
        taskViewModel.incompleteTasks.removeObservers(this)
        taskViewModel.completedTasks.removeObservers(this)

        // Observe the current task list based on filter
        taskViewModel.getCurrentTasks().observe(this) { tasks ->
            taskAdapter.submitList(tasks)
            updateEmptyState(tasks.isEmpty())
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewTasks.visibility = android.view.View.GONE
            binding.emptyStateLayout.visibility = android.view.View.VISIBLE
        } else {
            binding.recyclerViewTasks.visibility = android.view.View.VISIBLE
            binding.emptyStateLayout.visibility = android.view.View.GONE
        }
    }

    private fun showAddTaskDialog() {
        val dialog = AddTaskDialogFragment()
        dialog.show(supportFragmentManager, "AddTaskDialog")
    }

    private fun editTask(task: Task) {
        val dialog = AddTaskDialogFragment.newInstance(task)
        dialog.show(supportFragmentManager, "EditTaskDialog")
    }

    private fun toggleTaskCompletion(task: Task) {
        taskViewModel.toggleTaskCompletion(task)

        // Show completion feedback
        val message = if (task.isCompleted) "Task marked as incomplete" else "Task completed! 🎉"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showTaskOptions(task: Task) {
        val options = arrayOf("Edit", "Delete", "Duplicate")

        MaterialAlertDialogBuilder(this)
            .setTitle(task.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editTask(task)
                    1 -> showDeleteConfirmation(task)
                    2 -> duplicateTask(task)
                }
            }
            .show()
    }

    private fun showDeleteConfirmation(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                taskViewModel.delete(task)
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun duplicateTask(task: Task) {
        val duplicatedTask = task.copy(
            id = 0, // New ID will be auto-generated
            title = "${task.title} (Copy)",
            isCompleted = false,
            createdAt = java.util.Date()
        )
        taskViewModel.insert(duplicatedTask)
        Toast.makeText(this, "Task duplicated", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme_toggle -> {
                toggleTheme()
                true
            }
            R.id.action_delete_completed -> {
                showDeleteCompletedConfirmation()
                true
            }
            R.id.action_delete_all -> {
                showDeleteAllConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }

        // Save theme preference
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit().putInt("theme_mode", newMode).apply()

        // Apply new theme
        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
    }

    private fun showDeleteCompletedConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Completed Tasks")
            .setMessage("Are you sure you want to delete all completed tasks?")
            .setPositiveButton("Delete") { _, _ ->
                taskViewModel.deleteCompletedTasks()
                Toast.makeText(this, "Completed tasks deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAllConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete All Tasks")
            .setMessage("Are you sure you want to delete ALL tasks? This cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                lifecycleScope.launch {
                    // This is a destructive action, so we make it harder to access
                    taskViewModel.deleteAllTasks()
                    Toast.makeText(this@MainActivity, "All tasks deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
