package com.zybooks.focusflow.ui.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zybooks.focusflow.data.FocusSession
import com.zybooks.focusflow.data.SessionRepository
import com.zybooks.focusflow.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first

enum class TimerPhaseType { Focus, Break }

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
    val menuExpanded: Boolean = false,
    // simple focus / break durations (no long break)
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5
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
                            // While running, only update config; don't reset timer
                            current.copy(
                                autoStartNext = settings.autoStartNext,
                                focusMinutes = settings.focusMinutes,
                                breakMinutes = settings.shortBreakMinutes
                            )
                        } else {
                            // Not running: reset timer based on focusMinutes
                            val total = settings.focusMinutes * 60_000L
                            current.copy(
                                phaseType = TimerPhaseType.Focus,
                                autoStartNext = settings.autoStartNext,
                                focusMinutes = settings.focusMinutes,
                                breakMinutes = settings.shortBreakMinutes,
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
        if (_uiState.value.isRunning) {
            return
        }
        _uiState.update {
            it.copy(
                isRunning = true,
                snackbarMessage = null
            )
        }
        startTicker()
    }

    fun pause() {
        if (!_uiState.value.isRunning) {
            return
        }
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

    fun applyCustomFromSettings() {
        stopTicker()
        val repo = settingsRepository ?: return
        viewModelScope.launch {
            val settings = repo.settingsFlow.first()
            val total = settings.focusMinutes * 60_000L

            _uiState.update { current ->
                current.copy(
                    phaseType = TimerPhaseType.Focus,
                    focusMinutes = settings.focusMinutes,
                    breakMinutes = settings.shortBreakMinutes,
                    totalMillis = total,
                    remainingMillis = total,
                    isRunning = false,
                    snackbarMessage = "Using custom settings"
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
                focusMinutes = focusMin,
                breakMinutes = breakMin,
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
        if (ticker?.isActive == true) {
            return
        }
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
            if (!current.isRunning) {
                return@update current
            }

            val next = (current.remainingMillis - deltaMs).coerceAtLeast(0L)
            if (next > 0L) {
                current.copy(remainingMillis = next)
            } else {
                // interval finished
                val addedMin = (current.totalMillis / 60_000L).toInt()

                // log completed interval
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

                when (current.phaseType) {
                    TimerPhaseType.Focus -> {
                        // Next: break
                        val breakMillis = current.breakMinutes * 60_000L
                        current.copy(
                            phaseType = TimerPhaseType.Break,
                            totalMillis = breakMillis,
                            remainingMillis = breakMillis,
                            isRunning = current.autoStartNext,
                            todaySessionCount = current.todaySessionCount + 1,
                            todayMinutes = current.todayMinutes + addedMin,
                            snackbarMessage = "Session complete",
                            navigateToBreakCoach = false
                        )
                    }

                    TimerPhaseType.Break -> {
                        // Next: focus
                        val focusMillis = current.focusMinutes * 60_000L
                        current.copy(
                            phaseType = TimerPhaseType.Focus,
                            totalMillis = focusMillis,
                            remainingMillis = focusMillis,
                            isRunning = current.autoStartNext,
                            snackbarMessage = "Break over",
                            navigateToBreakCoach = false
                        )
                    }
                }
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

