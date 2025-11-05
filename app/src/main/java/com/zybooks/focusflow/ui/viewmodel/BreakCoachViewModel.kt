package com.zybooks.focusflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class BreathingPattern { FourFourFour, FourSevenEight, Custom }

data class BreakCoachUiState(
    val selectedPattern: BreathingPattern = BreathingPattern.FourFourFour,
    val isBreathingRunning: Boolean = false,
    val currentStepLabel: String = "Inhale",
    val movementMessage: String = "Try a short walk",
    val stepsText: String = "Steps in last 10 min: 120"
)

class BreakCoachViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BreakCoachUiState())
    val uiState: StateFlow<BreakCoachUiState> = _uiState

    fun selectPattern(pattern: BreathingPattern) {
        _uiState.update { it.copy(selectedPattern = pattern) }
    }

    fun startBreathing() {
        _uiState.update { it.copy(isBreathingRunning = true, currentStepLabel = "Inhale") }
    }

    fun stopBreathing() {
        _uiState.update { it.copy(isBreathingRunning = false, currentStepLabel = "Inhale") }
    }

    fun setBreathLabel(label: String) {
        _uiState.update { it.copy(currentStepLabel = label) }
    }
}