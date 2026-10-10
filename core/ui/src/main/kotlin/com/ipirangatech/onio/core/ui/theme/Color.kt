package com.ipirangatech.onio.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Onio coral. Used for fills and large/bold text; small text on white uses [OnioColors.priceText]. */
val BrandCoral = Color(0xFFF23F5D)

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = BrandCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDF0),
    onPrimaryContainer = Color(0xFF7A1230),
    secondary = Color(0xFFFF7A59),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDF0),
    onSecondaryContainer = Color(0xFF7A1230),
    tertiary = Color(0xFF00806E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F5F1),
    onTertiaryContainer = Color(0xFF00574A),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF222222),
    surface = Color.White,
    onSurface = Color(0xFF222222),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF6B6B6B),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF222222),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFFF7043),
    error = Color(0xFFD0011B),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF8C0012),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE8E8E8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF7088),
    onPrimary = Color(0xFF3B0A14),
    primaryContainer = Color(0xFF4A1520),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFFF9A80),
    onSecondary = Color(0xFF3B0A14),
    secondaryContainer = Color(0xFF4A1520),
    onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFF4DD0BC),
    onTertiary = Color(0xFF00201B),
    tertiaryContainer = Color(0xFF00403A),
    onTertiaryContainer = Color(0xFFB2F0E5),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA8A8A8),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF222222),
    inversePrimary = BrandCoral,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0008),
    errorContainer = Color(0xFF5C1219),
    onErrorContainer = Color(0xFFFFDAD9),
    outline = Color(0xFF5C5C5C),
    outlineVariant = Color(0xFF333333),
    surfaceContainerLowest = Color(0xFF121212),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)

/** Tokens the Material scheme has no slot for (retail-specific colors). */
@Immutable
class OnioColors(
    /** Price and other small brand-colored text; darker than the brand fill so it reads on white (WCAG AA). */
    val priceText: Color,
    val saleTagBackground: Color,
    val saleTagText: Color,
    val star: Color,
    /** "Free shipping" teal: text/outline color and its tinted background. */
    val freeShipping: Color,
    val freeShippingContainer: Color,
    val border: Color,
    /** Brand gradient used by the home header, banners and the savings card. */
    val headerStart: Color,
    val headerEnd: Color,
    /** Plain page background behind white sections. */
    val pageBackground: Color,
    /** Dark boxes of the flash-sale countdown. */
    val countdownBox: Color,
    val onCountdownBox: Color,
    /** Credit-card mock-up gradient (deliberately not brand colored, so it looks like a real card). */
    val cardStart: Color,
    val cardEnd: Color,
    /** Digital coins: gold face and darker rim. */
    val coinFace: Color,
    val coinRim: Color,
)

internal val LightOnioColors = OnioColors(
    priceText = Color(0xFFC2183B),
    saleTagBackground = Color(0xFFFFD839),
    saleTagText = Color(0xFFB3260E),
    star = Color(0xFFFFCE3D),
    freeShipping = Color(0xFF00806E),
    freeShippingContainer = Color(0xFFE0F5F1),
    border = Color(0xFFE8E8E8),
    headerStart = Color(0xFFFF7A59),
    headerEnd = BrandCoral,
    pageBackground = Color(0xFFF5F5F5),
    countdownBox = Color(0xFF4A1226),
    onCountdownBox = Color.White,
    cardStart = Color(0xFF3A3A3C),
    cardEnd = Color(0xFF1C1C1E),
    coinFace = Color(0xFFFFC83D),
    coinRim = Color(0xFFE08A00),
)

internal val DarkOnioColors = OnioColors(
    priceText = Color(0xFFFF8FA3),
    saleTagBackground = Color(0xFFFFD839),
    saleTagText = Color(0xFFB3260E),
    star = Color(0xFFFFCE3D),
    freeShipping = Color(0xFF4DD0BC),
    freeShippingContainer = Color(0xFF00403A),
    border = Color(0xFF333333),
    headerStart = Color(0xFFC4344F),
    headerEnd = Color(0xFF8F2038),
    pageBackground = Color(0xFF121212),
    countdownBox = Color(0xFF2A0A16),
    onCountdownBox = Color.White,
    cardStart = Color(0xFF3A3A3C),
    cardEnd = Color(0xFF1C1C1E),
    coinFace = Color(0xFFFFC83D),
    coinRim = Color(0xFFE08A00),
)
