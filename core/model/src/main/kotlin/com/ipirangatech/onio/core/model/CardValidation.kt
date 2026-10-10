package com.ipirangatech.onio.core.model

object CardValidation {
    fun digitsOnly(input: String): String = input.filter(Char::isDigit)

    fun passesLuhn(number: String): Boolean {
        val digits = digitsOnly(number)
        if (digits.length !in 12..19) return false
        var sum = 0
        digits.reversed().forEachIndexed { index, c ->
            var d = c - '0'
            if (index % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }

    fun brandOf(number: String): CardBrand {
        val d = digitsOnly(number)
        return when {
            d.startsWith("4") -> CardBrand.VISA
            d.length >= 2 && d.take(2).toInt() in 51..55 -> CardBrand.MASTERCARD
            d.length >= 4 && d.take(4).toInt() in 2221..2720 -> CardBrand.MASTERCARD
            d.startsWith("34") || d.startsWith("37") -> CardBrand.AMEX
            d.startsWith("636368") || d.startsWith("504175") -> CardBrand.ELO
            else -> CardBrand.OTHER
        }
    }

    fun isValidExpiry(month: Int, year: Int, nowYear: Int, nowMonth: Int): Boolean {
        if (month !in 1..12) return false
        val fullYear = if (year < 100) 2000 + year else year
        return fullYear > nowYear || (fullYear == nowYear && month >= nowMonth)
    }

    fun isValidCvv(cvv: String, brand: CardBrand): Boolean {
        val d = digitsOnly(cvv)
        return d.length == if (brand == CardBrand.AMEX) 4 else 3
    }
}
