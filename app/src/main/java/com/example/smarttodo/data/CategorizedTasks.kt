package com.example.smarttodo.data

data class CategorizedTasks(
    val today: List<Task>,
    val tomorrow: List<Task>,
    val upcoming: List<Task>,
    val completed: List<Task>
)
