package com.ipirangatech.onio.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ipirangatech.onio.core.model.CardBrand
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Order
import com.ipirangatech.onio.core.model.OrderItem
import com.ipirangatech.onio.core.model.OrderStatus
import com.ipirangatech.onio.core.model.PaymentCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "cart_items")
internal data class CartItemEntity(
    @PrimaryKey val productId: Long,
    val title: String,
    val thumbnailUrl: String,
    val unitPriceUsdCents: Long,
    val quantity: Int,
)

@Serializable
internal data class OrderItemJson(
    val productId: Long,
    val title: String,
    val thumbnailUrl: String,
    val unitPriceUsdCents: Long,
    val quantity: Int,
)

@Entity(tableName = "orders")
internal data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val itemsJson: String,
    val totalUsdCents: Long,
    val chargedMinorUnits: Long,
    val chargedCurrency: String,
    val cardBrand: String,
    val cardLast4: String,
    val shippingName: String,
    val shippingAddress: String,
    val placedAtMillis: Long,
    val cancelAtMillis: Long,
    val status: String,
)

private val json = Json { ignoreUnknownKeys = true }

internal fun CartItemEntity.toDomain() =
    CartItem(productId, title, thumbnailUrl, Money.usd(unitPriceUsdCents), quantity)

internal fun CartItem.toEntity() =
    CartItemEntity(productId, title, thumbnailUrl, unitPrice.minorUnits, quantity)

internal fun OrderItem.toJson() = OrderItemJson(productId, title, thumbnailUrl, unitPrice.minorUnits, quantity)

internal fun List<OrderItem>.encode(): String = json.encodeToString(map(OrderItem::toJson))

internal fun OrderEntity.toDomain() = Order(
    id = id,
    number = number,
    items = json.decodeFromString<List<OrderItemJson>>(itemsJson).map {
        OrderItem(it.productId, it.title, it.thumbnailUrl, Money.usd(it.unitPriceUsdCents), it.quantity)
    },
    totalUsd = Money.usd(totalUsdCents),
    charged = Money(chargedMinorUnits, chargedCurrency),
    card = PaymentCard(runCatching { CardBrand.valueOf(cardBrand) }.getOrDefault(CardBrand.OTHER), cardLast4),
    shippingName = shippingName,
    shippingAddress = shippingAddress,
    placedAtMillis = placedAtMillis,
    cancelAtMillis = cancelAtMillis,
    status = runCatching { OrderStatus.valueOf(status) }.getOrDefault(OrderStatus.CANCELLED),
)
