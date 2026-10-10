package com.ipirangatech.onio.core.model

import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/** Coupons discount either a percentage of the subtotal or a fixed amount in USD cents. */
enum class DiscountKind { PERCENT, AMOUNT }

/** A coupon "type" that can be redeemed by code or won on the wheel. */
data class CouponTemplate(
    val code: String,
    val kind: DiscountKind,
    val value: Int,
    val minSubtotalCents: Long = 0,
    val validDays: Int = 30,
)

/** A coupon sitting in the user's wallet. */
data class WalletCoupon(
    val id: Long,
    val code: String,
    val kind: DiscountKind,
    val value: Int,
    val minSubtotalCents: Long,
    val expiresAtMillis: Long,
    val usedAtMillis: Long?,
) {
    fun isUsable(nowMillis: Long) = usedAtMillis == null && nowMillis < expiresAtMillis

    fun isEligible(subtotal: Money, nowMillis: Long) = isUsable(nowMillis) && subtotal.minorUnits >= minSubtotalCents

    fun discountFor(subtotal: Money): Money = when (kind) {
        DiscountKind.PERCENT -> Money(subtotal.minorUnits * value / 100, subtotal.currencyCode)
        DiscountKind.AMOUNT -> Money(minOf(value.toLong(), subtotal.minorUnits), subtotal.currencyCode)
    }
}

/** Codes a user can type in; also the pool the wheel draws coupons from. */
object CouponCatalog {
    val Welcome = CouponTemplate("WELCOME10", DiscountKind.PERCENT, 10)
    val Percent5 = CouponTemplate("SPIN5", DiscountKind.PERCENT, 5)
    val Percent10 = CouponTemplate("SPIN10", DiscountKind.PERCENT, 10)
    val Amount5 = CouponTemplate("SPIN5OFF", DiscountKind.AMOUNT, 500, minSubtotalCents = 3_000)

    /** Public codes that work when typed. */
    val redeemable: List<CouponTemplate> = listOf(
        Welcome,
        CouponTemplate("ONIO15", DiscountKind.PERCENT, 15, minSubtotalCents = 5_000),
        CouponTemplate("FRETE10", DiscountKind.AMOUNT, 1_000, minSubtotalCents = 6_000),
        CouponTemplate("CALMA", DiscountKind.PERCENT, 8),
    )

    fun find(code: String): CouponTemplate? = redeemable.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
}

/** What the order costs after coupon and coins. See [PricingCalculator.COIN_VALUE_CENTS]. */
data class PriceBreakdown(
    val subtotal: Money,
    val couponDiscount: Money,
    val coinDiscount: Money,
    val total: Money,
    val coinsUsed: Int,
)

object PricingCalculator {
    /** Each coin is worth 10 US cents at checkout. */
    const val COIN_VALUE_CENTS = 10

    /** Coins can pay for at most half of what is left after the coupon. */
    private const val MAX_COIN_SHARE_PERCENT = 50

    fun maxCoins(subtotal: Money, coupon: WalletCoupon?, balance: Int): Int {
        val afterCoupon = subtotal.minorUnits - (coupon?.discountFor(subtotal)?.minorUnits ?: 0)
        val byValue = afterCoupon * MAX_COIN_SHARE_PERCENT / 100 / COIN_VALUE_CENTS
        return minOf(balance.toLong(), byValue).toInt().coerceAtLeast(0)
    }

    fun calculate(subtotal: Money, coupon: WalletCoupon?, coinsToUse: Int, balance: Int): PriceBreakdown {
        val couponDiscount = coupon?.discountFor(subtotal) ?: Money.zero(subtotal.currencyCode)
        val coins = coinsToUse.coerceIn(0, maxCoins(subtotal, coupon, balance))
        val coinDiscount = Money(coins.toLong() * COIN_VALUE_CENTS, subtotal.currencyCode)
        val total = Money((subtotal.minorUnits - couponDiscount.minorUnits - coinDiscount.minorUnits).coerceAtLeast(0), subtotal.currencyCode)
        return PriceBreakdown(subtotal, couponDiscount, coinDiscount, total, coins)
    }
}

