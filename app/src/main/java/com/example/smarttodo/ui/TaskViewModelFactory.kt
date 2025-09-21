package com.example.smarttodo.ui

import android.app.Application // Added import
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smarttodo.data.TaskRepository

/**
 * Factory for creating instances of [TaskViewModel].
 * This class implements [ViewModelProvider.Factory] and is necessary because
 * [TaskViewModel] has a constructor that accepts a [TaskRepository] and [Application] as dependencies.
 *
 * @property application The [Application] instance.
 * @property repository The [TaskRepository] instance that will be provided to new [TaskViewModel] instances.
 */
class TaskViewModelFactory(
    private val application: Application, // Added application parameter
    private val repository: TaskRepository
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the given `modelClass` (which is expected to be [TaskViewModel]).
     *
     * @param T The type of the ViewModel to create.
     * @param modelClass A [Class] whose instance is requested;
     *                   must be assignable from [TaskViewModel]::class.java.
     * @return A newly created [TaskViewModel] instance.
     * @throws IllegalArgumentException if the given [modelClass] is not [TaskViewModel] or
     *                                  a subclass of it.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application, repository) as T // Pass application to TaskViewModel
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}. This factory only creates TaskViewModel instances.")
    }
}
