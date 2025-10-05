package com.example.smarttodo.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.smarttodo.R
import com.example.smarttodo.data.Priority
import com.example.smarttodo.data.Task
import com.example.smarttodo.receiver.NotificationActionReceiver
import com.example.smarttodo.ui.NotificationActionActivity
import com.example.smarttodo.util.SoundMode

class NotificationHelper(private val context: Context) {

    private val wakeLockManager = WakeLockManager(context)
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID = "smart_todo_reminders"
        private const val NOTIFICATION_ID_BASE = 1000

        const val DEFAULT_SNOOZE_MINUTES = 10
        private const val PREF_SNOOZE_MINUTES = "pref_snooze_minutes"
        private const val PREF_SOUND_MODE = "pref_sound_mode"
        private const val PREF_CUSTOM_SOUND_URI = "pref_custom_sound_uri"

        private val VIBRATION_PATTERN = longArrayOf(0, 1000, 500, 1000, 500, 1000) // Shorter, effective pattern
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val name = context.getString(R.string.channel_name)
                val descriptionText = context.getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                    enableLights(true)
                    lightColor = Color.RED
                    setSound(null, AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build())
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channel", e)
            }
        }
    }

    fun getNotificationSoundSettings(): Pair<SoundMode, Uri?> {
        val modeName = prefs.getString(PREF_SOUND_MODE, SoundMode.DEFAULT.name) ?: SoundMode.DEFAULT.name
        val mode = runCatching { SoundMode.valueOf(modeName) }.getOrDefault(SoundMode.DEFAULT)
        val uriString = prefs.getString(PREF_CUSTOM_SOUND_URI, null)
        val uri = uriString?.let { runCatching { it.toUri() }.getOrNull() }
        return mode to uri
    }

    fun saveNotificationSettings(mode: SoundMode, customUri: Uri?) {
        prefs.edit {
            putString(PREF_SOUND_MODE, mode.name)
            if (mode == SoundMode.CUSTOM && customUri != null) {
                putString(PREF_CUSTOM_SOUND_URI, customUri.toString())
            } else {
                remove(PREF_CUSTOM_SOUND_URI)
            }
        }
    }

    fun getSnoozeDuration(): Int = prefs.getInt(PREF_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES).coerceAtLeast(1)

    fun saveSnoozeDuration(minutes: Int) {
        prefs.edit { putInt(PREF_SNOOZE_MINUTES, minutes.coerceAtLeast(1)) }
    }

    fun showTaskReminder(task: Task, isPreReminder: Boolean = false, soundUri: Uri? = null) {
        Log.d(TAG, "Showing task reminder for task ${task.id}: ${task.title}")
        wakeLockManager.acquireWakeLock()

        val notificationId = getNotificationId(task.id)
        val fullScreenPendingIntent = createFullScreenIntent(task)
        val completeAction = createCompleteAction(task.id)
        val snoozeAction = createSnoozeAction(task.id)

        val title = if (isPreReminder) {
            context.getString(R.string.pre_reminder_title_generic, task.title)
        } else {
            task.title
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(task.description)
            .setPriority(task.priority.toNotificationPriority())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(completeAction)
            .addAction(snoozeAction)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(VIBRATION_PATTERN)
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.description))
            .setContentIntent(fullScreenPendingIntent)
            .setSound(soundUri)
            .build()

        startVibration()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Notification posted for task ${task.id}")
        } else {
            Log.e(TAG, "Cannot show notification - missing POST_NOTIFICATIONS permission")
        }
    }

    private fun createFullScreenIntent(task: Task): PendingIntent {
        val intent = Intent(context, NotificationActionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
        }
        return PendingIntent.getActivity(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createActionIntent(action: String, taskId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val requestCode = when (action) {
            NotificationActionReceiver.ACTION_COMPLETE -> taskId * 10
            NotificationActionReceiver.ACTION_SNOOZE -> taskId * 10 + 1
            else -> taskId
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCompleteAction(taskId: Int): NotificationCompat.Action {
        val pendingIntent = createActionIntent(NotificationActionReceiver.ACTION_COMPLETE, taskId)
        return NotificationCompat.Action.Builder(
            R.drawable.ic_check,
            context.getString(R.string.notification_action_complete),
            pendingIntent
        ).build()
    }

    private fun createSnoozeAction(taskId: Int): NotificationCompat.Action {
        val pendingIntent = createActionIntent(NotificationActionReceiver.ACTION_SNOOZE, taskId)
        return NotificationCompat.Action.Builder(
            R.drawable.ic_snooze,
            context.getString(R.string.notification_action_snooze),
            pendingIntent
        ).build()
    }

    private fun startVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VIBRATION_PATTERN, 0)
            }
            Log.d(TAG, "Started continuous vibration")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    fun stopVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
            Log.d(TAG, "Stopped vibration")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }

    fun cancelNotification(taskId: Int) {
        NotificationManagerCompat.from(context).cancel(getNotificationId(taskId))
        stopVibration()
        wakeLockManager.releaseWakeLock()
        Log.d(TAG, "Cancelled notification for task $taskId")
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun getNotificationId(taskId: Int) = NOTIFICATION_ID_BASE + taskId

    private fun Priority.toNotificationPriority(): Int = when (this) {
        Priority.HIGH -> NotificationCompat.PRIORITY_MAX
        Priority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
        Priority.LOW -> NotificationCompat.PRIORITY_DEFAULT
    }

    private class WakeLockManager(context: Context) {

        private var wakeLock: PowerManager.WakeLock? = null
        private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        companion object {
            private const val TAG = "WakeLockManager"
        }

        fun acquireWakeLock() {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SmartTodo::TaskReminderWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                try {
                    wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
                    Log.d(TAG, "WakeLock acquired")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to acquire wakeLock", e)
                }
            }
        }

        fun releaseWakeLock() {
            if (wakeLock?.isHeld == true) {
                try {
                    wakeLock?.release()
                    Log.d(TAG, "WakeLock released")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to release wakeLock", e)
                }
            }
        }
    }
}
