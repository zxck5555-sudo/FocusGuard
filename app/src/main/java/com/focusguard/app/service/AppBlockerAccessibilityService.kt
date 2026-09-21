package com.focusguard.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.focusguard.app.data.AppManager
import com.focusguard.app.data.FocusPreferences
import com.focusguard.app.ui.blocker.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var focusPreferences: FocusPreferences
    private lateinit var appManager: AppManager
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    companion object {
        var isServiceRunning: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        focusPreferences = FocusPreferences.getInstance(this)
        appManager = AppManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Skip our own app or common system packages
        if (packageName == applicationContext.packageName ||
            packageName == "com.android.systemui" ||
            packageName.contains("launcher") ||
            packageName.contains("recents")
        ) {
            return
        }

        if (focusPreferences.isAppCurrentlyBlocked(packageName)) {
            val now = System.currentTimeMillis()
            // 600ms debounce to prevent redundant triggers from multiple window events
            if (packageName == lastBlockedPackage && (now - lastBlockedTimestamp) < 600) {
                return
            }

            lastBlockedPackage = packageName
            lastBlockedTimestamp = now

            // 1. Trigger warning vibration
            triggerHapticAlert()

            // 2. Increment statistics
            serviceScope.launch {
                focusPreferences.incrementBlockedAttempts()
            }

            // 3. Launch full-screen blocker UI
            val appName = appManager.getAppName(packageName)
            val blockerIntent = Intent(this, BlockerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(BlockerActivity.EXTRA_BLOCKED_PACKAGE, packageName)
                putExtra(BlockerActivity.EXTRA_BLOCKED_APP_NAME, appName)
            }
            startActivity(blockerIntent)
        }
    }

    private fun triggerHapticAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    override fun onInterrupt() {
        // Called when system interrupts the service
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }
}
