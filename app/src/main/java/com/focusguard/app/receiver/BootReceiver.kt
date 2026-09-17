package com.focusguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusguard.app.data.FocusPreferences
import com.focusguard.app.service.FocusTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = FocusPreferences.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                val isActive = prefs.isFocusActiveFlow.first()
                val endTime = prefs.focusEndTimeFlow.first()
                val now = System.currentTimeMillis()

                if (isActive && endTime > now) {
                    val remainingSeconds = ((endTime - now) / 1000).toInt()
                    if (remainingSeconds > 0) {
                        FocusTimerService.startService(context, remainingSeconds)
                    } else {
                        prefs.stopFocusSession()
                    }
                } else {
                    prefs.stopFocusSession()
                }
            }
        }
    }
}