/** What the wheel can land on, with its draw weight. Coin prizes dominate; coupons are rarer. */
sealed interface WheelPrize {
    data class Coins(val amount: Int) : WheelPrize
    data class Discount(val template: CouponTemplate) : WheelPrize
}

data class WheelSegment(val prize: WheelPrize, val weight: Int)

object Wheel {
    /** Segment order is the visual order around the wheel (clockwise from the top). */
    val segments: List<WheelSegment> = listOf(
        WheelSegment(WheelPrize.Coins(10), 24),
        WheelSegment(WheelPrize.Discount(CouponCatalog.Percent5), 8),
        WheelSegment(WheelPrize.Coins(25), 16),
        WheelSegment(WheelPrize.Coins(5), 28),
        WheelSegment(WheelPrize.Discount(CouponCatalog.Amount5), 5),
        WheelSegment(WheelPrize.Coins(50), 9),
        WheelSegment(WheelPrize.Discount(CouponCatalog.Percent10), 5),
        WheelSegment(WheelPrize.Coins(100), 3),
    )

    fun draw(random: Random): Int {
        val total = segments.sumOf { it.weight }
        var roll = random.nextInt(total)
        segments.forEachIndexed { index, segment ->
            if (roll < segment.weight) return index
            roll -= segment.weight
        }
        return segments.lastIndex
    }

    /**
     * Total rotation (degrees, clockwise) that leaves the centre of [segmentIndex] under the pointer at the
     * top, after at least [extraTurns] full turns starting from [currentRotation].
     */
    fun targetRotation(currentRotation: Float, segmentIndex: Int, extraTurns: Int = 5, jitter: Float = 0f): Float {
        val sweep = 360f / segments.size
        val centre = sweep * segmentIndex + sweep / 2f
        val base = ((360f - centre + jitter) % 360f + 360f) % 360f
        val current = ((currentRotation % 360f) + 360f) % 360f
        val delta = ((base - current) % 360f + 360f) % 360f
        return currentRotation + extraTurns * 360f + delta
    }
}

data class RewardsWallet(
    val coins: Int = 0,
    val spins: Int = 0,
    /** Consecutive check-in days, 0 when none yet; resets after a missed day. */
    val streak: Int = 0,
    val checkedInToday: Boolean = false,
)

data class CheckInResult(val day: Int, val coins: Int, val bonusSpin: Boolean)

/** Fixed reward for every order: it never grows with the cart value, on purpose. */
data class PurchaseReward(val coins: Int, val spins: Int)

data class SpinResult(val segmentIndex: Int, val prize: WheelPrize, val coupon: WalletCoupon?)

sealed interface RedeemResult {
    data class Added(val coupon: WalletCoupon) : RedeemResult
    data object Unknown : RedeemResult
    data object AlreadyHave : RedeemResult
}

interface CouponRepository {
    val coupons: Flow<List<WalletCoupon>>
    suspend fun grant(template: CouponTemplate): WalletCoupon
    suspend fun redeem(code: String): RedeemResult
    suspend fun markUsed(id: Long)
}

interface RewardsRepository {
    val wallet: Flow<RewardsWallet>

    /** Grants today's free spin (once per day, banked up to [MAX_SPINS]). Safe to call repeatedly. */
    suspend fun refreshDaily()

    /** Null when the user already checked in today. */
    suspend fun checkIn(): CheckInResult?

    suspend fun awardPurchase(): PurchaseReward

    /** Null when there are no spins left. Coins are credited / the coupon granted immediately. */
    suspend fun spin(): SpinResult?

    suspend fun spendCoins(amount: Int): Boolean

    companion object {
        const val MAX_SPINS = 5
        const val PURCHASE_COINS = 25
        const val PURCHASE_SPINS = 1

        /** Coins for check-in days 1..7; day 7 also grants a spin. */
        val CHECK_IN_COINS = listOf(5, 5, 10, 10, 15, 15, 50)
    }
}
