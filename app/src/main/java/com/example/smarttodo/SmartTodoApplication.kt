package com.example.smarttodo

import android.app.Application
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.data.TaskRepository

/**
 * Custom [Application] class for the SmartTodo application.
 * This class serves as a centralized place to initialize and provide access to
 * application-wide components, such as the Room database and the TaskRepository.
 * It ensures that these components are created as singletons and are available
 * throughout the application's lifecycle.
 */
class SmartTodoApplication : Application() {

    /**
     * Lazily initialized singleton instance of the Room [TaskDatabase].
     * The database is created only when this property is first accessed,
     * using the application context.
     */
    val database: TaskDatabase by lazy { TaskDatabase.getDatabase(this) }

    /**
     * Lazily initialized singleton instance of the [TaskRepository].
     * The repository is created only when this property is first accessed,
     * and it depends on the `taskDao()` provided by the [database] instance.
     * This repository serves as the single source of truth for task data for the UI layer.
     */
    val repository: TaskRepository by lazy { TaskRepository(database.taskDao()) }

    // No explicit onCreate() override is needed here if the only app-level
    // initializations are handled by the lazy delegates above.
    // If other global initializations were required (e.g., setting up logging,
    // analytics, etc.), they would typically go into an overridden onCreate() method.
}
