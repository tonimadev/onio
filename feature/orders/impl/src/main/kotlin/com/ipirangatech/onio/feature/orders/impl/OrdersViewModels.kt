package com.ipirangatech.onio.feature.orders.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
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
class OrdersViewModel @Inject constructor(
    private val orders: OrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrdersState())
    val uiState: StateFlow<OrdersState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            orders.cancelExpiredOrders()
        }
        viewModelScope.launch {
            orders.orders
                .catch { _uiState.update { s -> s.copy(isLoading = false, hasError = true) } }
                .collect { list ->
                    _uiState.update {
                        it.copy(isLoading = false, hasError = false, orders = list.map { o -> o.toUiModel() }.toImmutableList())
                    }
                }
        }
    }

    fun onIntent(intent: OrdersIntent) {
        when (intent) {
            is OrdersIntent.OrderClicked -> _uiState.update { it.copy(effect = OrdersEffect.NavigateToOrder(intent.orderId)) }
            OrdersIntent.DeadlineReached -> viewModelScope.launch {
                runCatching { orders.cancelExpiredOrders() }.onFailure { _uiState.update { s -> s.copy(hasError = true) } }
            }
            OrdersIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }
}

@HiltViewModel(assistedFactory = OrderDetailViewModel.Factory::class)
class OrderDetailViewModel @AssistedInject constructor(
    @Assisted private val orderId: Long,
    private val orders: OrderRepository,
    settings: SettingsRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(orderId: Long): OrderDetailViewModel
    }

    private val _uiState = MutableStateFlow(OrderDetailState())
    val uiState: StateFlow<OrderDetailState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch { runCatching { orders.cancelExpiredOrders() } }
        viewModelScope.launch {
            runCatching {
                settings.settings.collect { value -> _uiState.update { it.copy(userName = value.userName) } }
            }
        }
        viewModelScope.launch {
            orders.order(orderId)
                .catch { _uiState.update { s -> s.copy(isLoading = false, hasError = true) } }
                .collect { order ->
                    _uiState.update { it.copy(isLoading = false, hasError = order == null, order = order?.toUiModel()) }
                }
        }
    }

    fun onIntent(intent: OrderDetailIntent) {
        when (intent) {
            OrderDetailIntent.DeadlineReached -> viewModelScope.launch {
                runCatching { orders.cancelExpiredOrders() }.onFailure { _uiState.update { s -> s.copy(hasError = true) } }
            }
            OrderDetailIntent.BackClicked -> _uiState.update { it.copy(effect = OrderDetailEffect.NavigateBack) }
            OrderDetailIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }
}
