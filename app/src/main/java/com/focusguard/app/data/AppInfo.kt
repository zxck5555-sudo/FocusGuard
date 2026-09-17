package com.focusguard.app.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean = false,
    val isBlocked: Boolean = false
)

data class DistractionPreset(
    val title: String,
    val description: String,
    val packageKeywords: List<String>
)
