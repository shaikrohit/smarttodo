package com.example.smarttodo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smarttodo.data.TaskRepository

/**
 * Factory for creating instances of [TaskViewModel].
 * This class implements [ViewModelProvider.Factory] and is necessary because
 * [TaskViewModel] has a constructor that accepts a [TaskRepository] as a dependency.
 * The `ViewModelProvider` uses this factory to understand how to instantiate [TaskViewModel].
 *
 * @property repository The [TaskRepository] instance that will be provided to new [TaskViewModel] instances.
 */
class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the given `modelClass` (which is expected to be [TaskViewModel]).
     * This method is called by the `ViewModelProvider` when a ViewModel instance is requested.
     *
     * @param T The type of the ViewModel to create.
     * @param modelClass A [Class] whose instance is requested;
     *                   must be assignable from [TaskViewModel]::class.java.
     * @return A newly created [TaskViewModel] instance.
     * @throws IllegalArgumentException if the given [modelClass] is not [TaskViewModel] or
     *                                  a subclass of it.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is TaskViewModel or a subclass of it.
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            // If it is, create and return an instance of TaskViewModel,
            // supplying the repository. The cast is safe due to the isAssignableFrom check.
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        // If the requested class is not TaskViewModel, this factory cannot create it.
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}. This factory only creates TaskViewModel instances.")
    }
}
