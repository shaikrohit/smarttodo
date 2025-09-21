package com.example.smarttodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.smarttodo.SmartTodoApplication
// import com.example.smarttodo.util.NotificationHelper // Not strictly needed for NOTIFICATION_ID_PREFIX if we use passed ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MarkTaskCompleteReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_COMPLETE = "com.example.smarttodo.ACTION_MARK_COMPLETE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "com.example.smarttodo.EXTRA_MARK_COMPLETE_NOTIFICATION_ID" // Added
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_COMPLETE) {
            val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1) // Retrieve the notification ID

            if (taskId != -1 && notificationId != -1) {
                val repository = (context.applicationContext as SmartTodoApplication).repository
                CoroutineScope(Dispatchers.IO).launch {
                    val task = repository.getTaskById(taskId)
                    task?.let {
                        // Only mark complete if it's not already (though UI might prevent this)
                        if (!it.isCompleted) {
                            val updatedTask = it.copy(isCompleted = true)
                            repository.update(updatedTask)
                        }

                        // Dismiss the specific notification that was actioned
                        NotificationManagerCompat.from(context).cancel(notificationId)
                        // Optionally, you might want to show a Toast or update UI if the app is open
                    }
                }
            }
        }
    }
}
