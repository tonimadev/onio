package com.ipirangatech.onio.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    @Test fun luhnAcceptsTestCardsAndRejectsGarbage() {
        assertTrue(CardValidation.passesLuhn("4111 1111 1111 1111"))
        assertTrue(CardValidation.passesLuhn("5555555555554444"))
        assertFalse(CardValidation.passesLuhn("4111 1111 1111 1112"))
        assertFalse(CardValidation.passesLuhn("1234"))
    }

    @Test fun brandDetection() {
        assertEquals(CardBrand.VISA, CardValidation.brandOf("4111"))
        assertEquals(CardBrand.MASTERCARD, CardValidation.brandOf("5555"))
        assertEquals(CardBrand.AMEX, CardValidation.brandOf("3782"))
    }

    @Test fun expiry() {
        assertTrue(CardValidation.isValidExpiry(12, 30, 2026, 10))
        assertFalse(CardValidation.isValidExpiry(9, 26, 2026, 10))
        assertFalse(CardValidation.isValidExpiry(13, 30, 2026, 10))
    }

    @Test fun conversionRoundTripsAndIsIdentityForSameCurrency() {
        val usd = Money.usd(10_000)
        assertEquals(usd, ExchangeRates.convert(usd, "USD"))
        assertEquals(Money(50_000, "BRL"), ExchangeRates.convert(usd, "BRL"))
        assertEquals(usd, ExchangeRates.convert(ExchangeRates.convert(usd, "INR"), "USD"))
    }

    @Test fun orderExpiry() {
        val order = Order(1, "ON-1", emptyList(), Money.usd(100), Money.usd(100),
            PaymentCard(CardBrand.VISA, "1111"), "A", "B", 0, 1_000, OrderStatus.PROCESSING)
        assertFalse(order.isExpired(999))
        assertTrue(order.isExpired(1_000))
        assertEquals(0, order.remainingMillis(5_000))
    }
}
