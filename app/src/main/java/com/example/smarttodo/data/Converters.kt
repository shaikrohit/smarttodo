package com.example.smarttodo.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database
 * Converts Date objects to/from Long timestamps for database storage
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromPriority(value: Int): Priority {
        return Priority.values().first { it.value == value }
    }

    @TypeConverter
    fun priorityToInt(priority: Priority): Int {
        return priority.value
    }
}