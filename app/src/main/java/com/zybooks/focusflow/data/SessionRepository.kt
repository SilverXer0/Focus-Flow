package com.zybooks.focusflow.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SessionRepository(
    context: Context
) {
    private val _sessions = MutableStateFlow<List<FocusSession>>(emptyList())
    val sessionsFlow: Flow<List<FocusSession>> = _sessions

    suspend fun addSession(session: FocusSession) {
        _sessions.value = _sessions.value + session
    }

    suspend fun clearSessions() {
        _sessions.value = emptyList()
    }
}