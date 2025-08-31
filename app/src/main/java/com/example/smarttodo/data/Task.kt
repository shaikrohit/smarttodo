package com.example.smarttodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: Int = 1,
    val isCompleted: Boolean = false,
    // This is nullable to prevent crashes from legacy data that might have null creation dates.
    // The app ensures new tasks always have a non-null date.
    val createdAt: Date? = Date(),
    val dueDate: Date? = null,
    val hasReminder: Boolean = false
) : Serializable

enum class Priority(val value: Int, val displayName: String) : Serializable {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High")
}