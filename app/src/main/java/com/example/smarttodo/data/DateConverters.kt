package com.example.smarttodo.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database
 * Converts Date objects to/from Long timestamps for database storage
 */
class DateConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}