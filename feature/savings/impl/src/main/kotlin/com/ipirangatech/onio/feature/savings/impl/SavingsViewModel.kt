package com.ipirangatech.onio.feature.savings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.OrderRepository
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
class SavingsViewModel @Inject constructor(
    private val orders: OrderRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavingsState())
    val uiState: StateFlow<SavingsState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch { runCatching { orders.cancelExpiredOrders() } }
        viewModelScope.launch {
            runCatching {
                settings.settings.collect { value -> _uiState.update { it.copy(userName = value.userName) } }
            }
        }
        viewModelScope.launch {
            orders.savings
                .catch { _uiState.update { s -> s.copy(isLoading = false, hasError = true) } }
                .collect { savings ->
                    _uiState.update { it.copy(isLoading = false, hasError = false, savings = savings.toUiModel()) }
                }
        }
    }

    fun onIntent(intent: SavingsIntent) {
        when (intent) {
            SavingsIntent.SettingsClicked -> _uiState.update { it.copy(effect = SavingsEffect.NavigateToSettings) }
            SavingsIntent.SeeOrdersClicked -> _uiState.update { it.copy(effect = SavingsEffect.NavigateToOrders) }
            SavingsIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }
}
