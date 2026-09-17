package com.focusguard.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppManager(private val context: Context) {

    // Common addictive/distracting packages for quick selection
    val commonDistractingPackages = listOf(
        "com.google.android.youtube",        // YouTube
        "com.instagram.android",             // Instagram
        "com.zhiliaoapp.musically",          // TikTok
        "com.ss.android.ugc.trill",          // TikTok alternative
        "com.twitter.android",               // X (Twitter)
        "com.facebook.katana",               // Facebook
        "com.netflix.mediaclient",           // Netflix
        "com.frograms.wplay",                // Watcha
        "com.disney.disneyplus",             // Disney+
        "com.tving.player",                  // Tving
        "tv.cjenm.tving",                    // Tving old
        "com.iloen.melon",                   // Melon
        "com.kakao.talk",                    // KakaoTalk
        "com.nhn.android.webtoon",           // Naver Webtoon
        "com.android.chrome"                 // Chrome
    )

    suspend fun getInstalledApps(blockedPackages: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val appList = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            // Exclude our own app
            if (pkg == context.packageName || seenPackages.contains(pkg)) {
                continue
            }
            seenPackages.add(pkg)

            try {
                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                appList.add(
                    AppInfo(
                        packageName = pkg,
                        appName = appName,
                        icon = icon,
                        isSystemApp = isSystem,
                        isBlocked = blockedPackages.contains(pkg)
                    )
                )
            } catch (e: Exception) {
                // Ignore if package cannot be resolved
            }
        }

        // Sort: Blocked apps first, then alphabetically
        appList.sortedWith(
            compareByDescending<AppInfo> { it.isBlocked }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
