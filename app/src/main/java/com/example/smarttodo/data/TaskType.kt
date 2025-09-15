package com.example.smarttodo.data

/**
 * Enum representing the different types or categories a [Task] can belong to.
 * This helps in organizing and potentially filtering tasks based on their nature.
 */
enum class TaskType {
    /**
     * Represents tasks that involve creative work, such as brainstorming, designing, or writing.
     */
    CREATIVE,

    /**
     * Represents tasks that involve analytical thinking, such as problem-solving, data analysis, or research.
     */
    ANALYTICAL,

    /**
     * Represents tasks that are administrative or logistical in nature,
     * such as scheduling, responding to emails, or organizing files.
     */
    ADMINISTRATIVE
}
