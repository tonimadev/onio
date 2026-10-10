package com.ipirangatech.onio.core.model

/** A cart line stores a price snapshot so the cart works offline and stays stable. */
data class CartItem(
    val productId: Long,
    val title: String,
    val thumbnailUrl: String,
    val unitPrice: Money,
    val quantity: Int,
) {
    val lineTotal: Money get() = unitPrice * quantity
}
