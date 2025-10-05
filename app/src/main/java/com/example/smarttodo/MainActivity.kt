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
import com.example.smarttodo.ui.NotificationSettingsDialog
import com.example.smarttodo.ui.SwipeGestureHelper
import com.example.smarttodo.ui.TaskAdapter
import com.example.smarttodo.ui.TaskItemDecoration
import com.example.smarttodo.ui.TaskViewModel
import com.example.smarttodo.ui.TaskViewModelFactory
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Date
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private var inflatedEmptyState: View? = null

    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(application, (application as SmartTodoApplication).repository)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                // Show a toast message or a snackbar to inform the user
                showToast(R.string.notifications_permission_denied)
            }
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
        setupFilterChips()
        setupSwipeRefresh()
        observeViewModel()
        askScheduleExactAlarmPermission() // Request alarm permission
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val dialog = NotificationSettingsDialog()
                dialog.show(supportFragmentManager, NotificationSettingsDialog.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                    // Permission is already granted, no action needed.
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain to the user why we need the permission
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.notification_permission_title)
                        .setMessage(R.string.notification_permission_rationale)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                else -> {
                    // Directly ask for the permission
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

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val filter = when {
                checkedIds.contains(R.id.chipToday) -> "TODAY"
                checkedIds.contains(R.id.chipTomorrow) -> "TOMORROW"
                checkedIds.contains(R.id.chipUpcoming) -> "UPCOMING"
                checkedIds.contains(R.id.chipHighPriority) -> "HIGH_PRIORITY"
                checkedIds.contains(R.id.chipCompleted) -> "COMPLETED"
                else -> "ALL"
            }
            taskViewModel.setFilter(filter)
        }
    }

    private fun showTaskDetail(task: Task, view: View) {
        val intent = Intent(this, com.example.smarttodo.ui.TaskDetailActivity::class.java).apply {
            putExtra("task_id", task.id)
           putExtra("transition_name", "task_card_${'$'}{task.id}")
        }
        val options = ActivityOptions.makeSceneTransitionAnimation(
            this, view, "task_card_${'$'}{task.id}"
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

        taskViewModel.tasksToDisplay.observe(this) { tasks ->
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

    private fun showToast(@StringRes messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
    }

}
