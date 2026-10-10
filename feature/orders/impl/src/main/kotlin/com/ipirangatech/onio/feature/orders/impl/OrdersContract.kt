package com.ipirangatech.onio.feature.orders.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.CardBrand
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Order
import com.ipirangatech.onio.core.model.OrderItem
import com.ipirangatech.onio.core.model.OrderStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class UiOrderItem(val productId: Long, val title: String, val thumbnailUrl: String, val unitPrice: Money, val quantity: Int)

@Immutable
data class UiOrder(
    val id: Long,
    val number: String,
    val isCancelled: Boolean,
    val items: ImmutableList<UiOrderItem>,
    val itemCount: Int,
    val total: Money,
    val cardBrand: CardBrand,
    val cardLast4: String,
    val shippingName: String,
    val shippingAddress: String,
    val placedAtMillis: Long,
    val cancelAtMillis: Long,
)

internal fun OrderItem.toUiModel() = UiOrderItem(productId, title, thumbnailUrl, unitPrice, quantity)

internal fun Order.toUiModel() = UiOrder(
    id = id,
    number = number,
    isCancelled = status == OrderStatus.CANCELLED,
    items = items.map(OrderItem::toUiModel).toImmutableList(),
    itemCount = items.sumOf { it.quantity },
    total = totalUsd,
    cardBrand = card.brand,
    cardLast4 = card.last4,
    shippingName = shippingName,
    shippingAddress = shippingAddress,
    placedAtMillis = placedAtMillis,
    cancelAtMillis = cancelAtMillis,
)

// ---- list ----
sealed interface OrdersEffect {
    data class NavigateToOrder(val orderId: Long) : OrdersEffect
}

sealed interface OrdersIntent {
    data class OrderClicked(val orderId: Long) : OrdersIntent
    data object DeadlineReached : OrdersIntent
    data object ConsumeEffect : OrdersIntent
}

@Immutable
data class OrdersState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val orders: ImmutableList<UiOrder> = persistentListOf(),
    val effect: OrdersEffect? = null,
)

// ---- detail ----
sealed interface OrderDetailEffect {
    data object NavigateBack : OrderDetailEffect
}

sealed interface OrderDetailIntent {
    data object DeadlineReached : OrderDetailIntent
    data object BackClicked : OrderDetailIntent
    data object ConsumeEffect : OrderDetailIntent
}

@Immutable
data class OrderDetailState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val order: UiOrder? = null,
    val userName: String? = null,
    val effect: OrderDetailEffect? = null,
)
