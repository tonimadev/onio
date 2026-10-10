package com.ipirangatech.onio.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.ipirangatech.onio.core.model.ExchangeRates
import com.ipirangatech.onio.core.model.Money
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formats any [Money] into [currencyCode] using the user's locale (native digits, symbol placement). */
@Immutable
class MoneyFormatter(locale: Locale, val currencyCode: String) {
    private val currency = Currency.getInstance(currencyCode)
    private val format: NumberFormat = NumberFormat.getCurrencyInstance(locale).also { it.currency = currency }
    private val divisor: Double = Math.pow(10.0, currency.defaultFractionDigits.coerceAtLeast(0).toDouble())

    fun format(money: Money): String {
        val converted = ExchangeRates.convert(money, currencyCode)
        return format.format(converted.minorUnits / divisor)
    }
}

@Composable
fun rememberMoneyFormatter(currencyOverride: String?): MoneyFormatter {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val code = currencyOverride?.takeIf(ExchangeRates::isSupported)
        ?: ExchangeRates.defaultFor(locale.language, locale.country)
    return remember(locale, code) { MoneyFormatter(locale, code) }
}
