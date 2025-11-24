package com.zybooks.focusflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zybooks.focusflow.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val autoStartNext: Boolean = false,
    val vibrations: Boolean = true,
    val movementNudge: Boolean = true,
    val isLoading: Boolean = true,
    val exportResult: String? = null
)

class SettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            repo.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        focusMinutes = settings.focusMinutes,
                        shortBreakMinutes = settings.shortBreakMinutes,
                        autoStartNext = settings.autoStartNext,
                        vibrations = settings.vibrations,
                        movementNudge = settings.movementNudge,
                        isLoading = false,
                        exportResult = null
                    )
                }
            }
        }
    }

    fun setFocusMinutes(min: Int) {
        _uiState.update { it.copy(focusMinutes = min) }
        viewModelScope.launch { repo.updateFocusMinutes(min) }
    }

    fun setShortBreakMinutes(min: Int) {
        _uiState.update { it.copy(shortBreakMinutes = min) }
        viewModelScope.launch { repo.updateShortBreakMinutes(min) }
    }


    fun setAutoStart(enabled: Boolean) {
        _uiState.update { it.copy(autoStartNext = enabled) }
        viewModelScope.launch { repo.updateAutoStart(enabled) }
    }

    fun setVibrations(enabled: Boolean) {
        _uiState.update { it.copy(vibrations = enabled) }
        viewModelScope.launch { repo.updateVibrations(enabled) }
    }

    fun setMovementNudge(enabled: Boolean) {
        _uiState.update { it.copy(movementNudge = enabled) }
        viewModelScope.launch { repo.updateMovementNudge(enabled) }
    }

    fun resetStats() {
        viewModelScope.launch {
            repo.resetStats()
        }
    }

    fun exportSummary() {
        viewModelScope.launch {
            val txt = repo.exportSummaryToClipboard()
            _uiState.update { it.copy(exportResult = txt) }
        }
    }

    companion object {
        fun factory(repo: SettingsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repo) as T
                }
            }
        }
    }
}