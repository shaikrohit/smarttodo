package com.example.smarttodo.util

/**
 * A generic sealed class that represents the result of an operation,
 * which can either be a success or an error.
 *
 * @param T The type of data returned on success.
 */
sealed class OperationResult<out T> {
    /**
     * Represents a successful operation.
     * @param data The data returned by the successful operation.
     */
    data class Success<out T>(val data: T) : OperationResult<T>()

    /**
     * Represents a failed operation.
     * @param exception The exception that caused the failure.
     * @param message A user-friendly message describing the error, optional.
     *                If not provided, a generic message might be used or derived from the exception.
     */
    data class Error(val exception: Exception, val message: String? = null) : OperationResult<Nothing>()
}
