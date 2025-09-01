package com.example.smarttodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarttodo.data.Task
import com.example.smarttodo.databinding.ActivityMainBinding
import com.example.smarttodo.ui.AddTaskDialogFragment
import com.example.smarttodo.ui.SwipeGestureHelper
import com.example.smarttodo.ui.TaskAdapter
import com.example.smarttodo.ui.TaskFilter
import com.example.smarttodo.ui.TaskItemDecoration
import com.example.smarttodo.ui.TaskViewModel
import com.example.smarttodo.ui.TaskViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModelFactory((application as SmartTodoApplication).repository)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, getString(R.string.notifications_permission_granted), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.notifications_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

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

        applySavedTheme()
        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.permission_needed))
                        .setMessage(getString(R.string.notification_permission_rationale))
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
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
            addItemDecoration(TaskItemDecoration(resources.getDimensionPixelSize(R.dimen.task_item_spacing)))
        }

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
            taskViewModel.setSearchQuery(text.toString().trim())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, getString(R.string.tasks_refreshed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        taskViewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        taskViewModel.tasks.observe(this) { tasks ->
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
        val message = if (task.isCompleted) getString(R.string.task_incomplete) else getString(R.string.task_completed)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showTaskOptions(task: Task) {
        val options = arrayOf(
            getString(R.string.edit),
            getString(R.string.delete),
            getString(R.string.duplicate)
        )

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
            .setTitle(R.string.delete_task_title)
            .setMessage(getString(R.string.delete_task_message, task.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                taskViewModel.delete(task)
                Toast.makeText(this, getString(R.string.task_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun duplicateTask(task: Task) {
        val duplicatedTask = task.copy(
            id = 0, // New ID will be auto-generated
            title = "${task.title}${getString(R.string.task_title_copy_suffix)}",
            isCompleted = false,
            createdAt = java.util.Date()
        )
        taskViewModel.insert(duplicatedTask)
        Toast.makeText(this, getString(R.string.task_duplicated), Toast.LENGTH_SHORT).show()
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

        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit().putInt("theme_mode", newMode).apply()

        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
    }

    private fun showDeleteCompletedConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_completed_tasks_title)
            .setMessage(R.string.delete_completed_tasks_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                taskViewModel.deleteCompletedTasks()
                Toast.makeText(this, getString(R.string.completed_tasks_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteAllConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_all_tasks_title)
            .setMessage(R.string.delete_all_tasks_message)
            .setPositiveButton(R.string.delete_all) { _, _ ->
                lifecycleScope.launch {
                    taskViewModel.deleteAllTasks()
                    Toast.makeText(this@MainActivity, getString(R.string.all_tasks_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
