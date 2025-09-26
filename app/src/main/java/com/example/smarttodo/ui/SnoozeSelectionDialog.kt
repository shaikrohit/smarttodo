package com.example.smarttodo.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.smarttodo.R
import java.util.Calendar

/**
 * Dialog that appears when a user chooses to snooze a task notification,
 * offering multiple snooze duration options
 */
class SnoozeSelectionDialog : DialogFragment() {

    interface SnoozeSelectionListener {
        fun onSnoozeDurationSelected(taskId: Int, durationMillis: Long)
        fun onSnoozeCancel()
    }

    private var listener: SnoozeSelectionListener? = null
    private var taskTitle: String = ""
    private var taskId: Int = -1

    companion object {
        private const val ARG_TASK_TITLE = "arg_task_title"
        private const val ARG_TASK_ID = "arg_task_id"

        // Default snooze options
        object SnoozeOptions {
            const val MINUTES_5 = 5 * 60 * 1000L // 5 minutes in milliseconds
            const val MINUTES_15 = 15 * 60 * 1000L // 15 minutes
            const val MINUTES_30 = 30 * 60 * 1000L // 30 minutes
            const val HOUR_1 = 60 * 60 * 1000L // 1 hour
            const val HOUR_2 = 2 * 60 * 60 * 1000L // 2 hours
        }

        fun newInstance(taskId: Int, taskTitle: String): SnoozeSelectionDialog {
            val fragment = SnoozeSelectionDialog()
            val args = Bundle()
            args.putInt(ARG_TASK_ID, taskId)
            args.putString(ARG_TASK_TITLE, taskTitle)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is SnoozeSelectionListener) {
            listener = parentFragment as SnoozeSelectionListener
        } else if (context is SnoozeSelectionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement SnoozeSelectionListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            taskId = it.getInt(ARG_TASK_ID)
            taskTitle = it.getString(ARG_TASK_TITLE) ?: ""
        }

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_snooze_selection, container, false)

        // Set the task title
        val titleView = view.findViewById<TextView>(R.id.snooze_task_title)
        titleView.text = getString(R.string.snooze_dialog_title, taskTitle)

        // Create snooze option buttons
        val optionsContainer = view.findViewById<LinearLayout>(R.id.snooze_options_container)
        setupSnoozeOptions(optionsContainer)

        // Set up cancel button
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel_snooze)
        cancelButton.setOnClickListener {
            listener?.onSnoozeCancel()
            dismiss()
        }

        return view
    }

    private fun setupSnoozeOptions(container: LinearLayout) {
        // Add buttons for standard snooze options
        addSnoozeOptionButton(container, "5 minutes", SnoozeOptions.MINUTES_5)
        addSnoozeOptionButton(container, "15 minutes", SnoozeOptions.MINUTES_15)
        addSnoozeOptionButton(container, "30 minutes", SnoozeOptions.MINUTES_30)
        addSnoozeOptionButton(container, "1 hour", SnoozeOptions.HOUR_1)
        addSnoozeOptionButton(container, "2 hours", SnoozeOptions.HOUR_2)
    }

    private fun addSnoozeOptionButton(container: LinearLayout, text: String, durationMillis: Long) {
        val button = Button(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            this.text = text
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(24, 16, 24, 16)
            isAllCaps = false

            setOnClickListener {
                listener?.onSnoozeDurationSelected(taskId, durationMillis)
                dismiss()
            }
        }

        container.addView(button)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setWindowAnimations(R.style.DialogAnimation)
        }
    }
}
