package com.example.smarttodo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.smarttodo.R
import com.example.smarttodo.data.Priority
import com.example.smarttodo.data.Task
import com.example.smarttodo.databinding.DialogAddTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class AddTaskDialogFragment : DialogFragment() {

    private var _binding: DialogAddTaskBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by viewModels({ requireActivity() })
    private var editingTask: Task? = null
    private var selectedDate: Date? = null

    companion object {
        private const val ARG_TASK = "task"

        fun newInstance(task: Task): AddTaskDialogFragment {
            val fragment = AddTaskDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_TASK, task)
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

        setupDialog()
        setupClickListeners()
        populateEditingTask()
    }

    private fun setupDialog() {
        binding.textViewDialogTitle.text = if (editingTask != null) "Edit Task" else "Add New Task"
        binding.buttonSave.text = if (editingTask != null) "Update Task" else "Save Task"
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupClickListeners() {
        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonSave.setOnClickListener { saveTask() }
        binding.buttonSelectDate.setOnClickListener { showDatePicker() }
        binding.buttonSelectTime.setOnClickListener { showTimePicker() }
    }

    private fun populateEditingTask() {
        editingTask?.let { task ->
            binding.apply {
                editTextTitle.setText(task.title)
                editTextDescription.setText(task.description)

                when (task.priority) {
                    Priority.LOW.value -> radioLowPriority.isChecked = true
                    Priority.MEDIUM.value -> radioMediumPriority.isChecked = true
                    Priority.HIGH.value -> radioHighPriority.isChecked = true
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
        val calendar = Calendar.getInstance()
        selectedDate?.let { calendar.time = it }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                selectedDate?.let { newCalendar.time = it }
                newCalendar.set(year, month, dayOfMonth)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        if (selectedDate == null) {
            selectedDate = Date()
        }
        val calendar = Calendar.getInstance()
        selectedDate?.let { calendar.time = it }

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val newCalendar = Calendar.getInstance()
                newCalendar.time = selectedDate!!
                newCalendar.set(Calendar.HOUR_OF_DAY, hour)
                newCalendar.set(Calendar.MINUTE, minute)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }

    private fun updateDateTimeDisplay() {
        selectedDate?.let {
            val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            binding.textViewSelectedDateTime.text = "Due: ${formatter.format(it)}"
            binding.textViewSelectedDateTime.visibility = View.VISIBLE
        }
    }

    private fun saveTask() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.inputLayoutTitle.error = "Title is required"
            return
        } else {
            binding.inputLayoutTitle.error = null
        }

        val priority = when (binding.radioGroupPriority.checkedRadioButtonId) {
            R.id.radioLowPriority -> Priority.LOW.value
            R.id.radioMediumPriority -> Priority.MEDIUM.value
            R.id.radioHighPriority -> Priority.HIGH.value
            else -> Priority.LOW.value
        }
        val hasReminder = binding.switchReminder.isChecked

        if (editingTask != null) {
            val updatedTask = editingTask!!.copy(
                title = title,
                description = description,
                priority = priority,
                dueDate = selectedDate,
                hasReminder = hasReminder
            )
            taskViewModel.update(updatedTask)
            Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show()
        } else {
            val newTask = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = selectedDate,
                hasReminder = hasReminder,
                createdAt = Date()
            )
            taskViewModel.insert(newTask)
            Toast.makeText(requireContext(), "Task created", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
