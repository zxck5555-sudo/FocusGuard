package com.focusguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

import com.google.android.gms.ads.MobileAds

class FocusGuardApplication : Application() {

    companion object {
        const val TIMER_CHANNEL_ID = "focus_guard_timer_channel"
        const val ALERT_CHANNEL_ID = "focus_guard_alert_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Initialize Google Mobile Ads SDK on startup
        MobileAds.initialize(this) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Focus Timer Foreground Service Channel
            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                "집중 타이머 알림",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "집중 모드 진행 중 남은 시간을 표시합니다."
                setShowBadge(false)
            }

            // Focus Alerts & Interception Channel
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "차단 알림 및 세션 완료",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "차단 앱 감지 및 집중 시간 완료 알림을 제공합니다."
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
