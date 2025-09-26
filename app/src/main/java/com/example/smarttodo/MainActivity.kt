package com.example.smarttodo

import android.Manifest
import android.app.ActivityOptions
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
import com.example.smarttodo.ui.TaskItemDecoration
import com.example.smarttodo.ui.TaskViewModel
import com.example.smarttodo.ui.TaskViewModelFactory
import com.example.smarttodo.util.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
            showToast(messageResId)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission for Android 13+
        requestNotificationPermissionIfNeeded()

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupSearch()
        setupSwipeRefresh()
        observeViewModel()

        applySavedTheme()
        askScheduleExactAlarmPermission() // Request alarm permission
    }

    /**
     * Request notification permission if needed (Android 13+)
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    // You can initialize notification components here if needed
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show a dialog explaining why notifications are important
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.notification_permission_title)
                        .setMessage(getString(R.string.notification_permission_rationale))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                else -> {
                    // First time asking or "Don't ask again" selected
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun askScheduleExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.permission_needed))
                    .setMessage(getString(R.string.exact_alarm_permission_rationale))
                    .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(intent)
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_theme).isChecked = ThemeManager.isDarkMode(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                item.isChecked = !item.isChecked
                ThemeManager.setDarkMode(this, item.isChecked)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun applySavedTheme() {
        ThemeManager.applyTheme(ThemeManager.isDarkMode(this))
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task, view -> showTaskDetail(task, view) },
            onTaskLongClick = { task -> showTaskOptions(task) },
            onCompleteClick = { task -> toggleTaskCompletion(task) }
        )
        binding.recyclerViewTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(TaskItemDecoration(resources.getDimensionPixelSize(R.dimen.task_item_spacing)))
        }
        setupSwipeGestures()

        // Hide FAB on scroll for cleaner UX; show when user scrolls up
        binding.recyclerViewTasks.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 10) binding.fabAddTask.hide()
                else if (dy < -10) binding.fabAddTask.show()
            }
        })
    }

    private fun showTaskDetail(task: Task, view: View) {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_description", task.description)
            putExtra("transition_name", "task_card_${task.id}")
        }
        val options = ActivityOptions.makeSceneTransitionAnimation(
            this, view, "task_card_${task.id}"
        )
        startActivity(intent, options.toBundle())
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
                    taskAdapter.getTaskAt(position)?.let { showDeleteConfirmation(it, position) }
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

        taskViewModel.categorizedTasks.observe(this) { categorizedTasks ->
            val displayableList = mutableListOf<Any>()
            if (categorizedTasks.today.isNotEmpty()) {
                displayableList.add(getString(R.string.category_today))
                displayableList.addAll(categorizedTasks.today)
            }
            if (categorizedTasks.tomorrow.isNotEmpty()) {
                displayableList.add(getString(R.string.category_tomorrow))
                displayableList.addAll(categorizedTasks.tomorrow)
            }
            if (categorizedTasks.upcoming.isNotEmpty()) {
                displayableList.add(getString(R.string.category_upcoming))
                displayableList.addAll(categorizedTasks.upcoming)
            }
            if (categorizedTasks.completed.isNotEmpty()) {
                displayableList.add(getString(R.string.category_completed))
                displayableList.addAll(categorizedTasks.completed)
            }
            taskAdapter.submitList(displayableList)
            updateEmptyState(displayableList.isEmpty())
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
                // Wire the empty-state Add button to open the add task dialog
                inflatedEmptyState?.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonEmptyAdd)
                    ?.setOnClickListener {
                        showAddTaskDialog()
                    }
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
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
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
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener { 
                onCancel?.invoke()
            }
            .show()
    }

    private fun showDeleteConfirmation(task: Task, position: Int = -1) {
        showConfirmationDialog(
            titleResId = R.string.delete_task_title,
            messageResId = R.string.delete_task_message,
            messageArgs = arrayOf(task.title),
            onConfirm = {
                taskViewModel.delete(task)
            },
            onCancel = {
                if (position != -1) {
                    taskAdapter.notifyItemChanged(position)
                }
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

    @Suppress("unused")
    private fun showDeleteCompletedConfirmation() {
        showConfirmationDialog(
            titleResId = R.string.delete_completed_tasks_title,
            messageResId = R.string.delete_completed_tasks_message,
            onConfirm = { taskViewModel.deleteCompletedTasks() },
            onCancel = {}
        )
    }

    @Suppress("unused")
    private fun showDeleteAllConfirmation() {
        showConfirmationDialog(
            titleResId = R.string.delete_all_tasks_title,
            messageResId = R.string.delete_all_tasks_message,
            positiveButtonResId = R.string.delete_all,
            onConfirm = { taskViewModel.deleteAllTasks() },
            onCancel = {}
        )
    }

    private fun showToast(@StringRes messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}
