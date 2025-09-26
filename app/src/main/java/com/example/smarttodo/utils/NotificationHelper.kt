package com.example.smarttodo.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.smarttodo.MainActivity
import com.example.smarttodo.R
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.Priority
import com.example.smarttodo.ui.NotificationActionActivity
import android.content.SharedPreferences
import androidx.core.content.edit
import android.preference.PreferenceManager
import com.example.smarttodo.util.SoundMode
import androidx.core.net.toUri

class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID = "smart_todo_reminders"
        const val NOTIFICATION_ID_BASE = 1000
        // Snooze / preference related
        const val DEFAULT_SNOOZE_MINUTES = 5
        private const val PREF_SNOOZE_MINUTES = "pref_snooze_minutes"
        private const val PREF_SOUND_MODE = "pref_sound_mode"
        private const val PREF_CUSTOM_SOUND_URI = "pref_custom_sound_uri"

        // Enhanced continuous vibration pattern: more noticeable and persistent
        // Format: wait, vibrate, wait, vibrate, etc.
        // This pattern creates a continuous vibration effect that repeats
        private val VIBRATION_PATTERN = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000)
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN

                // Make the notification light blink for visibility
                enableLights(true)
                lightColor = Color.RED

                // Set sound attributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM) // Using USAGE_ALARM for higher priority
                        .build()
                    setSound(null, audioAttributes) // We'll set sound per notification
                }

                // Make the notification lights and badge show
                setShowBadge(true)
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // region Preference API --------------------------------------------------
    /**
     * User preference helpers (single source of truth for notification configuration).
     * Stored in default SharedPreferences so they persist across launches.
     */

    /** Returns current notification sound mode + optional custom Uri */
    fun getNotificationSoundSettings(): Pair<SoundMode, Uri?> {
        val modeName = prefs.getString(PREF_SOUND_MODE, SoundMode.DEFAULT.name) ?: SoundMode.DEFAULT.name
        val mode = runCatching { SoundMode.valueOf(modeName) }.getOrDefault(SoundMode.DEFAULT)
        val uri = prefs.getString(PREF_CUSTOM_SOUND_URI, null)?.let { runCatching { it.toUri() }.getOrNull() }
        return mode to uri
    }

    /** Persist sound mode and optional custom URI */
    fun saveNotificationSettings(mode: SoundMode, customUri: Uri?) {
        prefs.edit {
            putString(PREF_SOUND_MODE, mode.name)
            if (mode == SoundMode.CUSTOM && customUri != null) putString(PREF_CUSTOM_SOUND_URI, customUri.toString()) else remove(PREF_CUSTOM_SOUND_URI)
        }
    }

    /** Current snooze duration (minutes) */
    fun getSnoozeDuration(): Int = prefs.getInt(PREF_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES).coerceAtLeast(1)

    /** Save snooze duration (minutes) */
    fun saveSnoozeDuration(minutes: Int) {
        val safe = minutes.coerceAtLeast(1)
        prefs.edit { putInt(PREF_SNOOZE_MINUTES, safe) }
    }
    // endregion --------------------------------------------------------------

    // Updated method to display task reminder with continuous vibration until user responds
    fun showTaskReminder(task: Task, isPreReminder: Boolean = false, soundUri: Uri? = null) {
        Log.d(TAG, "Showing task reminder for task ${task.id}: ${task.title}")

        // Create a full-screen intent to ensure the notification wakes up the device
        val fullScreenIntent = Intent(context, NotificationActionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            task.id,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to complete the task
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, task.id)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id * 10, // Unique request code
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_check,
            context.getString(R.string.notification_action_complete),
            completePendingIntent
        ).build()

        // Action to snooze the notification
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, task.id)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id * 10 + 1, // Different request code
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_snooze,
            context.getString(R.string.notification_action_snooze),
            snoozePendingIntent
        ).build()

        // Get the appropriate title based on whether this is a pre-reminder or a main reminder
        val title = if (isPreReminder) {
            context.getString(R.string.pre_reminder_title_generic, task.title)
        } else {
            task.title
        }

        // Set priority based on the task's priority
        val notificationPriority = when (task.priority) {
            Priority.HIGH -> NotificationCompat.PRIORITY_MAX
            Priority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
            Priority.LOW -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(task.description)
            .setPriority(notificationPriority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(completeAction)
            .addAction(snoozeAction)
            .setAutoCancel(false) // Don't auto-cancel; require user interaction
            .setOngoing(true) // Make it persistent until user interaction
            .setVibrate(VIBRATION_PATTERN) // Set continuous vibration pattern
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.description))
            .setContentIntent(fullScreenPendingIntent)
            .setSound(soundUri)
            .build()

        // Start vibrating the device continuously
        startContinuousVibration()

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                notify(NOTIFICATION_ID_BASE + task.id, notification)
                Log.d(TAG, "Notification posted for task ${task.id}")
            } else {
                Log.e(TAG, "Cannot show notification - missing POST_NOTIFICATIONS permission")
            }
        }
    }

    // Method to start continuous vibration
    private fun startContinuousVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator

                // Create a repeating vibration effect
                val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0) // Repeat indefinitely
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0) // Repeat indefinitely
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(VIBRATION_PATTERN, 0) // Repeat indefinitely
                }
            }
            Log.d(TAG, "Started continuous vibration")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    // Method to stop vibration
    fun stopVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
            }
            Log.d(TAG, "Stopped vibration")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }

    // Cancel a specific notification by task ID
    fun cancelNotification(taskId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_BASE + taskId)
        }
        stopVibration()
        Log.d(TAG, "Cancelled notification for task $taskId")
    }

    // Simple toast helper method
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
