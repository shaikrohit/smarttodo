package com.example.smarttodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri // For String?.toUri()
import com.example.smarttodo.data.Task
import com.example.smarttodo.util.AlarmScheduler // Import for EXTRA_IS_PRE_REMINDER
import com.example.smarttodo.util.NotificationHelper

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_TASK_REMINDER = "com.example.smarttodo.ACTION_SHOW_TASK_REMINDER"
        const val EXTRA_TASK_OBJECT = "extra_task_object_reminder"
        const val EXTRA_TASK_ID = "com.example.smarttodo.EXTRA_TASK_ID_REMINDER" // Added constant
        const val EXTRA_SOUND_URI_STRING = "com.example.smarttodo.EXTRA_SOUND_URI_STRING" // New constant
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_TASK_REMINDER) {
            val task: Task? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_TASK_OBJECT, Task::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_TASK_OBJECT) as? Task
            }

            // val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1) // If you need to retrieve it
            val isPreReminder = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
            val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI_STRING) // Retrieve sound URI string
            val explicitSoundUri = soundUriString?.toUri() // Convert to Uri, will be null if string is null

            task?.let {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showTaskReminder(it, isPreReminder, explicitSoundUri) // Pass the explicit sound URI
            }
        }
    }
}
