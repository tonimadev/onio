package com.ipirangatech.onio.feature.savings.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Savings

@Immutable
data class UiSavings(val total: Money, val cancelledOrders: Int, val itemsNotBought: Int)

internal fun Savings.toUiModel() = UiSavings(total, cancelledOrders, itemsNotBought)

sealed interface SavingsEffect {
    data object NavigateToSettings : SavingsEffect
    data object NavigateToOrders : SavingsEffect
}

sealed interface SavingsIntent {
    data object SettingsClicked : SavingsIntent
    data object SeeOrdersClicked : SavingsIntent
    data object ConsumeEffect : SavingsIntent
}

@Immutable
data class SavingsState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val savings: UiSavings? = null,
    val userName: String? = null,
    val effect: SavingsEffect? = null,
)
