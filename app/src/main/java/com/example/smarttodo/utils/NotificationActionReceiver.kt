package com.example.smarttodo.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.example.smarttodo.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.smarttodo.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if(taskId == -1) return

        when(intent.action) {
            ACTION_COMPLETE -> completeTask(context, taskId)
            ACTION_SNOOZE -> snoozeTask(context, taskId)
        }
    }

    private fun completeTask(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = TaskDatabase.getDatabase(context)
            val repository = TaskRepository(database.taskDao())
            val task = repository.getTaskById(taskId)
            task?.let {
                repository.toggleTaskCompletion(it)
                NotificationHelper(context).cancelNotification(taskId)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Task completed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun snoozeTask(context: Context, taskId: Int) {
        NotificationHelper(context).cancelNotification(taskId)
        Toast.makeText(context, "Task snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
    }
}
