package com.ipirangatech.onio.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {
    @Test fun usesTheLocalesCurrencyAndSymbol() {
        val usd = MoneyFormatter.forLocale(Locale.US, null).format(Money.usd(999))
        assertEquals("$9.99", usd)
        val brl = MoneyFormatter.forLocale(Locale.forLanguageTag("pt-BR"), null).format(Money.usd(999))
        assertTrue(brl, brl.startsWith("R$") && brl.endsWith("49,95"))
    }

    @Test fun overrideBeatsTheLocaleAndUnsupportedOverridesAreIgnored() {
        assertEquals("EUR", MoneyFormatter.currencyFor(Locale.US, "EUR"))
        assertEquals("USD", MoneyFormatter.currencyFor(Locale.US, "XXX"))
        assertEquals("BRL", MoneyFormatter.currencyFor(Locale.forLanguageTag("pt-BR"), null))
    }

    @Test fun everySupportedCurrencyHasADefaultLanguage() {
        listOf("en", "zh", "hi", "es", "fr", "ar", "bn", "pt", "ru", "ur").forEach { language ->
            val code = ExchangeRates.defaultFor(language, null)
            assertTrue("$language -> $code", ExchangeRates.isSupported(code))
            // Must not throw for any supported currency.
            MoneyFormatter(Locale.forLanguageTag(language), code).format(Money.usd(12_345))
        }
    }
}
