package com.zybooks.focusflow.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private const val DATASTORE_NAME = "focusflow_prefs"

val Context.focusFlowDataStore by preferencesDataStore(name = DATASTORE_NAME)

object SettingsKeys {
    val FOCUS_MIN = intPreferencesKey("focus_minutes")
    val SHORT_BREAK_MIN = intPreferencesKey("short_break_minutes")
    val LONG_BREAK_MIN = intPreferencesKey("long_break_minutes")
    val LONG_BREAK_EVERY = intPreferencesKey("long_break_every")
    val AUTO_START = booleanPreferencesKey("auto_start_next")
}

data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val autoStartNext: Boolean = false
)

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<TimerSettings> =
        context.focusFlowDataStore.data.map { prefs ->
            TimerSettings(
                focusMinutes = prefs[SettingsKeys.FOCUS_MIN] ?: 25,
                shortBreakMinutes = prefs[SettingsKeys.SHORT_BREAK_MIN] ?: 5,
                longBreakMinutes = prefs[SettingsKeys.LONG_BREAK_MIN] ?: 15,
                longBreakEvery = prefs[SettingsKeys.LONG_BREAK_EVERY] ?: 4,
                autoStartNext = prefs[SettingsKeys.AUTO_START] ?: false
            )
        }

    suspend fun updateAutoStart(auto: Boolean) {
        context.focusFlowDataStore.edit { it[SettingsKeys.AUTO_START] = auto }
    }

    suspend fun updateFocusMinutes(min: Int) {
        context.focusFlowDataStore.edit { it[SettingsKeys.FOCUS_MIN] = min }
    }

}