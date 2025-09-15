package com.example.smarttodo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.smarttodo.R
import com.example.smarttodo.data.Priority
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskType
import com.example.smarttodo.databinding.DialogAddTaskBinding
import com.google.android.material.snackbar.Snackbar // Import Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTaskDialogFragment : DialogFragment() {

    private var _binding: DialogAddTaskBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by viewModels({ requireActivity() })

    private var editingTask: Task? = null
    private var selectedDate: Date? = null

    private val displayDateFormatter: SimpleDateFormat by lazy {
        try {
            val formatString = getString(R.string.date_time_format_display)
            SimpleDateFormat(formatString, Locale.getDefault())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load date format string R.string.date_time_format_display. Using default.", e)
            SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        }
    }

    companion object {
        private const val TAG = "AddTaskDialog"
        const val TAG_ADD = "AddTaskDialog_Add"
        const val TAG_EDIT = "EditTaskDialog_Edit"
        private const val ARG_TASK = "task"

        fun newInstance(task: Task): AddTaskDialogFragment {
            val fragment = AddTaskDialogFragment()
            val args = Bundle().apply {
                putSerializable(ARG_TASK, task)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_SmartToDo_Dialog)
        arguments?.getSerializable(ARG_TASK)?.let {
            editingTask = it as Task
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialogAppearance()
        setupClickListeners()
        populateFieldsForEditing()
        observeUserMessages() // Setup observer for ViewModel messages
    }

    private fun observeUserMessages() {
        taskViewModel.userMessageEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { userMessage ->
                val messageText = userMessage.customMessage ?: getString(userMessage.messageResId!!)
                if (userMessage.isError) {
                    // Use binding.root for Snackbar in DialogFragment
                    Snackbar.make(binding.root, messageText, Snackbar.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), messageText, Toast.LENGTH_SHORT).show()
                    dismiss() // Dismiss dialog on success
                }
            }
        }
    }

    private fun setupDialogAppearance() {
        val isEditing = editingTask != null
        binding.textViewDialogTitle.text = if (isEditing) getString(R.string.edit_task_title) else getString(R.string.add_task_title)
        binding.buttonSave.text = if (isEditing) getString(R.string.update_task) else getString(R.string.save_task)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupClickListeners() {
        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonSave.setOnClickListener { saveTask() }
        binding.buttonSelectDate.setOnClickListener { showDatePicker() }
        binding.buttonSelectTime.setOnClickListener { showTimePicker() }
    }

    private fun populateFieldsForEditing() {
        editingTask?.let { task ->
            binding.apply {
                editTextTitle.setText(task.title)
                editTextDescription.setText(task.description)
                when (task.priority) {
                    Priority.LOW -> radioLowPriority.isChecked = true
                    Priority.MEDIUM -> radioMediumPriority.isChecked = true
                    Priority.HIGH -> radioHighPriority.isChecked = true
                }
                when (task.taskType) {
                    TaskType.CREATIVE -> chipCreative.isChecked = true
                    TaskType.ANALYTICAL -> chipAnalytical.isChecked = true
                    TaskType.ADMINISTRATIVE -> chipAdministrative.isChecked = true
                }
                task.dueDate?.let {
                    selectedDate = it
                    updateDateTimeDisplay()
                }
                switchReminder.isChecked = task.hasReminder
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            selectedDate?.let { time = it }
        }
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    selectedDate?.let { time = it } // Preserve existing time
                }
                newCalendar.set(year, month, dayOfMonth)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        if (selectedDate == null) {
            selectedDate = Date() // Default to now if no date is set
        }
        val calendar = Calendar.getInstance().apply {
            time = selectedDate!!
        }
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance().apply {
                    time = selectedDate!!
                }
                newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCalendar.set(Calendar.MINUTE, minute)
                newCalendar.set(Calendar.SECOND, 0)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // Uses device locale for 12/24 hour format.
        )
        timePickerDialog.show()
    }

    private fun updateDateTimeDisplay() {
        selectedDate?.let {
            try {
                binding.textViewSelectedDateTime.text = getString(R.string.due_date_prefix_display, displayDateFormatter.format(it))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load due date prefix string R.string.due_date_prefix_display. Using default.", e)
                binding.textViewSelectedDateTime.text = "Due: ${displayDateFormatter.format(it)}"
            }
            binding.textViewSelectedDateTime.visibility = View.VISIBLE
        } ?: run {
            binding.textViewSelectedDateTime.visibility = View.GONE
        }
    }

    private fun saveTask() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.inputLayoutTitle.error = getString(R.string.title_required)
            return
        } else {
            binding.inputLayoutTitle.error = null
        }

        val priority = when (binding.radioGroupPriority.checkedRadioButtonId) {
            R.id.radioLowPriority -> Priority.LOW
            R.id.radioMediumPriority -> Priority.MEDIUM
            R.id.radioHighPriority -> Priority.HIGH
            else -> Priority.LOW
        }

        val taskType = when (binding.chipGroupTaskType.checkedChipId) {
            R.id.chipCreative -> TaskType.CREATIVE
            R.id.chipAnalytical -> TaskType.ANALYTICAL
            R.id.chipAdministrative -> TaskType.ADMINISTRATIVE
            else -> TaskType.ADMINISTRATIVE
        }
        val hasReminder = binding.switchReminder.isChecked

        if (editingTask != null) {
            val updatedTask = editingTask!!.copy(
                title = title,
                description = description,
                priority = priority,
                dueDate = selectedDate,
                hasReminder = hasReminder,
                taskType = taskType
            )
            taskViewModel.update(updatedTask)
            // Toast and dismiss are now handled by observing userMessageEvent
        } else {
            val newTask = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = selectedDate,
                hasReminder = hasReminder,
                createdAt = Date(),
                taskType = taskType
            )
            taskViewModel.insert(newTask)
            // Toast and dismiss are now handled by observing userMessageEvent
        }
        // Do NOT dismiss here. Dismissal is handled by the userMessageEvent observer on success.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
