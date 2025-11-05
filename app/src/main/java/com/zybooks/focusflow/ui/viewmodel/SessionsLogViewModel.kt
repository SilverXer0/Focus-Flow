package com.zybooks.focusflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zybooks.focusflow.data.FocusSession
import com.zybooks.focusflow.data.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionsLogViewModel(
    private val repo: SessionRepository
) : ViewModel() {

    val sessions: StateFlow<List<FocusSession>> =
        repo.sessionsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearLog() {
        viewModelScope.launch {
            repo.clearSessions()
        }
    }

    companion object {
        fun factory(repo: SessionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SessionsLogViewModel(repo) as T
                }
            }
    }
}