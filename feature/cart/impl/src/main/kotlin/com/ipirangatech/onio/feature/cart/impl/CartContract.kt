package com.ipirangatech.onio.feature.cart.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.Money
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class UiCartItem(
    val productId: Long,
    val title: String,
    val thumbnailUrl: String,
    val unitPrice: Money,
    val quantity: Int,
    val lineTotal: Money,
)

internal fun CartItem.toUiModel() = UiCartItem(productId, title, thumbnailUrl, unitPrice, quantity, lineTotal)

sealed interface CartEffect {
    data object NavigateToCheckout : CartEffect
    data class NavigateToProduct(val productId: Long) : CartEffect
}

sealed interface CartIntent {
    data class Increment(val productId: Long) : CartIntent
    data class Decrement(val productId: Long) : CartIntent
    data class Remove(val productId: Long) : CartIntent
    data class OpenProduct(val productId: Long) : CartIntent
    data object CheckoutClicked : CartIntent
    data object ConsumeEffect : CartIntent
}

@Immutable
data class CartState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val items: ImmutableList<UiCartItem> = persistentListOf(),
    val subtotal: Money = Money.zero(),
    val effect: CartEffect? = null,
)
