package com.ipirangatech.onio.feature.checkout.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.Money

enum class CheckoutField { NAME, ADDRESS, CARD_NUMBER, EXPIRY, CVV }

sealed interface CheckoutEffect {
    data class OrderPlaced(val orderId: Long) : CheckoutEffect
    data object NavigateBack : CheckoutEffect
}

sealed interface CheckoutIntent {
    data class FieldChanged(val field: CheckoutField, val value: String) : CheckoutIntent
    data object Submit : CheckoutIntent
    data object BackClicked : CheckoutIntent
    data object ConsumeEffect : CheckoutIntent
}

@Immutable
data class CheckoutState(
    val total: Money = Money.zero(),
    val itemCount: Int = 0,
    val thumbnails: List<String> = emptyList(),
    val name: String = "",
    val address: String = "",
    val cardNumber: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val errors: Set<CheckoutField> = emptySet(),
    val isProcessing: Boolean = false,
    val hasError: Boolean = false,
    val effect: CheckoutEffect? = null,
)
