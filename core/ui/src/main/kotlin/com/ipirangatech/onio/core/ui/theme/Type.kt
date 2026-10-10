package com.ipirangatech.onio.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun TextStyle.dense(size: Int, line: Int, weight: FontWeight) =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = 0.sp)

private val Base = Typography()

/** Denser scale than Material's, and no letter spacing (the M3 defaults spread letters noticeably). */
internal val OnioTypography = Typography(
    displaySmall = Base.displaySmall.dense(28, 34, FontWeight.Bold),
    headlineMedium = Base.headlineMedium.dense(22, 28, FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.dense(20, 26, FontWeight.SemiBold),
    titleLarge = Base.titleLarge.dense(18, 24, FontWeight.SemiBold),
    titleMedium = Base.titleMedium.dense(15, 20, FontWeight.SemiBold),
    titleSmall = Base.titleSmall.dense(13, 18, FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.dense(15, 22, FontWeight.Normal),
    bodyMedium = Base.bodyMedium.dense(14, 20, FontWeight.Normal),
    bodySmall = Base.bodySmall.dense(12, 16, FontWeight.Normal),
    labelLarge = Base.labelLarge.dense(14, 20, FontWeight.Medium),
    labelMedium = Base.labelMedium.dense(12, 16, FontWeight.Medium),
    labelSmall = Base.labelSmall.dense(11, 14, FontWeight.Medium),
)
