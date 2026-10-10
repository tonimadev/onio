package com.ipirangatech.onio.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light: ColorScheme = lightColorScheme(
    primary = Color(0xFF6C3CE9),
    secondary = Color(0xFFFF7A1A),
    tertiary = Color(0xFF0E9F6E),
    background = Color(0xFFFAF9FD),
    surface = Color(0xFFFFFFFF),
)

private val Dark: ColorScheme = darkColorScheme(
    primary = Color(0xFFC9B8FF),
    secondary = Color(0xFFFFB27A),
    tertiary = Color(0xFF5FD9A6),
    background = Color(0xFF111015),
    surface = Color(0xFF1B1A21),
)

@Composable
fun OnioTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
