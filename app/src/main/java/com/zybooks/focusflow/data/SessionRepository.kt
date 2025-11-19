package com.zybooks.focusflow.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionsDataStore by preferencesDataStore(name = "sessions_prefs")

class SessionRepository(
    private val context: Context
) {
    private val SESSIONS_KEY = stringPreferencesKey("sessions_serialized")
    val sessionsFlow: Flow<List<FocusSession>> =
        context.sessionsDataStore.data.map { prefs ->
            val raw = prefs[SESSIONS_KEY] ?: ""
            decodeSessions(raw)
        }

    suspend fun addSession(session: FocusSession) {
        context.sessionsDataStore.edit { prefs ->
            val current = decodeSessions(prefs[SESSIONS_KEY] ?: "")
            val updated = current + session
            prefs[SESSIONS_KEY] = encodeSessions(updated)
        }
    }

    suspend fun clearSessions() {
        context.sessionsDataStore.edit { prefs ->
            prefs.remove(SESSIONS_KEY)
        }
    }

    private fun encodeSessions(list: List<FocusSession>): String {
        return list.joinToString("|") { s ->
            listOf(
                s.id.toString(),
                s.startTimeMillis.toString(),
                s.durationMinutes.toString(),
                s.type.replace("|", "").replace(",", ""), // sanitize a bit
                if (s.completed) "1" else "0"
            ).joinToString(",")
        }
    }

    private fun decodeSessions(raw: String): List<FocusSession> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { part ->
            val pieces = part.split(",")
            if (pieces.size < 5) return@mapNotNull null
            try {
                FocusSession(
                    id = pieces[0].toLong(),
                    startTimeMillis = pieces[1].toLong(),
                    durationMinutes = pieces[2].toInt(),
                    type = pieces[3],
                    completed = (pieces[4] == "1")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}