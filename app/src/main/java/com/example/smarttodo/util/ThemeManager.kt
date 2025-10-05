package com.example.smarttodo.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.smarttodo.R

object ThemeManager {
    /**
     * Get priority color based on priority level (priority colors left unchanged).
     */
    fun getPriorityColor(context: Context, priority: Int): Int = when (priority) {
        0 -> ContextCompat.getColor(context, R.color.priority_low)
        1 -> ContextCompat.getColor(context, R.color.priority_medium)
        else -> ContextCompat.getColor(context, R.color.priority_high)
    }
}
