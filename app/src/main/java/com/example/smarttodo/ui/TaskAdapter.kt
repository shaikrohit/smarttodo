package com.example.smarttodo.ui

import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttodo.R
import com.example.smarttodo.data.Priority
import com.example.smarttodo.data.Task
import com.example.smarttodo.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit,
    private val onCompleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getTaskAt(position: Int): Task = getItem(position)

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onTaskClick(getItem(pos))
                }
            }
            binding.root.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onTaskLongClick(getItem(pos))
                    true
                } else false
            }
            binding.checkboxComplete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onCompleteClick(getItem(pos))
                }
            }
        }

        fun bind(task: Task) {
            binding.apply {
                textViewTitle.text = task.title
                checkboxComplete.isChecked = task.isCompleted
                // Description
                if (task.description.isNotEmpty()) {
                    textViewDescription.text = task.description
                    textViewDescription.visibility = android.view.View.VISIBLE
                } else {
                    textViewDescription.visibility = android.view.View.GONE
                }
                // Due date
                task.dueDate?.let { dueDate ->
                    val formatter =
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                    textViewDueDate.text = "Due: ${formatter.format(dueDate)}"
                    layoutDueDate.visibility = android.view.View.VISIBLE
                    val now = Date()
                    if (dueDate.before(now) && !task.isCompleted) {
                        textViewDueDate.setTextColor(
                            ContextCompat.getColor(
                                root.context,
                                R.color.priority_high
                            )
                        )
                    } else {
                        val typedValue = TypedValue()
                        root.context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
                        textViewDueDate.setTextColor(typedValue.data)
                    }
                } ?: run {
                    layoutDueDate.visibility = android.view.View.GONE
                }
                // Priority color
                val priorityColor = when (task.priority) {
                    Priority.HIGH.value -> R.color.priority_high
                    Priority.MEDIUM.value -> R.color.priority_medium
                    Priority.LOW.value -> R.color.priority_low
                    else -> R.color.priority_low
                }
                viewPriorityIndicator.setBackgroundColor(
                    ContextCompat.getColor(root.context, priorityColor)
                )
                // Completed styling
                if (task.isCompleted) {
                    textViewTitle.paintFlags =
                        textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    textViewTitle.alpha = 0.6f
                    textViewDescription.alpha = 0.6f
                    root.alpha = 0.7f
                } else {
                    textViewTitle.paintFlags =
                        textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    textViewTitle.alpha = 1.0f
                    textViewDescription.alpha = 1.0f
                    root.alpha = 1.0f
                }
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }

}