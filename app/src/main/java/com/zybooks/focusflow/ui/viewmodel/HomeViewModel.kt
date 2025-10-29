package com.zybooks.focusflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import android.os.SystemClock

enum class TimerPhaseType { Focus, ShortBreak, LongBreak }

data class HomeUiState(
    val phaseType: TimerPhaseType = TimerPhaseType.Focus,
    val remainingMillis: Long = 25L * 60_000L,
    val totalMillis: Long = 25L * 60_000L,
    val isRunning: Boolean = false,
    val autoStartNext: Boolean = false,
    val todaySessionCount: Int = 0,
    val todayMinutes: Int = 0,
    val snackbarMessage: String? = null,
    val navigateToBreakCoach: Boolean = false,
    val menuExpanded: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var ticker: Job? = null
    private var lastTickRef: Long = 0L

    fun toggleMenu(force: Boolean? = null) {
        _uiState.update { it.copy(menuExpanded = force ?: !it.menuExpanded) }
    }

    fun start() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true, snackbarMessage = null) }
        startTicker()
    }

    fun pause() {
        if (!_uiState.value.isRunning) return
        stopTicker()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun stop() {
        stopTicker()
        _uiState.update {
            it.copy(
                isRunning = false,
                remainingMillis = it.totalMillis,
                snackbarMessage = "Session stopped"
            )
        }
    }

    fun setAutoStart(enabled: Boolean) {
        _uiState.update { it.copy(autoStartNext = enabled) }
    }

    fun selectPreset(focusMin: Int, breakMin: Int) {
        val total = focusMin * 60_000L
        stopTicker()
        _uiState.update {
            it.copy(
                phaseType = TimerPhaseType.Focus,
                totalMillis = total,
                remainingMillis = total,
                isRunning = false,
                snackbarMessage = "Preset set to $focusMin/$breakMin"
            )
        }
    }

    fun openCustomPicker() {
        _uiState.update { it.copy(snackbarMessage = "Custom picker coming soon") }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        lastTickRef = SystemClock.elapsedRealtime()
        ticker = viewModelScope.launch {
            while (isActive && _uiState.value.isRunning) {
                delay(100L)
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastTickRef
                lastTickRef = now
                onTick(delta)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun onTick(deltaMs: Long) {
        _uiState.update { current ->
            val next = (current.remainingMillis - deltaMs).coerceAtLeast(0L)
            if (next == 0L) {
                val addedMin = (current.totalMillis / 60_000L).toInt()
                current.copy(
                    remainingMillis = current.totalMillis,
                    isRunning = current.autoStartNext,
                    todaySessionCount = current.todaySessionCount + 1,
                    todayMinutes = current.todayMinutes + addedMin,
                    snackbarMessage = "Session complete",
                    navigateToBreakCoach = false
                )
            } else {
                current.copy(remainingMillis = next)
            }
        }
    }
}