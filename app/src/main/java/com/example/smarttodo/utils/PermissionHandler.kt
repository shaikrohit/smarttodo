package com.example.smarttodo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.example.smarttodo.R

object PermissionHandler {

    fun showPermissionRationale(
        activity: Activity,
        message: String,
        onPositiveAction: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setMessage(message)
            .setPositiveButton(activity.getString(R.string.go_to_settings)) { _, _ ->
                onPositiveAction()
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .show()
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}
