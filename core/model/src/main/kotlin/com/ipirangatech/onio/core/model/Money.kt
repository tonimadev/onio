package com.ipirangatech.onio.core.model

/** Monetary amount in the minor unit of [currencyCode] (e.g. cents for USD). */
data class Money(val minorUnits: Long, val currencyCode: String = ExchangeRates.BASE) {
    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Currency mismatch" }
        return Money(minorUnits + other.minorUnits, currencyCode)
    }

    operator fun times(quantity: Int): Money = Money(minorUnits * quantity, currencyCode)

    companion object {
        fun usd(minorUnits: Long) = Money(minorUnits, ExchangeRates.BASE)
        fun zero(currencyCode: String = ExchangeRates.BASE) = Money(0, currencyCode)
    }
}

fun Iterable<Money>.sum(currencyCode: String = ExchangeRates.BASE): Money =
    fold(Money.zero(currencyCode)) { acc, m -> acc + m }
