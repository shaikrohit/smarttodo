package com.example.smarttodo.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

object ThemeManager {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_IS_DARK_MODE = "is_dark_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isDarkMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_DARK_MODE, true) // Default to dark mode
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_DARK_MODE, isDark).apply()
        applyTheme(isDark)
    }

    fun applyTheme(isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /**
     * Apply the current theme to an activity
     * Call this in onCreate before setContentView
     */
    fun applyThemeToActivity(activity: Activity) {
        val isDark = isDarkMode(activity)
        applyTheme(isDark)
    }

    /**
     * Check if the system is currently in dark mode
     */
    fun isSystemInDarkMode(context: Context): Boolean {
        return (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Get priority color based on priority level with consideration for the current theme
     */
    fun getPriorityColor(context: Context, priority: Int): Int {
        return when (priority) {
            0 -> ContextCompat.getColor(context, com.example.smarttodo.R.color.priority_low)
            1 -> ContextCompat.getColor(context, com.example.smarttodo.R.color.priority_medium)
            else -> ContextCompat.getColor(context, com.example.smarttodo.R.color.priority_high)
        }
    }
}
