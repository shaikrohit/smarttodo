package com.example.smarttodo.ui

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import java.util.Date
import java.util.Locale

/**
 * A [ListAdapter] for displaying [Task] items in a RecyclerView.
 * It handles the creation and binding of ViewHolders, and uses [TaskDiffCallback]
 * to efficiently update the list when changes occur.
 *
 * @param onTaskClick Callback invoked when a task item is clicked.
 * @param onTaskLongClick Callback invoked when a task item is long-clicked.
 * @param onCompleteClick Callback invoked when a task's completion checkbox is clicked.
 */
class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit,
    private val onCompleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    /**
     * Called when RecyclerView needs a new [TaskViewHolder] of the given type to represent
     * an item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Returns the [Task] at the specified adapter position.
     */
    fun getTaskAt(position: Int): Task = getItem(position)

    /**
     * ViewHolder for task items. Holds the view binding, sets up click listeners,
     * and initializes a reusable [SimpleDateFormat] instance.
     * The `bind` method is responsible for populating the views with task data.
     *
     * @property binding The view binding instance for `item_task.xml`.
     */
    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var itemDateFormatter: SimpleDateFormat

        init {
            try {
                val formatString = itemView.context.getString(R.string.date_time_format_item)
                itemDateFormatter = SimpleDateFormat(formatString, Locale.getDefault())
            } catch (e: Exception) {
                Log.e("TaskViewHolder", "Failed to load date format string R.string.date_time_format_item. Using default.", e)
                itemDateFormatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) // Default fallback
            }

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { task -> onTaskClick(task) }
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { task -> onTaskLongClick(task) }
                    true
                } else false
            }
            binding.checkboxComplete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { task -> onCompleteClick(task) }
                }
            }
        }

        /**
         * Binds a [Task] object to the views in this ViewHolder.
         * Uses the pre-initialized [itemDateFormatter] for date formatting.
         */
        fun bind(task: Task) {
            binding.apply {
                textViewTitle.text = task.title
                checkboxComplete.isChecked = task.isCompleted

                if (task.description.isNotEmpty()) {
                    textViewDescription.text = task.description
                    textViewDescription.visibility = View.VISIBLE
                } else {
                    textViewDescription.visibility = View.GONE
                }

                task.dueDate?.let { dueDate ->
                    try {
                        textViewDueDate.text = itemView.context.getString(R.string.due_date_prefix_item, itemDateFormatter.format(dueDate))
                    } catch (e: Exception) {
                        Log.e("TaskViewHolder", "Failed to load due date prefix string R.string.due_date_prefix_item. Using default.", e)
                        textViewDueDate.text = "Due: ${itemDateFormatter.format(dueDate)}" // Basic fallback
                    }
                    layoutDueDate.visibility = View.VISIBLE

                    val now = Date()
                    if (dueDate.before(now) && !task.isCompleted) {
                        textViewDueDate.setTextColor(ContextCompat.getColor(root.context, R.color.priority_high))
                    } else {
                        textViewDueDate.setTextColor(ContextCompat.getColor(root.context, R.color.text_secondary_light))
                    }
                } ?: run {
                    layoutDueDate.visibility = View.GONE
                }

                val priorityColorResId = when (task.priority) {
                    Priority.HIGH -> R.color.priority_high
                    Priority.MEDIUM -> R.color.priority_medium
                    Priority.LOW -> R.color.priority_low
                }
                viewPriorityIndicator.setBackgroundColor(ContextCompat.getColor(root.context, priorityColorResId))

                if (task.isCompleted) {
                    textViewTitle.paintFlags = textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    textViewTitle.alpha = 0.6f
                    textViewDescription.alpha = 0.6f
                    root.alpha = 0.7f
                } else {
                    textViewTitle.paintFlags = textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    textViewTitle.alpha = 1.0f
                    textViewDescription.alpha = 1.0f
                    root.alpha = 1.0f
                }
            }
        }
    }

    /**
     * A [DiffUtil.ItemCallback] for calculating the difference between two non-null [Task] items.
     */
    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
