package com.ipirangatech.onio.core.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formats any [Money] into [currencyCode] using [locale] (native digits, symbol placement). */
class MoneyFormatter(locale: Locale, val currencyCode: String) {
    private val currency = Currency.getInstance(currencyCode)
    private val format: NumberFormat = NumberFormat.getCurrencyInstance(locale).also { it.currency = currency }
    private val divisor: Double = Math.pow(10.0, currency.defaultFractionDigits.coerceAtLeast(0).toDouble())

    fun format(money: Money): String {
        val converted = ExchangeRates.convert(money, currencyCode)
        return format.format(converted.minorUnits / divisor)
    }

    companion object {
        /** Display currency: the user's override if supported, otherwise derived from the locale. */
        fun currencyFor(locale: Locale, override: String?): String =
            override?.takeIf(ExchangeRates::isSupported) ?: ExchangeRates.defaultFor(locale.language, locale.country)

        fun forLocale(locale: Locale, currencyOverride: String?) =
            MoneyFormatter(locale, currencyFor(locale, currencyOverride))
    }
}
