package com.ipirangatech.onio.core.ui

import java.text.NumberFormat
import java.util.Locale

/** Locale-aware clock text like 29:59 or 1:02:03 (native digits where the locale has them). */
fun formatCountdown(millis: Long, locale: Locale = Locale.getDefault()): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    val plain = NumberFormat.getIntegerInstance(locale).also { it.isGroupingUsed = false }
    val twoDigits = NumberFormat.getIntegerInstance(locale).also {
        it.isGroupingUsed = false
        it.minimumIntegerDigits = 2
    }
    val tail = twoDigits.format(minutes) + ":" + twoDigits.format(seconds)
    return if (hours > 0) plain.format(hours) + ":" + tail else tail
}
