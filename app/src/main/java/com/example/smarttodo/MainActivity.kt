package com.example.smarttodo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttodo.data.Task
import com.example.smarttodo.databinding.ActivityMainBinding
import com.example.smarttodo.ui.AddTaskDialogFragment
import com.example.smarttodo.ui.SwipeGestureHelper
import com.example.smarttodo.ui.TaskAdapter
import com.example.smarttodo.ui.TaskFilter
import com.example.smarttodo.ui.TaskItemDecoration
import com.example.smarttodo.ui.TaskViewModel
import com.example.smarttodo.ui.TaskViewModelFactory
import com.example.smarttodo.ui.UserMessage // Import UserMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar // Import Snackbar
import com.google.android.material.tabs.TabLayout
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private var inflatedEmptyState: View? = null

    private val taskViewModel: TaskViewModel by viewModels {
        // Updated to pass application context to the factory
        TaskViewModelFactory(application, (application as SmartTodoApplication).repository)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            val messageResId = if (isGranted) {
                R.string.notifications_permission_granted
            } else {
                R.string.notifications_permission_denied
            }
            Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
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
                if (position != RecyclerView.NO_POSITION) {
                    taskAdapter.getTaskAt(position)?.let { toggleTaskCompletion(it) }
                }
            },
            onSwipeLeft = { position ->
                if (position != RecyclerView.NO_POSITION) {
                    taskAdapter.getTaskAt(position)?.let { showDeleteConfirmation(it) }
                }
            }
        )
        val itemTouchHelper = ItemTouchHelper(swipeGestureHelper)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
        binding.fabPomodoro.setOnClickListener {
            val intent = Intent(this, PomodoroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val filter = when (tab?.position) {
                    0 -> TaskFilter.ALL
                    1 -> TaskFilter.INCOMPLETE
                    2 -> TaskFilter.COMPLETED
                    else -> TaskFilter.ALL
                }
                taskViewModel.setFilter(filter)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) { /* No action */ }
            override fun onTabReselected(tab: TabLayout.Tab?) { /* No action */ }
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

        taskViewModel.userMessageEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { userMessage ->
                val messageText = userMessage.customMessage ?: getString(userMessage.messageResId!!)
                if (userMessage.isError) {
                    Snackbar.make(binding.root, messageText, Snackbar.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, messageText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewTasks.visibility = View.GONE
            if (inflatedEmptyState == null) {
                inflatedEmptyState = binding.emptyStateViewStub.inflate()
            }
            inflatedEmptyState?.visibility = View.VISIBLE
        } else {
            binding.recyclerViewTasks.visibility = View.VISIBLE
            inflatedEmptyState?.visibility = View.GONE
        }
    }

    private fun showAddTaskDialog() {
        val dialog = AddTaskDialogFragment()
        dialog.show(supportFragmentManager, AddTaskDialogFragment.TAG_ADD)
    }

    private fun editTask(task: Task) {
        val dialog = AddTaskDialogFragment.newInstance(task)
        dialog.show(supportFragmentManager, AddTaskDialogFragment.TAG_EDIT)
    }

    private fun toggleTaskCompletion(task: Task) {
        taskViewModel.toggleTaskCompletion(task)
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

    private fun showConfirmationDialog(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes positiveButtonResId: Int = R.string.delete,
        messageArgs: Array<Any>? = null,
        onConfirm: () -> Unit
    ) {
        val message = if (messageArgs != null) {
            getString(messageResId, *messageArgs)
        } else {
            getString(messageResId)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(titleResId))
            .setMessage(message)
            .setPositiveButton(getString(positiveButtonResId)) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmation(task: Task) {
        showConfirmationDialog(
            titleResId = R.string.delete_task_title,
            messageResId = R.string.delete_task_message,
            messageArgs = arrayOf(task.title),
            onConfirm = {
                taskViewModel.delete(task)
            }
        )
    }

    private fun duplicateTask(task: Task) {
        val duplicatedTask = task.copy(
            id = 0,
            title = "${task.title}${getString(R.string.task_title_copy_suffix)}",
            isCompleted = false,
            createdAt = Date()
        )
        taskViewModel.insert(duplicatedTask)
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
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        val newNightMode = if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit().putInt("theme_mode", newNightMode).apply()
        AppCompatDelegate.setDefaultNightMode(newNightMode)
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedThemeMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)
    }

    private fun showDeleteCompletedConfirmation() {
        showConfirmationDialog(
            titleResId = R.string.delete_completed_tasks_title,
            messageResId = R.string.delete_completed_tasks_message,
            onConfirm = { taskViewModel.deleteCompletedTasks() }
        )
    }

    private fun showDeleteAllConfirmation() {
        showConfirmationDialog(
            titleResId = R.string.delete_all_tasks_title,
            messageResId = R.string.delete_all_tasks_message,
            positiveButtonResId = R.string.delete_all,
            onConfirm = { taskViewModel.deleteAllTasks() }
        )
    }
}
