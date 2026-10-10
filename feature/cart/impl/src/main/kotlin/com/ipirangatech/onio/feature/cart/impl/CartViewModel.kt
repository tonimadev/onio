package com.ipirangatech.onio.feature.cart.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.sum
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
class CartViewModel @Inject constructor(
    private val cart: CartRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CartState())
    val uiState: StateFlow<CartState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            cart.items
                .catch { _uiState.update { s -> s.copy(isLoading = false, hasError = true) } }
                .collect { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasError = false,
                            items = items.map { item -> item.toUiModel() }.toImmutableList(),
                            subtotal = items.map { item -> item.lineTotal }.sum(),
                        )
                    }
                }
        }
    }

    fun onIntent(intent: CartIntent) {
        when (intent) {
            is CartIntent.Increment -> change(intent.productId, +1)
            is CartIntent.Decrement -> change(intent.productId, -1)
            is CartIntent.Remove -> launchSafely { cart.remove(intent.productId) }
            is CartIntent.OpenProduct ->
                _uiState.update { it.copy(effect = CartEffect.NavigateToProduct(intent.productId)) }
            CartIntent.CheckoutClicked -> _uiState.update {
                if (it.items.isEmpty()) it else it.copy(effect = CartEffect.NavigateToCheckout)
            }
            CartIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun change(productId: Long, delta: Int) {
        val current = _uiState.value.items.firstOrNull { it.productId == productId } ?: return
        launchSafely { cart.setQuantity(productId, current.quantity + delta) }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { _uiState.update { s -> s.copy(hasError = true) } }
        }
    }
}
