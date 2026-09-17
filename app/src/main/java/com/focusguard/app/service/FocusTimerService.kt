package com.focusguard.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusguard.app.FocusGuardApplication
import com.focusguard.app.R
import com.focusguard.app.data.FocusPreferences
import com.focusguard.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class FocusTimerService : Service() {

    companion object {
        const val ACTION_START = "com.focusguard.app.action.START_FOCUS"
        const val ACTION_STOP = "com.focusguard.app.action.STOP_FOCUS"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"

        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002

        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context, durationSeconds: Int) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null
    private lateinit var focusPreferences: FocusPreferences

    override fun onCreate() {
        super.onCreate()
        focusPreferences = FocusPreferences.getInstance(this)
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
                startFocusTimer(durationSeconds)
            }
            ACTION_STOP -> {
                stopFocusTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startFocusTimer(durationSeconds: Int) {
        timerJob?.cancel()

        // Start Foreground Service with initial notification
        startForeground(NOTIFICATION_ID, buildTimerNotification(durationSeconds))

        serviceScope.launch {
            focusPreferences.startFocusSession(durationSeconds)
        }

        timerJob = serviceScope.launch {
            val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)

            while (isActive) {
                val remainingMillis = endTime - System.currentTimeMillis()
                if (remainingMillis <= 0) {
                    onTimerFinished()
                    break
                }

                val remainingSeconds = (remainingMillis / 1000).toInt()
                updateNotification(remainingSeconds)
                delay(1000)
            }
        }
    }

    private fun stopFocusTimer() {
        timerJob?.cancel()
        serviceScope.launch {
            focusPreferences.stopFocusSession()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerFinished() {
        serviceScope.launch {
            focusPreferences.stopFocusSession()
        }
        showCompletionNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(remainingSeconds: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildTimerNotification(remainingSeconds))
    }

    private fun buildTimerNotification(remainingSeconds: Int): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        val blockedCount = FocusPreferences.cachedBlockedPackages.size

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FocusGuardApplication.TIMER_CHANNEL_ID)
            .setContentTitle("🎯 집중 모드 진행 중 ($timeString)")
            .setContentText("현재 ${blockedCount}개의 방해 앱이 차단되어 있습니다.")
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showCompletionNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completionNotification = NotificationCompat.Builder(this, FocusGuardApplication.ALERT_CHANNEL_ID)
            .setContentTitle("🎉 집중 세션 완료!")
            .setContentText("설정하신 집중 시간을 끝까지 완수하셨습니다. 수고하셨습니다!")
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(COMPLETION_NOTIFICATION_ID, completionNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        timerJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
