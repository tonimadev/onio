package com.ipirangatech.onio.feature.rewards.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CouponRepository
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.RedeemResult
import com.ipirangatech.onio.core.model.RewardsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewards: RewardsRepository,
    private val coupons: CouponRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RewardsState())
    val uiState: StateFlow<RewardsState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch { runCatching { rewards.refreshDaily() } }
        viewModelScope.launch {
            rewards.wallet
                .catch { _uiState.update { s -> s.copy(hasError = true) } }
                .collect { wallet -> _uiState.update { it.copy(wallet = wallet) } }
        }
        viewModelScope.launch {
            coupons.coupons
                .catch { _uiState.update { s -> s.copy(hasError = true) } }
                .collect { list -> _uiState.update { it.copy(coupons = list.toImmutableList()) } }
        }
    }

    fun onIntent(intent: RewardsIntent) {
        when (intent) {
            RewardsIntent.CheckIn -> viewModelScope.launch {
                runCatching { rewards.checkIn() }
                    .onSuccess { result -> if (result != null) _uiState.update { it.copy(lastCheckIn = result) } }
                    .onFailure { _uiState.update { s -> s.copy(hasError = true) } }
            }
            RewardsIntent.DismissCheckIn -> _uiState.update { it.copy(lastCheckIn = null) }
            RewardsIntent.WheelClicked -> _uiState.update { it.copy(effect = RewardsEffect.NavigateToWheel) }
            is RewardsIntent.CodeChanged -> _uiState.update { it.copy(code = intent.code.take(MAX_CODE), codeMessage = null) }
            RewardsIntent.RedeemCode -> redeem()
            RewardsIntent.BackClicked -> _uiState.update { it.copy(effect = RewardsEffect.NavigateBack) }
            RewardsIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun redeem() {
        val code = _uiState.value.code
        if (code.isBlank()) return
        viewModelScope.launch {
            runCatching { coupons.redeem(code) }
                .onSuccess { result ->
                    val message = when (result) {
                        is RedeemResult.Added -> CodeMessage.ADDED
                        RedeemResult.Unknown -> CodeMessage.UNKNOWN
                        RedeemResult.AlreadyHave -> CodeMessage.ALREADY_HAVE
                    }
                    _uiState.update { it.copy(codeMessage = message, code = if (result is RedeemResult.Added) "" else it.code) }
                }
                .onFailure { _uiState.update { s -> s.copy(hasError = true) } }
        }
    }

    private companion object {
        const val MAX_CODE = 24
    }
}

@HiltViewModel
class WheelViewModel @Inject constructor(
    private val rewards: RewardsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WheelState())
    val uiState: StateFlow<WheelState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch { runCatching { rewards.refreshDaily() } }
        viewModelScope.launch {
            rewards.wallet
                .catch { _uiState.update { s -> s.copy(hasError = true) } }
                .collect { wallet -> _uiState.update { it.copy(wallet = wallet) } }
        }
    }

    fun onIntent(intent: WheelIntent) {
        when (intent) {
            WheelIntent.Spin -> spin()
            WheelIntent.SpinFinished -> _uiState.update {
                if (it.pending == null) it else it.copy(isSpinning = false, result = it.pending, pending = null)
            }
            WheelIntent.DismissResult -> _uiState.update { it.copy(result = null) }
            WheelIntent.BackClicked -> _uiState.update { it.copy(effect = WheelEffect.NavigateBack) }
            WheelIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun spin() {
        val s = _uiState.value
        if (s.isSpinning || s.result != null) return
        // Flag first so a double tap can't start two spins while the repository call is in flight.
        _uiState.update { it.copy(isSpinning = true, hasError = false) }
        viewModelScope.launch {
            runCatching { rewards.spin() }
                .onSuccess { result ->
                    _uiState.update { if (result == null) it.copy(isSpinning = false) else it.copy(pending = result) }
                }
                .onFailure { _uiState.update { it.copy(isSpinning = false, hasError = true) } }
        }
    }
}

@HiltViewModel(assistedFactory = OrderPlacedViewModel.Factory::class)
class OrderPlacedViewModel @AssistedInject constructor(
    @Assisted private val orderId: Long,
    @Assisted("coins") coins: Int,
    @Assisted("spins") spins: Int,
    private val orders: OrderRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(orderId: Long, @Assisted("coins") coins: Int, @Assisted("spins") spins: Int): OrderPlacedViewModel
    }

    private val _uiState = MutableStateFlow(OrderPlacedState(coins = coins, spinsEarned = spins))
    val uiState: StateFlow<OrderPlacedState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            runCatching { orders.order(orderId).first() }
                .onSuccess { order ->
                    _uiState.update { it.copy(orderNumber = order?.number, total = order?.totalUsd, hasError = order == null) }
                }
                .onFailure { _uiState.update { it.copy(hasError = true) } }
        }
    }

    fun onIntent(intent: OrderPlacedIntent) {
        when (intent) {
            OrderPlacedIntent.SpinNow -> emit(OrderPlacedEffect.NavigateToWheel)
            OrderPlacedIntent.ViewOrder -> emit(OrderPlacedEffect.NavigateToOrder(orderId))
            OrderPlacedIntent.Continue -> emit(OrderPlacedEffect.ContinueShopping)
            OrderPlacedIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun emit(effect: OrderPlacedEffect) = _uiState.update { it.copy(effect = effect) }
}
