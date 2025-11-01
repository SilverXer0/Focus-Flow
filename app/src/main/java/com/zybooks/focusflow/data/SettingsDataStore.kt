package com.zybooks.focusflow.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val DATASTORE_NAME = "focusflow_prefs"
val Context.focusFlowDataStore by preferencesDataStore(name = DATASTORE_NAME)

object SettingsKeys {
    val FOCUS_MIN = intPreferencesKey("focus_minutes")
    val SHORT_BREAK_MIN = intPreferencesKey("short_break_minutes")
    val LONG_BREAK_MIN = intPreferencesKey("long_break_minutes")
    val LONG_BREAK_EVERY = intPreferencesKey("long_break_every")
    val AUTO_START = booleanPreferencesKey("auto_start_next")
    val VIBRATIONS = booleanPreferencesKey("vibrations")
    val MOVEMENT_NUDGE = booleanPreferencesKey("movement_nudge")
}

data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val autoStartNext: Boolean = false,
    val vibrations: Boolean = true,
    val movementNudge: Boolean = true
)

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<TimerSettings> =
        context.focusFlowDataStore.data.map { prefs ->
            TimerSettings(
                focusMinutes = prefs[SettingsKeys.FOCUS_MIN] ?: 25,
                shortBreakMinutes = prefs[SettingsKeys.SHORT_BREAK_MIN] ?: 5,
                longBreakMinutes = prefs[SettingsKeys.LONG_BREAK_MIN] ?: 15,
                longBreakEvery = prefs[SettingsKeys.LONG_BREAK_EVERY] ?: 4,
                autoStartNext = prefs[SettingsKeys.AUTO_START] ?: false,
                vibrations = prefs[SettingsKeys.VIBRATIONS] ?: true,
                movementNudge = prefs[SettingsKeys.MOVEMENT_NUDGE] ?: true
            )
        }

    suspend fun updateFocusMinutes(min: Int) {
        context.focusFlowDataStore.edit { it[SettingsKeys.FOCUS_MIN] = min }
    }

    suspend fun updateShortBreakMinutes(min: Int) {
        context.focusFlowDataStore.edit { it[SettingsKeys.SHORT_BREAK_MIN] = min }
    }

    suspend fun updateLongBreakMinutes(min: Int) {
        context.focusFlowDataStore.edit { it[SettingsKeys.LONG_BREAK_MIN] = min }
    }

    suspend fun updateLongBreakEvery(n: Int) {
        context.focusFlowDataStore.edit { it[SettingsKeys.LONG_BREAK_EVERY] = n }
    }

    suspend fun updateAutoStart(enabled: Boolean) {
        context.focusFlowDataStore.edit { it[SettingsKeys.AUTO_START] = enabled }
    }

    suspend fun updateVibrations(enabled: Boolean) {
        context.focusFlowDataStore.edit { it[SettingsKeys.VIBRATIONS] = enabled }
    }

    suspend fun updateMovementNudge(enabled: Boolean) {
        context.focusFlowDataStore.edit { it[SettingsKeys.MOVEMENT_NUDGE] = enabled }
    }

    suspend fun resetStats() {
        // add in later once add session logs
    }

    suspend fun exportSummaryToClipboard(): String {
        // placeholder string for now
        return runBlocking { "FocusFlow summary (placeholder)" }
    }
}