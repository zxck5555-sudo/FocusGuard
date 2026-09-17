package com.focusguard.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_guard_prefs")

class FocusPreferences(private val context: Context) {

    companion object {
        @Volatile
        private var instance: FocusPreferences? = null

        fun getInstance(context: Context): FocusPreferences {
            return instance ?: synchronized(this) {
                instance ?: FocusPreferences(context.applicationContext).also { instance = it }
            }
        }

        private val KEY_BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        private val KEY_IS_FOCUS_ACTIVE = booleanPreferencesKey("is_focus_active")
        private val KEY_FOCUS_END_TIME = longPreferencesKey("focus_end_time")
        private val KEY_FOCUS_TOTAL_SECONDS = intPreferencesKey("focus_total_seconds")
        private val KEY_STRICT_MODE = booleanPreferencesKey("strict_mode")
        private val KEY_TOTAL_BLOCKED_ATTEMPTS = intPreferencesKey("total_blocked_attempts")

        // In-memory cache for ultra-fast zero-latency lookup in AccessibilityService
        @Volatile
        var cachedBlockedPackages: Set<String> = emptySet()
            private set

        @Volatile
        var cachedIsFocusActive: Boolean = false
            private set

        @Volatile
        var cachedFocusEndTime: Long = 0L
            private set

        @Volatile
        var cachedStrictMode: Boolean = true
            private set
    }

    init {
        // Keep in-memory cache synchronized with DataStore
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.collect { prefs ->
                cachedBlockedPackages = prefs[KEY_BLOCKED_PACKAGES] ?: emptySet()
                cachedIsFocusActive = prefs[KEY_IS_FOCUS_ACTIVE] ?: false
                cachedFocusEndTime = prefs[KEY_FOCUS_END_TIME] ?: 0L
                cachedStrictMode = prefs[KEY_STRICT_MODE] ?: true
            }
        }
    }

    val blockedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLOCKED_PACKAGES] ?: emptySet()
    }

    val isFocusActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val active = prefs[KEY_IS_FOCUS_ACTIVE] ?: false
        val endTime = prefs[KEY_FOCUS_END_TIME] ?: 0L
        active && (endTime > System.currentTimeMillis())
    }

    val focusEndTimeFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_FOCUS_END_TIME] ?: 0L
    }

    val focusTotalSecondsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_FOCUS_TOTAL_SECONDS] ?: (25 * 60)
    }

    val strictModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_STRICT_MODE] ?: true
    }

    val totalBlockedAttemptsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOTAL_BLOCKED_ATTEMPTS] ?: 0
    }

    suspend fun setBlockedPackages(packages: Set<String>) {
        cachedBlockedPackages = packages
        context.dataStore.edit { prefs ->
            prefs[KEY_BLOCKED_PACKAGES] = packages
        }
    }

    suspend fun toggleBlockedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[KEY_BLOCKED_PACKAGES] = current
            cachedBlockedPackages = current
        }
    }

    suspend fun startFocusSession(durationSeconds: Int) {
        val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)
        cachedIsFocusActive = true
        cachedFocusEndTime = endTime
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_FOCUS_ACTIVE] = true
            prefs[KEY_FOCUS_END_TIME] = endTime
            prefs[KEY_FOCUS_TOTAL_SECONDS] = durationSeconds
        }
    }

    suspend fun stopFocusSession() {
        cachedIsFocusActive = false
        cachedFocusEndTime = 0L
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_FOCUS_ACTIVE] = false
            prefs[KEY_FOCUS_END_TIME] = 0L
        }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        cachedStrictMode = enabled
        context.dataStore.edit { prefs ->
            prefs[KEY_STRICT_MODE] = enabled
        }
    }

    suspend fun incrementBlockedAttempts() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_TOTAL_BLOCKED_ATTEMPTS] ?: 0
            prefs[KEY_TOTAL_BLOCKED_ATTEMPTS] = current + 1
        }
    }

    fun isAppCurrentlyBlocked(packageName: String): Boolean {
        if (!cachedIsFocusActive) return false
        if (System.currentTimeMillis() >= cachedFocusEndTime) return false
        return cachedBlockedPackages.contains(packageName)
    }
}
