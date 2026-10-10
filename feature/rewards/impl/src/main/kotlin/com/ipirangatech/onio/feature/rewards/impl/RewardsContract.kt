package com.ipirangatech.onio.feature.rewards.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.CheckInResult
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.RewardsWallet
import com.ipirangatech.onio.core.model.SpinResult
import com.ipirangatech.onio.core.model.WalletCoupon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// ---------------- hub ----------------
sealed interface RewardsEffect {
    data object NavigateToWheel : RewardsEffect
    data object NavigateBack : RewardsEffect
}

sealed interface RewardsIntent {
    data object CheckIn : RewardsIntent
    data object DismissCheckIn : RewardsIntent
    data object WheelClicked : RewardsIntent
    data class CodeChanged(val code: String) : RewardsIntent
    data object RedeemCode : RewardsIntent
    data object BackClicked : RewardsIntent
    data object ConsumeEffect : RewardsIntent
}

enum class CodeMessage { ADDED, UNKNOWN, ALREADY_HAVE }

@Immutable
data class RewardsState(
    val wallet: RewardsWallet = RewardsWallet(),
    val coupons: ImmutableList<WalletCoupon> = persistentListOf(),
    val code: String = "",
    val codeMessage: CodeMessage? = null,
    /** Set right after a successful check-in so the UI can celebrate; cleared by DismissCheckIn. */
    val lastCheckIn: CheckInResult? = null,
    val hasError: Boolean = false,
    val effect: RewardsEffect? = null,
) {
    /** Number of check-in tiles already collected in the current 7-day cycle. */
    val daysCollected: Int
        get() = if (wallet.checkedInToday) wallet.streak else if (wallet.streak >= 7) 0 else wallet.streak
}

// ---------------- wheel ----------------
sealed interface WheelEffect {
    data object NavigateBack : WheelEffect
}

sealed interface WheelIntent {
    data object Spin : WheelIntent
    /** The UI reports the rotation animation reached its end. */
    data object SpinFinished : WheelIntent
    data object DismissResult : WheelIntent
    data object BackClicked : WheelIntent
    data object ConsumeEffect : WheelIntent
}

@Immutable
data class WheelState(
    val wallet: RewardsWallet = RewardsWallet(),
    val isSpinning: Boolean = false,
    /** Already decided and credited; the UI animates towards it and then sends SpinFinished. */
    val pending: SpinResult? = null,
    val result: SpinResult? = null,
    val hasError: Boolean = false,
    val effect: WheelEffect? = null,
)

// ---------------- celebration ----------------
sealed interface OrderPlacedEffect {
    data object NavigateToWheel : OrderPlacedEffect
    data class NavigateToOrder(val orderId: Long) : OrderPlacedEffect
    data object ContinueShopping : OrderPlacedEffect
}

sealed interface OrderPlacedIntent {
    data object SpinNow : OrderPlacedIntent
    data object ViewOrder : OrderPlacedIntent
    data object Continue : OrderPlacedIntent
    data object ConsumeEffect : OrderPlacedIntent
}

@Immutable
data class OrderPlacedState(
    val orderNumber: String? = null,
    val total: Money? = null,
    val coins: Int = 0,
    val spinsEarned: Int = 0,
    val hasError: Boolean = false,
    val effect: OrderPlacedEffect? = null,
)
