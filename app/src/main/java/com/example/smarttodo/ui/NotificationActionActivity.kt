package com.example.smarttodo.ui

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.smarttodo.utils.NotificationActionReceiver
import com.example.smarttodo.utils.NotificationHelper
import com.example.smarttodo.utils.NotificationManager
import com.example.smarttodo.utils.WakeLockManager

/**
 * Transparent activity that opens when a user clicks on a notification action
 * Helps display dialogs from notification actions
 */
class NotificationActionActivity : AppCompatActivity(), SnoozeSelectionDialog.SnoozeSelectionListener {

    private var taskId: Int = -1
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val ACTION_SNOOZE = "action_snooze"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the task ID and action from the intent
        taskId = intent.getIntExtra(NotificationActionReceiver.EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task"
        val action = intent.getStringExtra(EXTRA_ACTION)

        if (taskId == -1) {
            finish()
            return
        }

        // Acquire a wake lock to make sure we complete the action
        wakeLock = WakeLockManager.acquireWakeLock(this, "NotificationActionActivity:$taskId")

        val notificationHelper = NotificationHelper(this)
        notificationHelper.stopVibration()
        notificationHelper.cancelNotification(taskId)

        if (action == ACTION_SNOOZE) {
            val dialog = SnoozeSelectionDialog.newInstance(taskId, taskTitle)
            dialog.show(supportFragmentManager, "SnoozeSelectionDialog")
        } else {
            // Handle other actions or finish
            finish()
        }
    }

    override fun onSnoozeDurationSelected(taskId: Int, durationMillis: Long) {
        Log.d("NotificationActionActivity", "User selected snooze duration: ${durationMillis}ms for task $taskId")

        // Send broadcast with snooze duration to NotificationActionReceiver
        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_DURATION, durationMillis)
        }
        sendBroadcast(snoozeIntent)
        finish()
    }

    override fun onSnoozeCancel() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        WakeLockManager.releaseWakeLock(wakeLock)
    }
}
