package com.ipirangatech.onio.core.model

enum class OrderStatus { PROCESSING, CANCELLED }

enum class CardBrand { VISA, MASTERCARD, AMEX, ELO, OTHER }

/** Only non-sensitive card data is ever kept. */
data class PaymentCard(val brand: CardBrand, val last4: String)

data class OrderItem(
    val productId: Long,
    val title: String,
    val thumbnailUrl: String,
    val unitPrice: Money,
    val quantity: Int,
)

data class Order(
    val id: Long,
    val number: String,
    val items: List<OrderItem>,
    /** Canonical total in USD, used for the savings tally. */
    val totalUsd: Money,
    /** Amount the user saw at checkout, in [chargedCurrency]. */
    val charged: Money,
    val card: PaymentCard,
    val shippingName: String,
    val shippingAddress: String,
    val placedAtMillis: Long,
    val cancelAtMillis: Long,
    val status: OrderStatus,
) {
    fun remainingMillis(nowMillis: Long): Long = (cancelAtMillis - nowMillis).coerceAtLeast(0)
    fun isExpired(nowMillis: Long): Boolean = nowMillis >= cancelAtMillis
}

data class Savings(val total: Money, val cancelledOrders: Int, val itemsNotBought: Int)
