package com.example.smarttodo.diagnostics

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager

/**
 * Comprehensive diagnostic utility to verify notification system health
 * and prevent future vibration and snooze issues
 */
object NotificationSystemDiagnostics {
    private const val TAG = "NotificationDiagnostics"

    /**
     * Performs complete system health check and reports any issues
     */
    fun performCompleteHealthCheck(context: Context): HealthCheckReport {
        Log.i(TAG, "🔍 Starting comprehensive notification system health check...")

        val report = HealthCheckReport()

        // Check vibration capability
        report.vibrationStatus = checkVibrationSystem(context)

        // Check notification permissions
        report.notificationPermissions = checkNotificationPermissions(context)

        // Check alarm scheduling permissions
        report.alarmPermissions = checkAlarmPermissions(context)

        // Check audio settings
        report.audioSettings = checkAudioSettings(context)

        // Test vibration functionality
        report.vibrationTest = testVibrationFunctionality(context)

        // Generate overall health score
        report.overallHealth = calculateOverallHealth(report)

        val summary = generateHealthSummary(report)
        Log.i(TAG, "🏥 Health Check Complete:\n$summary")

        return report
    }

    private fun checkVibrationSystem(context: Context): VibrationStatus {
        return try {
            val hasVibratePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.VIBRATE
            ) == PackageManager.PERMISSION_GRANTED

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val hasVibrator = vibrator.hasVibrator()

            VibrationStatus(
                hasPermission = hasVibratePermission,
                hasVibrator = hasVibrator,
                isWorking = hasVibratePermission && hasVibrator,
                details = "Permission: $hasVibratePermission, Hardware: $hasVibrator"
            )
        } catch (e: Exception) {
            VibrationStatus(
                hasPermission = false,
                hasVibrator = false,
                isWorking = false,
                details = "Error: ${e.message}"
            )
        }
    }

    private fun checkNotificationPermissions(context: Context): PermissionStatus {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 13
        }

        return PermissionStatus(
            granted = hasPermission,
            required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            details = if (hasPermission) "Granted" else "Missing POST_NOTIFICATIONS"
        )
    }

    private fun checkAlarmPermissions(context: Context): PermissionStatus {
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Not required before Android 12
        }

        return PermissionStatus(
            granted = canScheduleExact,
            required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            details = if (canScheduleExact) "Can schedule exact alarms" else "Cannot schedule exact alarms"
        )
    }

    private fun checkAudioSettings(context: Context): AudioStatus {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = audioManager.ringerMode
        val isNotificationAllowed = when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> true
            AudioManager.RINGER_MODE_VIBRATE -> false // Sound off, vibration only
            AudioManager.RINGER_MODE_SILENT -> false // Everything off
            else -> true
        }

        return AudioStatus(
            ringerMode = ringerMode,
            soundAllowed = isNotificationAllowed,
            vibrationAllowed = ringerMode != AudioManager.RINGER_MODE_SILENT,
            details = "Ringer mode: $ringerMode"
        )
    }

    private fun testVibrationFunctionality(context: Context): TestResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator

                // Test short vibration
                val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)

                TestResult(true, "Vibration test successful (API 31+)")
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }

                TestResult(true, "Vibration test successful (legacy)")
            }
        } catch (e: Exception) {
            TestResult(false, "Vibration test failed: ${e.message}")
        }
    }

    private fun calculateOverallHealth(report: HealthCheckReport): HealthScore {
        var score = 0
        var maxScore = 0

        // Vibration system (30 points)
        maxScore += 30
        if (report.vibrationStatus.isWorking) score += 30
        else if (report.vibrationStatus.hasVibrator) score += 15

        // Notification permissions (25 points)
        maxScore += 25
        if (report.notificationPermissions.granted) score += 25

        // Alarm permissions (25 points)
        maxScore += 25
        if (report.alarmPermissions.granted) score += 25

        // Audio settings (20 points)
        maxScore += 20
        if (report.audioSettings.vibrationAllowed) score += 10
        if (report.audioSettings.soundAllowed) score += 10

        val percentage = (score * 100) / maxScore
        val level = when {
            percentage >= 90 -> HealthLevel.EXCELLENT
            percentage >= 75 -> HealthLevel.GOOD
            percentage >= 50 -> HealthLevel.FAIR
            else -> HealthLevel.POOR
        }

        return HealthScore(score, maxScore, percentage, level)
    }

    private fun generateHealthSummary(report: HealthCheckReport): String {
        return buildString {
            appendLine("📊 NOTIFICATION SYSTEM HEALTH REPORT")
            appendLine("=====================================")
            appendLine("Overall Health: ${report.overallHealth.level} (${report.overallHealth.percentage}%)")
            appendLine("")
            appendLine("🔸 Vibration System: ${if (report.vibrationStatus.isWorking) "✅ WORKING" else "❌ ISSUES"}")
            appendLine("   ${report.vibrationStatus.details}")
            appendLine("")
            appendLine("🔸 Notification Permissions: ${if (report.notificationPermissions.granted) "✅ GRANTED" else "❌ MISSING"}")
            appendLine("   ${report.notificationPermissions.details}")
            appendLine("")
            appendLine("🔸 Alarm Permissions: ${if (report.alarmPermissions.granted) "✅ GRANTED" else "❌ MISSING"}")
            appendLine("   ${report.alarmPermissions.details}")
            appendLine("")
            appendLine("🔸 Audio Settings: ${if (report.audioSettings.vibrationAllowed) "✅ VIBRATION OK" else "❌ SILENT MODE"}")
            appendLine("   ${report.audioSettings.details}")
            appendLine("")
            appendLine("🔸 Vibration Test: ${if (report.vibrationTest.success) "✅ PASSED" else "❌ FAILED"}")
            appendLine("   ${report.vibrationTest.details}")
        }
    }

    data class HealthCheckReport(
        var vibrationStatus: VibrationStatus = VibrationStatus(),
        var notificationPermissions: PermissionStatus = PermissionStatus(),
        var alarmPermissions: PermissionStatus = PermissionStatus(),
        var audioSettings: AudioStatus = AudioStatus(),
        var vibrationTest: TestResult = TestResult(),
        var overallHealth: HealthScore = HealthScore()
    )

    data class VibrationStatus(
        val hasPermission: Boolean = false,
        val hasVibrator: Boolean = false,
        val isWorking: Boolean = false,
        val details: String = ""
    )

    data class PermissionStatus(
        val granted: Boolean = false,
        val required: Boolean = false,
        val details: String = ""
    )

    data class AudioStatus(
        val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
        val soundAllowed: Boolean = false,
        val vibrationAllowed: Boolean = false,
        val details: String = ""
    )

    data class TestResult(
        val success: Boolean = false,
        val details: String = ""
    )

    data class HealthScore(
        val score: Int = 0,
        val maxScore: Int = 0,
        val percentage: Int = 0,
        val level: HealthLevel = HealthLevel.POOR
    )

    enum class HealthLevel {
        EXCELLENT, GOOD, FAIR, POOR
    }
}
