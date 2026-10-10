package com.ipirangatech.onio.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RewardsTest {
    private fun coupon(kind: DiscountKind, value: Int, min: Long = 0, used: Long? = null, expires: Long = 1_000) =
        WalletCoupon(1, "X", kind, value, min, expires, used)

    @Test fun percentAndAmountDiscounts() {
        val subtotal = Money.usd(20_000)
        assertEquals(2_000L, coupon(DiscountKind.PERCENT, 10).discountFor(subtotal).minorUnits)
        assertEquals(500L, coupon(DiscountKind.AMOUNT, 500).discountFor(subtotal).minorUnits)
        // A fixed coupon can never exceed the subtotal.
        assertEquals(300L, coupon(DiscountKind.AMOUNT, 500).discountFor(Money.usd(300)).minorUnits)
    }

    @Test fun eligibilityChecksMinimumExpiryAndUse() {
        val c = coupon(DiscountKind.PERCENT, 10, min = 5_000)
        assertFalse(c.isEligible(Money.usd(4_999), nowMillis = 10))
        assertTrue(c.isEligible(Money.usd(5_000), nowMillis = 10))
        assertFalse(c.isEligible(Money.usd(5_000), nowMillis = 1_000)) // expired
        assertFalse(coupon(DiscountKind.PERCENT, 10, used = 5).isUsable(10))
    }

    @Test fun coinsPayAtMostHalfOfWhatIsLeftAfterTheCoupon() {
        val subtotal = Money.usd(10_000)
        // Half of $100.00 is $50.00 = 500 coins at 10 cents each.
        assertEquals(500, PricingCalculator.maxCoins(subtotal, null, balance = 99_999))
        assertEquals(250, PricingCalculator.maxCoins(subtotal, coupon(DiscountKind.PERCENT, 50), balance = 99_999))
        assertEquals(120, PricingCalculator.maxCoins(subtotal, null, balance = 120))
    }

    @Test fun breakdownAppliesCouponThenCoinsAndNeverGoesNegative() {
        val b = PricingCalculator.calculate(Money.usd(10_000), coupon(DiscountKind.PERCENT, 10), coinsToUse = 400, balance = 500)
        assertEquals(1_000L, b.couponDiscount.minorUnits)
        assertEquals(4_000L, b.coinDiscount.minorUnits) // 400 coins x 10 cents
        assertEquals(5_000L, b.total.minorUnits)
        // Asking for more coins than allowed is clamped, not an error.
        val clamped = PricingCalculator.calculate(Money.usd(1_000), null, coinsToUse = 10_000, balance = 10_000)
        assertEquals(50, clamped.coinsUsed) // $5.00 is half of $10.00
        assertEquals(500L, clamped.total.minorUnits)
    }

    @Test fun redeemableCodesAreCaseInsensitiveAndUnknownOnesAreNull() {
        assertEquals(CouponCatalog.Welcome, CouponCatalog.find(" welcome10 "))
        assertEquals(null, CouponCatalog.find("NOPE"))
    }

    @Test fun wheelDrawRespectsTheWeights() {
        val random = Random(42)
        val counts = IntArray(Wheel.segments.size)
        repeat(20_000) { counts[Wheel.draw(random)]++ }
        val total = Wheel.segments.sumOf { it.weight }
        Wheel.segments.forEachIndexed { i, s ->
            val expected = 20_000.0 * s.weight / total
            assertTrue("segment $i: ${counts[i]} vs $expected", counts[i] in (expected * 0.85).toInt()..(expected * 1.15).toInt())
        }
        // Coupons are rarer than coins.
        val couponWeight = Wheel.segments.filter { it.prize is WheelPrize.Discount }.sumOf { it.weight }
        assertTrue(couponWeight < total / 3)
    }

    @Test fun targetRotationLandsTheChosenSegmentUnderThePointer() {
        val sweep = 360f / Wheel.segments.size
        for (index in Wheel.segments.indices) {
            for (start in listOf(0f, 133f, 1_000.5f)) {
                val end = Wheel.targetRotation(start, index)
                assertTrue("at least 5 full turns", end - start >= 5 * 360f)
                // After rotating clockwise by `end`, the segment centre sits at (centre + end) mod 360 == 0 (top).
                val centre = sweep * index + sweep / 2f
                val landed = ((centre + end) % 360f + 360f) % 360f
                assertTrue("segment $index landed at $landed", landed < 0.01f || landed > 359.99f)
            }
        }
    }
}
