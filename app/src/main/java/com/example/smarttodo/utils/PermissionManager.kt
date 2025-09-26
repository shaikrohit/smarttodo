package com.example.smarttodo.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smarttodo.R

/**
 * Helper class for managing notification and other permissions
 */
class PermissionManager(private val activity: AppCompatActivity) {

    private val TAG = "PermissionManager"

    // Permission request launcher for notification permissions
    private val requestPermissionLauncher: ActivityResultLauncher<String> = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted!")
            showToast(activity.getString(R.string.notifications_permission_granted))
        } else {
            Log.d(TAG, "Notification permission denied!")
            showToast(activity.getString(R.string.notifications_permission_denied))
            showNotificationPermissionRationale()
        }
    }

    /**
     * Check and request notification permission if needed
     * Returns true if permission is already granted
     */
    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    true
                }
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale if needed
                    showNotificationPermissionRationale()
                    false
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    false
                }
            }
        } else {
            // Notification permissions are granted by default for older Android versions
            true
        }
    }

    /**
     * Check vibration permission. This is a normal permission and doesn't need runtime request,
     * but we'll verify it's in the manifest and inform the user if it's not
     */
    fun checkVibrationPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.e(TAG, "Vibration permission is missing! This should be declared in the manifest.")
            // This is unlikely to happen if the permission is in the manifest, but just in case
            showToast("Vibration permission is required for notifications!")
        }

        return hasPermission
    }

    /**
     * Show a dialog explaining why notification permissions are needed
     */
    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.notification_permission_title))
            .setMessage(activity.getString(R.string.notification_permission_rationale))
            .setPositiveButton(activity.getString(R.string.go_to_settings)) { _, _ ->
                // Open app settings so the user can enable notifications
                openAppSettings()
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .show()
    }

    /**
     * Open the app's settings screen
     */
    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + activity.packageName)
            activity.startActivity(this)
        }
    }

    /**
     * Show a toast message
     */
    private fun showToast(message: String) {
        android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
