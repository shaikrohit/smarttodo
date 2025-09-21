package com.example.smarttodo.util

/**
 * A generic sealed class that represents the result of an operation,
 * which can either be a success or an error.
 * It is used throughout the application, particularly by the data layer (repositories)
 * to communicate outcomes of data manipulation tasks (like insert, update, delete)
 * to the ViewModel layer. This allows for structured handling of successful results
 * and any exceptions or error conditions that may occur.
 *
 * @param T The type of data returned on successful completion of the operation.
 *          For operations that do not return data (e.g., a delete operation), [Unit] can be used.
 */
sealed class OperationResult<out T> {
    /**
     * Represents a successful outcome of an operation.
     * Contains the data payload of type [T] resulting from the operation.
     *
     * @param data The data returned by the successful operation. For operations
     *             that don't yield a specific result (e.g., successful update or delete),
     *             this is typically [Unit].
     */
    data class Success<out T>(val data: T) : OperationResult<T>()

    /**
     * Represents a failed outcome of an operation.
     * Contains the [Exception] that occurred and an optional user-friendly [message].
     * This class helps in propagating error details from data or business logic layers
     * to the UI layer for appropriate user feedback or error logging.
     *
     * @param exception The [Exception] that was caught during the operation,
     *                  providing details about the failure.
     * @param message A user-friendly, potentially localized, message describing the error.
     *                This can be displayed to the user. If null, a generic error message
     *                might be used by the UI layer, or one might be derived from the [exception].
     */
    data class Error(val exception: Exception, val message: String? = null) : OperationResult<Nothing>()
}
