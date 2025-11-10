package com.zybooks.focusflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zybooks.focusflow.data.FocusSession
import com.zybooks.focusflow.data.SessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class StatsUiState(
    val totalMinutesThisWeek: Int = 0,
    val sessionsThisWeek: Int = 0,
    val streakDays: Int = 0,
    val avgFocusMinutes: Int = 0,
    val weekBars: List<Int> = emptyList()
)

class StatsViewModel(
    private val repo: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        viewModelScope.launch {
            repo.sessionsFlow.collect { sessions ->
                _uiState.value = buildStats(sessions)
            }
        }
    }

    private fun buildStats(sessions: List<FocusSession>): StatsUiState {
        if (sessions.isEmpty()) return StatsUiState()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayStart = cal.timeInMillis
        val oneDay = 24L * 60L * 60L * 1000L

        // make buckets for last 7 days: index 0 = today
        val dailyMinutes = MutableList(7) { 0 }

        for (s in sessions) {
            val diffDays = ((todayStart - s.startTimeMillis) / oneDay).toInt()
            if (diffDays in 0..6) {
                dailyMinutes[diffDays] += s.durationMinutes
            }
        }

        val totalWeek = dailyMinutes.sum()
        val sessionsThisWeek = dailyMinutes.sumOf { if (it > 0) 1 else 0 as Int }

        var streak = 0
        for (i in 0 until 7) {
            if (dailyMinutes[i] > 0) streak++ else break
        }

        val avg = if (sessions.isNotEmpty()) {
            sessions.map { it.durationMinutes }.average().toInt()
        } else 0

        val bars = dailyMinutes.reversed()

        return StatsUiState(
            totalMinutesThisWeek = totalWeek,
            sessionsThisWeek = sessions.size,
            streakDays = streak,
            avgFocusMinutes = avg,
            weekBars = bars
        )
    }

    companion object {
        fun factory(repo: SessionRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatsViewModel(repo) as T
                }
            }
        }
    }
}