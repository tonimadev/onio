package com.ipirangatech.onio.feature.checkout.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.WalletCoupon

enum class CouponCodeMessage { ADDED, UNKNOWN, ALREADY_HAVE }

enum class CheckoutField { NAME, ADDRESS, CARD_NUMBER, EXPIRY, CVV }

sealed interface CheckoutEffect {
    data class OrderPlaced(val orderId: Long, val coinsEarned: Int, val spinsEarned: Int) : CheckoutEffect
    data object NavigateBack : CheckoutEffect
}

sealed interface CheckoutIntent {
    data class FieldChanged(val field: CheckoutField, val value: String) : CheckoutIntent
    data object Submit : CheckoutIntent
    data object OpenCoupons : CheckoutIntent
    data object CloseCoupons : CheckoutIntent
    /** Null means "no coupon". */
    data class SelectCoupon(val couponId: Long?) : CheckoutIntent
    data object ToggleCoins : CheckoutIntent
    data class CouponCodeChanged(val code: String) : CheckoutIntent
    data object RedeemCouponCode : CheckoutIntent
    data object BackClicked : CheckoutIntent
    data object ConsumeEffect : CheckoutIntent
}

@Immutable
data class CheckoutState(
    /** What the user pays: subtotal minus coupon and coins. */
    val total: Money = Money.zero(),
    val subtotal: Money = Money.zero(),
    val couponDiscount: Money = Money.zero(),
    val coinDiscount: Money = Money.zero(),
    val coinsUsed: Int = 0,
    val coinBalance: Int = 0,
    val maxCoins: Int = 0,
    val useCoins: Boolean = false,
    val coupons: List<WalletCoupon> = emptyList(),
    val selectedCouponId: Long? = null,
    val couponSheetOpen: Boolean = false,
    val couponCode: String = "",
    val couponCodeMessage: CouponCodeMessage? = null,
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
