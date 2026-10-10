package com.ipirangatech.onio.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalOnioColors = staticCompositionLocalOf { LightOnioColors }

/** Access to the retail-specific tokens: `OnioTheme.colors.priceText`. Mirrors how `MaterialTheme` is used. */
object OnioTheme {
    val colors: OnioColors
        @Composable @ReadOnlyComposable get() = LocalOnioColors.current
}

@Composable
fun OnioTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOnioColors provides if (darkTheme) DarkOnioColors else LightOnioColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            shapes = OnioShapes,
            typography = OnioTypography,
            content = content,
        )
    }
}
