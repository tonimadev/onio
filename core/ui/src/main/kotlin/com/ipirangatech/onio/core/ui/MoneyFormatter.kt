package com.ipirangatech.onio.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.ipirangatech.onio.core.model.MoneyFormatter
import java.util.Locale

@Composable
fun rememberMoneyFormatter(currencyOverride: String?): MoneyFormatter {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(locale, currencyOverride) { MoneyFormatter.forLocale(locale, currencyOverride) }
}
