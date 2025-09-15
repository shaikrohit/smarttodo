package com.example.smarttodo.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database.
 * This class provides methods to convert custom types or types not natively supported by Room
 * into types that Room can persist in the database, and vice-versa.
 * For example, it handles conversion for [Date] objects to/from [Long] timestamps
 * and [Priority] enum to/from [Int] values.
 */
class Converters {

    /**
     * Converts a [Long] timestamp (from the database) into a [Date] object.
     * If the timestamp is null, this method returns null.
     *
     * @param value The [Long] timestamp value from the database.
     * @return The corresponding [Date] object, or null if the input was null.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        // If value is not null, create a new Date object from it; otherwise, return null.
        return value?.let { Date(it) }
    }

    /**
     * Converts a [Date] object into a [Long] timestamp for database storage.
     * If the Date object is null, this method returns null.
     *
     * @param date The [Date] object to convert.
     * @return The corresponding [Long] timestamp (milliseconds since epoch), or null if the input was null.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        // If date is not null, get its time in milliseconds; otherwise, return null.
        return date?.time
    }

    /**
     * Converts an [Int] value (from the database) into a [Priority] enum.
     * This method assumes the integer value stored in the database correctly corresponds
     * to one of the `value` properties of the [Priority] enum.
     *
     * Note: If the [value] from the database does not match any existing [Priority.value],
     * this implementation (`Priority.values().first { it.value == value }`)
     * will throw a [NoSuchElementException].
     *
     * @param value The integer representation of the priority from the database.
     * @return The corresponding [Priority] enum.
     */
    @TypeConverter
    fun fromPriority(value: Int): Priority {
        // Finds the first Priority enum constant whose `value` property matches the input integer.
        return Priority.values().first { it.value == value }
    }

    /**
     * Converts a [Priority] enum into its [Int] representation for database storage.
     *
     * @param priority The [Priority] enum to convert.
     * @return The integer value associated with the priority.
     */
    @TypeConverter
    fun priorityToInt(priority: Priority): Int {
        // Returns the `value` property of the Priority enum.
        return priority.value
    }
}
