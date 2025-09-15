package com.example.smarttodo.util

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 * An event is an action that should only be consumed once, e.g., navigation,
 * showing a Snackbar message, or triggering a one-time animation.
 */
open class Event<out T>(private val content: T) {

    @Suppress("MemberVisibilityCanBePrivate") // Allow external read but not write
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}
