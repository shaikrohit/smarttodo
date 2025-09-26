package com.example.smarttodo.utils

/**
 * Defines standard snooze options for task reminders
 */
object SnoozeOptions {
    // Snooze durations in milliseconds
    const val SNOOZE_5_MIN = 5 * 60 * 1000L
    const val SNOOZE_15_MIN = 15 * 60 * 1000L
    const val SNOOZE_30_MIN = 30 * 60 * 1000L
    const val SNOOZE_1_HOUR = 60 * 60 * 1000L
    const val SNOOZE_2_HOUR = 2 * 60 * 60 * 1000L
    const val SNOOZE_TOMORROW = 24 * 60 * 60 * 1000L

    // Default snooze duration
    const val DEFAULT_SNOOZE_DURATION = SNOOZE_5_MIN

    // Named snooze options for display in UI
    val SNOOZE_OPTIONS = mapOf(
        SNOOZE_5_MIN to "5 minutes",
        SNOOZE_15_MIN to "15 minutes",
        SNOOZE_30_MIN to "30 minutes",
        SNOOZE_1_HOUR to "1 hour",
        SNOOZE_2_HOUR to "2 hours",
        SNOOZE_TOMORROW to "Tomorrow"
    )

    /**
     * Gets a human-readable description of a snooze duration
     */
    fun getDurationText(duration: Long): String {
        return SNOOZE_OPTIONS[duration] ?: "Custom time"
    }
}
