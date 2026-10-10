package com.ipirangatech.onio.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            repository.settings
                .catch { _uiState.update { s -> s.copy(isLoading = false, hasError = true) } }
                .collect { settings -> _uiState.update { it.copy(isLoading = false, settings = settings) } }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.CancelMinutesSelected -> save { repository.setCancelAfterMinutes(intent.minutes) }
            is SettingsIntent.CurrencySelected -> save { repository.setCurrencyOverride(intent.code) }
            is SettingsIntent.LanguageSelected ->
                _uiState.update { it.copy(effect = SettingsEffect.ApplyLanguage(intent.tag)) }
            SettingsIntent.BackClicked -> _uiState.update { it.copy(effect = SettingsEffect.NavigateBack) }
            SettingsIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { _uiState.update { s -> s.copy(hasError = true) } }
        }
    }
}
