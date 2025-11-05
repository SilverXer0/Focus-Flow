package com.zybooks.focusflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import android.os.SystemClock
import com.zybooks.focusflow.data.SettingsRepository
import androidx.lifecycle.ViewModelProvider
import com.zybooks.focusflow.data.FocusSession
import com.zybooks.focusflow.data.SessionRepository

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

class HomeViewModel(
    private val settingsRepository: SettingsRepository? = null,
    private val sessionRepository: SessionRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var ticker: Job? = null
    private var lastTickRef: Long = 0L

    init {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.settingsFlow.collect { settings ->
                    _uiState.update { current ->
                        if (current.isRunning) {
                            current.copy(
                                autoStartNext = settings.autoStartNext
                            )
                        } else {
                            val total = settings.focusMinutes * 60_000L
                            current.copy(
                                autoStartNext = settings.autoStartNext,
                                totalMillis = total,
                                remainingMillis = total
                            )
                        }
                    }
                }
            }
        }
    }

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
        val currentState = _uiState.value
        stopTicker()
        _uiState.update {
            it.copy(
                isRunning = false,
                remainingMillis = it.totalMillis,
                snackbarMessage = "Session stopped"
            )
        }
        val ranMillis = currentState.totalMillis - currentState.remainingMillis
        if (ranMillis > 5_000) {
            val mins = (ranMillis / 60_000L).toInt().coerceAtLeast(1)
            viewModelScope.launch {
                sessionRepository?.addSession(
                    FocusSession(
                        id = System.currentTimeMillis(),
                        startTimeMillis = System.currentTimeMillis() - ranMillis,
                        durationMinutes = mins,
                        type = currentState.phaseType.name,
                        completed = false
                    )
                )
            }
        }
    }

    fun setAutoStart(enabled: Boolean) {
        _uiState.update { it.copy(autoStartNext = enabled) }
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.updateAutoStart(enabled)
            }
        }
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
                viewModelScope.launch {
                    sessionRepository?.addSession(
                        FocusSession(
                            id = System.currentTimeMillis(),
                            startTimeMillis = System.currentTimeMillis() - current.totalMillis,
                            durationMinutes = addedMin,
                            type = current.phaseType.name,
                            completed = true
                        )
                    )
                }
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
    companion object {
        fun factory(
            settingsRepo: SettingsRepository,
            sessionRepo: SessionRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(settingsRepo, sessionRepo) as T
                }
            }
        }
    }
}