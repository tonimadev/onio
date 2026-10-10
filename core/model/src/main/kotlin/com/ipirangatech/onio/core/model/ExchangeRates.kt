package com.ipirangatech.onio.core.model

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Static, offline exchange table (units of currency per 1 USD). Values are approximate on purpose:
 * Onio never moves real money, so no live rates are needed.
 */
object ExchangeRates {
    const val BASE = "USD"

    private val perUsd: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "CNY" to 7.2,
        "INR" to 83.0,
        "EUR" to 0.92,
        "SAR" to 3.75,
        "BDT" to 110.0,
        "BRL" to 5.0,
        "RUB" to 90.0,
        "PKR" to 280.0,
    )

    /** Fraction digits of each supported currency (ISO 4217); all supported ones use 2. */
    private fun fractionDigits(code: String): Int = 2

    val supportedCurrencies: List<String> = perUsd.keys.toList()

    fun isSupported(code: String) = code in perUsd

    /** Converts [money] to [target]; converting to the same currency is the identity. */
    fun convert(money: Money, target: String): Money {
        if (money.currencyCode == target) return money
        val from = perUsd[money.currencyCode] ?: error("Unsupported currency ${money.currencyCode}")
        val to = perUsd[target] ?: error("Unsupported currency $target")
        val usdMajor = money.minorUnits / 10.0.pow(fractionDigits(money.currencyCode)) / from
        val targetMinor = (usdMajor * to * 10.0.pow(fractionDigits(target))).roundToLong()
        return Money(targetMinor, target)
    }

    /** Default display currency for a BCP-47 language tag / region, falling back to USD. */
    fun defaultFor(language: String, region: String?): String = when {
        language == "pt" -> "BRL"
        language == "zh" -> "CNY"
        language == "hi" -> "INR"
        language == "bn" -> "BDT"
        language == "ru" -> "RUB"
        language == "ur" -> "PKR"
        language == "ar" -> "SAR"
        language == "es" || language == "fr" -> "EUR"
        else -> "USD"
    }
}
