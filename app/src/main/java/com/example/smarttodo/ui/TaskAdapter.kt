@file:Suppress("unused", "UNUSED_PARAMETER")

package com.example.smarttodo.ui

import android.content.Context
import android.graphics.Paint
import android.util.Log
  import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
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

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

/**
 * Adapter for task list with headers. Uses ListAdapter for efficient diffs.
 * Improvements made:
 * - Cache a shared date formatter to avoid per-ViewHolder allocations.
 * - Resolve secondary text color from theme attributes so theme switching works correctly.
 * - Use payload updates to animate only completion changes.
 */
class TaskAdapter(
    private val onTaskClick: (Task, View) -> Unit,
    private val onTaskLongClick: (Task) -> Unit,
    private val onCompleteClick: (Task) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(TaskDiffCallback()) {

    companion object {
        // Shared formatter (initialized lazily using first available context)
        @Volatile
        private var sharedDateFormatter: SimpleDateFormat? = null

        @Suppress("UNUSED_PARAMETER")
        private fun getSharedFormatter(context: Context): SimpleDateFormat {
            val existing = sharedDateFormatter
            if (existing != null) return existing
            synchronized(this) {
                val once = sharedDateFormatter
                if (once != null) return once
                val formatString = try {
                    context.getString(R.string.date_time_format_item)
                } catch (e: Exception) {
                    // Log exception and reference it as throwable so analyzers see it used
                    Log.w("TaskAdapter", "Failed to read date_time_format_item, using default format", e)
                    "MMM dd, hh:mm a"
                }
                val fmt = SimpleDateFormat(formatString, Locale.getDefault())
                sharedDateFormatter = fmt
                return fmt
            }
        }

        private fun resolveSecondaryTextColor(context: Context, fallbackRes: Int): Int {
            // Use theme-aware color for secondary text
            val typedValue = android.util.TypedValue()
            return if (context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)) {
                typedValue.data
            } else {
                ContextCompat.getColor(context, fallbackRes)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is String -> ITEM_VIEW_TYPE_HEADER
            is Task -> ITEM_VIEW_TYPE_ITEM
            else -> throw IllegalArgumentException("Invalid type of data " + getItem(position).javaClass.name)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            ITEM_VIEW_TYPE_ITEM -> {
                val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TaskViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TaskViewHolder -> {
                val task = getItem(position) as Task
                holder.bind(task)
            }
            is HeaderViewHolder -> {
                val headerTitle = getItem(position) as String
                holder.bind(headerTitle)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            if (holder is TaskViewHolder) {
                val task = getItem(position) as Task
                payloads.forEach { payload ->
                    if (payload == TaskDiffCallback.COMPLETION_PAYLOAD) {
                        holder.animateCompletion(task.isCompleted)
                    }
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun getTaskAt(position: Int): Task? {
        val item = getItem(position)
        return if (item is Task) {
            item
        } else {
            null
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Lazily initialize shared formatter using the view context
            getSharedFormatter(itemView.context)

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (getItem(position) as? Task)?.let { task -> onTaskClick(task, itemView) }
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (getItem(position) as? Task)?.let { task -> onTaskLongClick(task) }
                    true
                } else false
            }
            binding.checkboxComplete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (getItem(position) as? Task)?.let { task -> onCompleteClick(task) }
                }
            }
        }

        fun bind(task: Task) {
            binding.root.transitionName = "task_card_${task.id}"
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
                        val formatter = getSharedFormatter(itemView.context)
                        layoutDueDate.text = itemView.context.getString(R.string.due_date_prefix_item, formatter.format(dueDate))
                    } catch (e: Exception) {
                        val ex = e
                        Log.e("TaskViewHolder", "Failed to load due date prefix string R.string.due_date_format_item. Using fallback.", ex)
                        // Use the translatable resource as a fallback as well
                        layoutDueDate.text = itemView.context.getString(R.string.due_date_prefix_item, getSharedFormatter(itemView.context).format(dueDate))
                    }
                    layoutDueDate.visibility = View.VISIBLE

                    val now = Date()
                    if (dueDate.before(now) && !task.isCompleted) {
                        layoutDueDate.setTextColor(ContextCompat.getColor(root.context, R.color.priority_high))
                    } else {
                        // Resolve secondary text color from theme so it matches dark/light
                        val secondaryColor = resolveSecondaryTextColor(root.context, R.color.textMuted)
                        layoutDueDate.setTextColor(secondaryColor)
                    }
                } ?: run {
                    layoutDueDate.visibility = View.GONE
                }

                val priorityColorResId = when (task.priority) {
                    Priority.HIGH -> R.color.priority_high
                    Priority.MEDIUM -> R.color.priority_medium
                    Priority.LOW -> R.color.priority_low
                    else -> android.R.color.transparent
                }
                viewPriorityIndicator.setBackgroundColor(ContextCompat.getColor(root.context, priorityColorResId))

                updateCompletedStatus(task.isCompleted)
            }
        }

        fun animateCompletion(isCompleted: Boolean) {
            val animation = if (isCompleted) {
                AnimationUtils.loadAnimation(itemView.context, R.anim.fade_out)
            } else {
                AnimationUtils.loadAnimation(itemView.context, R.anim.fade_in)
            }
            itemView.startAnimation(animation)
            updateCompletedStatus(isCompleted)
        }

        private fun updateCompletedStatus(isCompleted: Boolean) {
            if (isCompleted) {
                binding.textViewTitle.paintFlags = binding.textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textViewTitle.alpha = 0.6f
                binding.textViewDescription.alpha = 0.6f
                binding.root.alpha = 0.7f
            } else {
                binding.textViewTitle.paintFlags = binding.textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textViewTitle.alpha = 1.0f
                binding.textViewDescription.alpha = 1.0f
                binding.root.alpha = 1.0f
            }
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerTitle: TextView = view.findViewById(R.id.header_title)

        fun bind(title: String) {
            headerTitle.text = title
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Any>() {
        companion object {
            val COMPLETION_PAYLOAD = Any()
        }

        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is Task && newItem is Task) {
                oldItem.id == newItem.id
            } else if (oldItem is String && newItem is String) {
                oldItem == newItem
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is Task && newItem is Task) {
                oldItem == newItem
            } else if (oldItem is String && newItem is String) {
                oldItem == newItem
            } else {
                false
            }
        }

        override fun getChangePayload(oldItem: Any, newItem: Any): Any? {
            if (oldItem is Task && newItem is Task && oldItem.isCompleted != newItem.isCompleted) {
                return COMPLETION_PAYLOAD
            }
            return null
        }
    }
}
