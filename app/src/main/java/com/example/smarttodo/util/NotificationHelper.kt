package com.example.smarttodo.util
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit // KTX for SharedPreferences
import androidx.core.net.toUri // KTX for String to Uri
import com.example.smarttodo.MainActivity
import com.example.smarttodo.R
import com.example.smarttodo.data.Task
// Assuming your TaskDetailActivity is in this package, adjust if necessary
import com.example.smarttodo.ui.TaskDetailActivity 
import com.example.smarttodo.receiver.MarkTaskCompleteReceiver
import com.example.smarttodo.receiver.SnoozeTaskReceiver

enum class SoundMode {
    DEFAULT,
    CUSTOM,
    SILENT
}

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val NOTIFICATION_ID_PREFIX = 1000 // For main reminders
        const val PRE_REMINDER_NOTIFICATION_ID_OFFSET = 200000 // To differentiate pre-reminder notification IDs
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_NOTIFICATION_SOUND_URI = "notification_sound_uri"
        private const val KEY_NOTIFICATION_SOUND_MODE = "notification_sound_mode"
        private const val KEY_SNOOZE_DURATION_MINUTES = "snooze_duration_minutes"
        const val DEFAULT_SNOOZE_MINUTES = 5
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID" // Define key, assuming it's used by TaskDetailActivity
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

                val (soundMode, customSoundUri) = getNotificationSoundSettings()
                val soundUriToSet: Uri? = when (soundMode) {
                    SoundMode.CUSTOM -> customSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    SoundMode.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    SoundMode.SILENT -> null
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUriToSet, audioAttributes)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(task: Task, isPreReminder: Boolean = false, explicitSoundUri: Uri? = null) {
        val baseNotificationId = task.id
        val notificationId = if (isPreReminder) {
            NOTIFICATION_ID_PREFIX + baseNotificationId + PRE_REMINDER_NOTIFICATION_ID_OFFSET
        } else {
            NOTIFICATION_ID_PREFIX + baseNotificationId
        }

        // Intent to open TaskDetailActivity
        val taskDetailIntent = Intent(context, TaskDetailActivity::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id) // Pass task ID
            // Ensure the intent is unique for different tasks if TaskDetailActivity handles updates
            // by setting a unique action or data uri if needed, e.g., setData(Uri.parse("task://${task.id}"))
            action = Intent.ACTION_VIEW + "_" + task.id // Make action unique for pending intent
        }

        // Create back stack
        val openAppPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
            addNextIntent(taskDetailIntent)
            getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val markCompleteIntent = Intent(context, MarkTaskCompleteReceiver::class.java).apply {
            action = MarkTaskCompleteReceiver.ACTION_MARK_COMPLETE
            putExtra(MarkTaskCompleteReceiver.EXTRA_TASK_ID, task.id)
            putExtra(MarkTaskCompleteReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markCompletePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            markCompleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUriToPlay: Uri? = explicitSoundUri ?: run {
            val (currentSoundMode, currentCustomSoundUri) = getNotificationSoundSettings()
            when (currentSoundMode) {
                SoundMode.CUSTOM -> currentCustomSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                SoundMode.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                SoundMode.SILENT -> null
            }
        }

        val snoozeIntent = Intent(context, SnoozeTaskReceiver::class.java).apply {
            action = SnoozeTaskReceiver.ACTION_SNOOZE_TASK
            putExtra(SnoozeTaskReceiver.EXTRA_TASK_ID, task.id)
            putExtra(SnoozeTaskReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, isPreReminder)
            putExtra(SnoozeTaskReceiver.EXTRA_SOUND_URI_STRING, soundUriToPlay?.toString())
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isPreReminder) {
            val offsetMinutes = task.preReminderOffsetMinutes
            if (offsetMinutes != null && task.dueDate != null) {
                context.getString(R.string.pre_reminder_title_dynamic, task.title, offsetMinutes.toString())
            } else {
                 context.getString(R.string.pre_reminder_title_generic, task.title)
            }
        } else {
            task.title
        }

        val contentText = task.description.takeIf { it.isNotEmpty() } ?: context.getString(R.string.default_task_reminder_content)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent) // Use the TaskStackBuilder pending intent
            .setAutoCancel(true)
            .setSound(soundUriToPlay)
            .addAction(R.drawable.ic_action_complete, context.getString(R.string.notification_action_complete), markCompletePendingIntent)
            .addAction(R.drawable.ic_action_snooze, context.getString(R.string.notification_action_snooze), snoozePendingIntent)

        if (soundUriToPlay == null) { 
            builder.setVibrate(longArrayOf(0, 250, 250, 250))
        } else {
             builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e("NotificationHelper", "SecurityException: Missing POST_NOTIFICATIONS permission?", e)
            }
        }
    }

    fun saveNotificationSettings(mode: SoundMode, uri: Uri? = null) {
        prefs.edit {
            putString(KEY_NOTIFICATION_SOUND_MODE, mode.name)
            if (mode == SoundMode.CUSTOM && uri != null) {
                putString(KEY_NOTIFICATION_SOUND_URI, uri.toString())
            } else {
                remove(KEY_NOTIFICATION_SOUND_URI)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.deleteNotificationChannel(CHANNEL_ID) 
            createNotificationChannel()
        }
    }

    fun getNotificationSoundSettings(): Pair<SoundMode, Uri?> {
        val modeName = prefs.getString(KEY_NOTIFICATION_SOUND_MODE, SoundMode.DEFAULT.name)
        val soundMode = SoundMode.valueOf(modeName ?: SoundMode.DEFAULT.name)
        val soundString = prefs.getString(KEY_NOTIFICATION_SOUND_URI, null)
        val customSoundUri = soundString?.toUri()
        return Pair(soundMode, customSoundUri)
    }

    fun saveSnoozeDuration(minutes: Int) {
        prefs.edit {
            putInt(KEY_SNOOZE_DURATION_MINUTES, minutes)
        }
    }

    fun getSnoozeDuration(): Int {
        return prefs.getInt(KEY_SNOOZE_DURATION_MINUTES, DEFAULT_SNOOZE_MINUTES)
    }
}
