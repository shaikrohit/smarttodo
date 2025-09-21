package com.example.smarttodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smarttodo.data.Task
import com.example.smarttodo.util.NotificationHelper

class SnoozedNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_SNOOZED_NOTIFICATION = "com.example.smarttodo.ACTION_SHOW_SNOOZED_NOTIFICATION"
        const val EXTRA_TASK_OBJECT = "extra_task_object"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_SNOOZED_NOTIFICATION) {
            val task: Task? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_TASK_OBJECT, Task::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_TASK_OBJECT) as? Task
            }

            task?.let {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showTaskReminder(it)
            }
        }
    }
}
