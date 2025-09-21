package com.example.smarttodo.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.smarttodo.MainActivity
import com.example.smarttodo.R
import com.example.smarttodo.data.Task

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "smart_todo_reminders"
        const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Reminders"
            val descriptionText = "Notifications for task reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH // Changed to HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Default sound can be set here or overridden per notification
                // If you want a default sound for the channel, uncomment and set
                // setSound(defaultSoundUri, null)
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(task: Task, soundUri: Uri? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Action to complete the task
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            task.id * 10, // Unique request code
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val completeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_check, // You might need a specific icon for "complete"
            "Complete",
            completePendingIntent
        ).build()

        // Action to snooze the task
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, task.id)
        }
        val snoozePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            task.id * 20, // Unique request code
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_timer, // You might need a specific icon for "snooze"
            "Snooze",
            snoozePendingIntent
        ).build()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Reminder")
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if(task.description.isNotEmpty()) "${task.title}\n${task.description}" else task.title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // This sets priority for pre-Oreo. For Oreo+, channel importance is key.
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(completeAction) // Add the complete action
            .addAction(snoozeAction)   // Add the snooze action

        // Set custom sound if provided
        soundUri?.let {
            builder.setSound(it)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_BASE + task.id, builder.build())
            }
        }
    }

    fun cancelNotification(taskId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_BASE + taskId)
        }
    }
}