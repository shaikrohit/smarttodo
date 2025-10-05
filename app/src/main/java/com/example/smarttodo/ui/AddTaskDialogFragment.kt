package com.example.smarttodo.ui

import android.os.Bundle
import android.text.InputType
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.smarttodo.R
import com.example.smarttodo.data.Priority
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskType
import com.example.smarttodo.databinding.DialogAddTaskBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A DialogFragment for adding a new task or editing an existing one.
 * It handles user input for task details such as title, description, priority,
 * due date, and reminders.
 */
class AddTaskDialogFragment : DialogFragment() {

    private var _binding: DialogAddTaskBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Replace fragment-scoped ViewModel creation with an activity-scoped ViewModel using the app's factory
    private val taskViewModel: TaskViewModel by activityViewModels {
        TaskViewModelFactory(
            requireActivity().application,
            (requireActivity().application as com.example.smarttodo.SmartTodoApplication).repository
        )
    }

    private var editingTask: Task? = null
    private var selectedDate: Date? = null
    private var customPreReminderMinutes: Int? = null
    private var isSpinnerProgrammaticallySet: Boolean = false

    private val displayDateFormatter: SimpleDateFormat by lazy {
        try {
            val formatString = getString(R.string.date_time_format_display)
            SimpleDateFormat(formatString, Locale.getDefault())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load date format string from resources. Using default.", e)
            SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) // Default fallback
        }
    }

    companion object {
        private const val TAG = "AddTaskDialog" // For logging
        const val TAG_ADD = "AddTaskDialog_Add"
        const val TAG_EDIT = "EditTaskDialog_Edit"
        private const val ARG_TASK = "task"

        // Map of spinner position (excluding "No pre-reminder" and "Custom") to offset minutes
        private val PREDEFINED_OFFSET_VALUES = mapOf(
            1 to 5,    // Position 1 -> 5 minutes (e.g., "5 minutes before")
            2 to 15,   // Position 2 -> 15 minutes
            3 to 30,   // Position 3 -> 30 minutes
            4 to 60    // Position 4 -> 1 hour
        )
        private const val NO_PRE_REMINDER_POSITION = 0
        // Ensure this position matches the "Custom" item in your spinner_pre_reminder_options array
        private const val CUSTOM_PRE_REMINDER_POSITION = 5

        /**
         * Creates a new instance of AddTaskDialogFragment for editing an existing task.
         * @param task The task to be edited.
         * @return A new instance of AddTaskDialogFragment.
         */
        fun newInstance(task: Task): AddTaskDialogFragment {
            val fragment = AddTaskDialogFragment()
            val args = Bundle().apply { putSerializable(ARG_TASK, task) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_SmartToDo_Dialog_Slide) // Apply custom dialog theme
        Log.d(TAG, "onCreate: DialogFragment created.")
        arguments?.getSerializable(ARG_TASK)?.let {
            editingTask = it as? Task
            if (editingTask == null) {
                Log.w(TAG, "Failed to cast ARG_TASK to Task object during onCreate.")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddTaskBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView: Binding inflated.")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing dialog UI, listeners, and observers.")
        setupDialogAppearance()
        populateFieldsForEditing()
        setupClickListenersAndWatchers()
        observeUserMessages()
        updatePreReminderUIVisibility() // Set initial UI state for pre-reminder section
    }

    /**
     * Observes messages from the ViewModel, typically for success or error feedback after operations.
     */
    private fun observeUserMessages() {
        taskViewModel.userMessageEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { userMessage ->
                val messageText = userMessage.customMessage ?: getString(userMessage.messageResId!!)
                Log.d(TAG, "UserMessageEvent received: '$messageText', IsError: ${userMessage.isError}")
                if (userMessage.isError) {
                    Snackbar.make(binding.root, messageText, Snackbar.LENGTH_LONG).show()
                } else {
                    // Success messages for operations like insert/update are typically handled by dismissing the dialog.
                    // A Toast here can be for other non-critical feedback if needed.
                    Toast.makeText(requireContext(), messageText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Sets up the dialog's title and save button text based on whether it's an add or edit operation.
     * Also configures the dialog window layout parameters.
     */
    private fun setupDialogAppearance() {
        val isEditing = editingTask != null
        binding.textViewDialogTitle.text = if (isEditing) getString(R.string.edit_task_title) else getString(R.string.add_task_title)
        binding.buttonSave.text = if (isEditing) getString(R.string.update_task) else getString(R.string.save_task)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        Log.d(TAG, "Dialog appearance setup. Editing: $isEditing")
    }

    /**
     * If editing an existing task, this method populates the input fields with the task's data.
     */
    private fun populateFieldsForEditing() {
        editingTask?.let { task ->
            Log.d(TAG, "Populating fields for editing task ID: ${task.id}, Title: ${task.title}")
            binding.apply {
                editTextTitle.setText(task.title)
                editTextDescription.setText(task.description)
                when (task.priority) {
                    Priority.LOW -> chipLowPriority.isChecked = true
                    Priority.MEDIUM -> chipMediumPriority.isChecked = true
                    Priority.HIGH -> chipHighPriority.isChecked = true
                }
                when (task.taskType) {
                    TaskType.CREATIVE -> chipCreative.isChecked = true
                    TaskType.ANALYTICAL -> chipAnalytical.isChecked = true
                    TaskType.ADMINISTRATIVE -> chipAdministrative.isChecked = true
                }
                task.dueDate?.let { selectedDate = it }
                switchReminder.isChecked = task.hasReminder
                setPreReminderSpinnerSelection(task.preReminderOffsetMinutes)
            }
        } ?: Log.d(TAG, "No task to edit, fields will remain default/empty.")
        updateDateTimeDisplay() // Also updates pre-reminder visibility based on date
    }

    /**
     * Sets the selection of the pre-reminder spinner based on a given offset in minutes.
     * @param offsetMinutes The pre-reminder offset in minutes, or null if none.
     */
    private fun setPreReminderSpinnerSelection(offsetMinutes: Int?) {
        isSpinnerProgrammaticallySet = true // Set flag to prevent onItemSelected from firing immediately
        customPreReminderMinutes = null // Reset any previously set custom minutes
        var selectionMade = false

        if (offsetMinutes != null && offsetMinutes > 0) {
            PREDEFINED_OFFSET_VALUES.entries.find { it.value == offsetMinutes }?.let {
                binding.spinnerPreReminder.setSelection(it.key) // it.key is the position
                selectionMade = true
                Log.d(TAG, "Set pre-reminder spinner to position ${it.key} for offset $offsetMinutes")
            }
            if (!selectionMade) { // If not found in predefined, it's custom
                customPreReminderMinutes = offsetMinutes
                binding.spinnerPreReminder.setSelection(CUSTOM_PRE_REMINDER_POSITION)
                Log.d(TAG, "Set pre-reminder spinner to CUSTOM position for offset $offsetMinutes")
            }
        } else {
            binding.spinnerPreReminder.setSelection(NO_PRE_REMINDER_POSITION)
            Log.d(TAG, "Set pre-reminder spinner to NO_PRE_REMINDER_POSITION.")
        }
        // updatePreReminderUIVisibility() // This is usually called after this method, e.g., in populateFields or updateDateTimeDisplay
    }

    private fun showDatePicker() {
        val currentCal = Calendar.getInstance()
        selectedDate?.let { currentCal.time = it }

        val constraints = CalendarConstraints.Builder()
            .setStart(System.currentTimeMillis() - 1000) // allow today
            .build()

        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setCalendarConstraints(constraints)
            .setSelection(currentCal.timeInMillis)

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val preserved = Calendar.getInstance().apply { selectedDate?.let { time = it } }
            val newDateCal = Calendar.getInstance().apply { timeInMillis = selection }
            // Preserve hour/min/sec if already chosen
            newDateCal.set(Calendar.HOUR_OF_DAY, preserved.get(Calendar.HOUR_OF_DAY))
            newDateCal.set(Calendar.MINUTE, preserved.get(Calendar.MINUTE))
            newDateCal.set(Calendar.SECOND, 0)
            selectedDate = newDateCal.time
            updateDateTimeDisplay()
        }
        picker.show(parentFragmentManager, "material_date_picker")
    }

    private fun showTimePicker() {
        if (selectedDate == null) {
            selectedDate = Date()
        }
        val cal = Calendar.getInstance().apply { time = selectedDate!! }
        val is24 = DateFormat.is24HourFormat(requireContext())
        val picker = MaterialTimePicker.Builder()
            .setTitleText(R.string.select_time)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTimeFormat(if (is24) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .build()
        picker.addOnPositiveButtonClickListener {
            val newCal = Calendar.getInstance().apply { time = selectedDate!! }
            newCal.set(Calendar.HOUR_OF_DAY, picker.hour)
            newCal.set(Calendar.MINUTE, picker.minute)
            newCal.set(Calendar.SECOND, 0)
            selectedDate = newCal.time
            updateDateTimeDisplay()
        }
        picker.show(parentFragmentManager, "material_time_picker")
    }

    /**
     * Updates the display of the selected due date and time.
     * Also triggers an update to the pre-reminder UI visibility.
     */
    private fun updateDateTimeDisplay() {
        selectedDate?.let {
            binding.textViewSelectedDateTime.text = getString(R.string.due_date_prefix_display, displayDateFormatter.format(it))
            binding.textViewSelectedDateTime.visibility = View.VISIBLE
            Log.d(TAG, "Date/Time display updated: ${binding.textViewSelectedDateTime.text}")
        } ?: run {
            binding.textViewSelectedDateTime.visibility = View.GONE
            Log.d(TAG, "Date/Time display hidden as no date is selected.")
        }
        updatePreReminderUIVisibility() // Visibility of pre-reminder depends on date and switch
    }

    /**
     * Updates the visibility of the pre-reminder section (spinner and custom text)
     * based on whether the reminder switch is on and a due date is set.
     */
    private fun updatePreReminderUIVisibility() {
        val showPreReminderSection = binding.switchReminder.isChecked && selectedDate != null
        binding.preReminderLayout.visibility = if (showPreReminderSection) View.VISIBLE else View.GONE

        if (showPreReminderSection && customPreReminderMinutes != null && binding.spinnerPreReminder.selectedItemPosition == CUSTOM_PRE_REMINDER_POSITION) {
            binding.textViewCustomPreReminderDisplay.text = getString(R.string.custom_pre_reminder_dynamic_spinner_text, customPreReminderMinutes)
            binding.textViewCustomPreReminderDisplay.visibility = View.VISIBLE
        } else {
            binding.textViewCustomPreReminderDisplay.visibility = View.GONE
        }
        Log.d(TAG, "Pre-reminder UI visibility updated. Section visible: $showPreReminderSection, Custom text visible: ${binding.textViewCustomPreReminderDisplay.visibility == View.VISIBLE}")
    }

    private fun showCustomPreReminderDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.custom_pre_reminder_input_hint)
            customPreReminderMinutes?.let { setText(it.toString()) }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.custom_pre_reminder_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.custom_pre_reminder_set_button, null) // Set listener later for validation
            .setNegativeButton(R.string.cancel) { _, _ ->
                // If custom was selected but no value entered, revert spinner to "No pre-reminder"
                if (customPreReminderMinutes == null) {
                    Log.d(TAG, "Custom pre-reminder dialog cancelled, reverting spinner to NO_PRE_REMINDER.")
                    isSpinnerProgrammaticallySet = true
                    binding.spinnerPreReminder.setSelection(NO_PRE_REMINDER_POSITION)
                }
                updatePreReminderUIVisibility() // Update main dialog UI
            }
            .create()

        dialog.setOnShowListener { alertDialog ->
            val positiveButton = (alertDialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val inputText = editText.text.toString()
                val minutes = inputText.toIntOrNull()
                if (minutes != null && minutes > 0 && minutes <= 1440) { // Max 24 hours (1440 minutes)
                    customPreReminderMinutes = minutes
                    isSpinnerProgrammaticallySet = true // To prevent onItemSelected listener from re-triggering dialog
                    binding.spinnerPreReminder.setSelection(CUSTOM_PRE_REMINDER_POSITION)
                    Log.d(TAG, "Custom pre-reminder set to $minutes minutes.")
                    updatePreReminderUIVisibility()
                    dialog.dismiss()
                } else {
                    Log.w(TAG, "Invalid custom pre-reminder input: $inputText")
                    editText.error = getString(R.string.custom_pre_reminder_error_invalid_input)
                }
            }
            editText.doOnTextChanged { _, _, _, _ -> editText.error = null } // Clear error on text change
        }
        dialog.show()
        Log.d(TAG, "Showing custom pre-reminder input dialog.")
    }

    /**
     * Gathers data from input fields, validates it, and then requests the TaskViewModel
     * to either insert a new task or update an existing one.
     * Includes error handling for the ViewModel interaction.
     */
    private fun saveTask() {
        val title = binding.editTextTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.inputLayoutTitle.error = getString(R.string.title_required)
            Log.w(TAG, "Save task attempt failed: Title is empty.")
            return
        } else {
            binding.inputLayoutTitle.error = null // Clear error if title is provided
        }

        val description = binding.editTextDescription.text.toString().trim()
        val priority = when {
            binding.chipLowPriority.isChecked -> Priority.LOW
            binding.chipMediumPriority.isChecked -> Priority.MEDIUM
            binding.chipHighPriority.isChecked -> Priority.HIGH
            else -> Priority.LOW // Default priority
        }
        val taskType = when (binding.chipGroupTaskType.checkedChipId) {
            R.id.chipCreative -> TaskType.CREATIVE
            R.id.chipAnalytical -> TaskType.ANALYTICAL
            R.id.chipAdministrative -> TaskType.ADMINISTRATIVE
            else -> TaskType.ADMINISTRATIVE // Default task type, consider if this should be nullable or have a 'NONE' option
        }
        val hasReminder = binding.switchReminder.isChecked
        var preReminderOffsetToSave: Int? = null

        if (hasReminder && selectedDate != null) {
            when (val spinnerPosition = binding.spinnerPreReminder.selectedItemPosition) {
                CUSTOM_PRE_REMINDER_POSITION -> {
                    preReminderOffsetToSave = customPreReminderMinutes
                }
                NO_PRE_REMINDER_POSITION -> {
                    preReminderOffsetToSave = null
                }
                else -> { // Predefined values (positions based on PREDEFINED_OFFSET_VALUES map keys)
                    preReminderOffsetToSave = PREDEFINED_OFFSET_VALUES[spinnerPosition]
                }
            }
            // Ensure offset is null if "Custom" was selected but no valid value was entered/confirmed
            if (binding.spinnerPreReminder.selectedItemPosition == CUSTOM_PRE_REMINDER_POSITION && customPreReminderMinutes == null) {
                 preReminderOffsetToSave = null
                 Log.d(TAG, "Custom pre-reminder was selected but no value set, so offset is null.")
            }
        } else {
             preReminderOffsetToSave = null // No reminder if switch is off or no date set
        }

        val taskToSave = editingTask?.copy(
            title = title,
            description = description,
            priority = priority,
            taskType = taskType,
            dueDate = selectedDate,
            hasReminder = hasReminder,
            preReminderOffsetMinutes = preReminderOffsetToSave
        ) ?: Task(
            title = title,
            description = description,
            priority = priority,
            taskType = taskType,
            createdAt = Date(), // Set creation date only for new tasks
            dueDate = selectedDate,
            hasReminder = hasReminder,
            preReminderOffsetMinutes = preReminderOffsetToSave
        )

        Log.i(TAG, "Attempting to save/update task: Title='${taskToSave.title}', DueDate=${taskToSave.dueDate}, Reminder=${taskToSave.hasReminder}, Offset=${taskToSave.preReminderOffsetMinutes}")
        try {
            if (editingTask != null) {
                Log.d(TAG, "Calling viewModel.update for task ID: ${taskToSave.id}")
                taskViewModel.update(taskToSave)
            } else {
                Log.d(TAG, "Calling viewModel.insert for new task.")
                taskViewModel.insert(taskToSave)
            }

            // Attempt to dismiss the dialog
            if (isAdded && activity != null && !isStateSaved) {
                Log.d(TAG, "Task save/update successful, dismissing dialog.")
                dismissAllowingStateLoss() // Use dismissAllowingStateLoss to prevent crashes if state is already saved
            } else {
                Log.w(TAG, "Cannot dismiss dialog after save/update: isAdded=$isAdded, activity=$activity, isStateSaved=$isStateSaved. ViewModel should ideally notify for dismissal.")
            }
        } catch (e: Exception) {
            // This catch block is for unexpected synchronous errors during the ViewModel call itself.
            // Most database errors from Room (if happening on a background thread via ViewModel) would be asynchronous.
            Log.e(TAG, "CRITICAL: Exception during ViewModel call in saveTask: ${e.message}", e)
            Snackbar.make(binding.root, getString(R.string.error_saving_task_critical), Snackbar.LENGTH_LONG).show()
            // Do not dismiss the dialog if such a critical error occurs here.
        }
    }

    /**
     * Sets up click listeners for buttons and other interactive elements in the dialog.
     */
    private fun setupClickListenersAndWatchers() {
        // Setup the date and time picker buttons
        binding.buttonSelectDate.setOnClickListener { showDatePicker() }
        binding.buttonSelectTime.setOnClickListener { showTimePicker() }

        // Setup save button
        binding.buttonSave.setOnClickListener { saveTask() }

        // Setup cancel button
        binding.buttonCancel.setOnClickListener { dismiss() }

        // Setup reminder switch
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            updatePreReminderUIVisibility()
        }

        // Validate title input
        binding.editTextTitle.doOnTextChanged { text, _, _, _ ->
            binding.inputLayoutTitle.error = if (text.isNullOrBlank())
                getString(R.string.title_required)
            else
                null
        }

        // Setup priority chips
        binding.chipGroupPriority.setOnCheckedChangeListener { group, checkedId ->
            // Priority selection is automatically handled by the ChipGroup
            Log.d(TAG, "Priority selection changed to chip ID: $checkedId")
        }

        // Setup task type chips
        binding.chipGroupTaskType.setOnCheckedChangeListener { group, checkedId ->
            Log.d(TAG, "Task type selection changed to chip ID: $checkedId")
        }

        // Setup pre-reminder spinner listener
        binding.spinnerPreReminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isSpinnerProgrammaticallySet) {
                    // If this selection was triggered programmatically, reset flag and return
                    isSpinnerProgrammaticallySet = false
                    return
                }

                when (position) {
                    CUSTOM_PRE_REMINDER_POSITION -> {
                        showCustomPreReminderDialog()
                    }
                    NO_PRE_REMINDER_POSITION -> {
                        customPreReminderMinutes = null
                    }
                    else -> {
                        // For predefined values, store the corresponding minutes value
                        customPreReminderMinutes = null // Clear custom value
                        val minutes = PREDEFINED_OFFSET_VALUES[position]
                        Log.d(TAG, "Selected predefined pre-reminder option: $minutes minutes")
                    }
                }
                updatePreReminderUIVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Releasing binding.")
        _binding = null // Crucial to avoid memory leaks and crashes related to accessing binding after this point
    }
}
