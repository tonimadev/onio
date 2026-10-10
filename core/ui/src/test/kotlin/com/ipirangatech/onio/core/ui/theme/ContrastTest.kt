package com.ipirangatech.onio.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG 2.x contrast checks for the text/background pairs the app actually draws. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun check(name: String, fg: Color, bg: Color, min: Double) {
        val r = ratio(fg, bg)
        assertTrue("$name: contrast %.2f < %.1f".format(r, min), r >= min)
    }

    @Test fun bodyTextMeetsAaInBothThemes() {
        for ((name, s, c) in listOf(
            Triple("light", LightScheme, LightOnioColors),
            Triple("dark", DarkScheme, DarkOnioColors),
        )) {
            check("$name onSurface/surface", s.onSurface, s.surface, 4.5)
            check("$name onSurface/background", s.onBackground, s.background, 4.5)
            check("$name secondary text/surface", s.onSurfaceVariant, s.surface, 4.5)
            check("$name secondary text/background", s.onSurfaceVariant, s.background, 4.5)
            check("$name price/surface", c.priceText, s.surface, 4.5)
            check("$name price/page background", c.priceText, c.pageBackground, 4.5)
            check("$name price/selected chip", c.priceText, s.primaryContainer, 4.5)
            check("$name free-shipping/surface", c.freeShipping, s.surface, 4.5)
            check("$name sale tag", c.saleTagText, c.saleTagBackground, 4.5)
            check("$name countdown digits", c.onCountdownBox, c.countdownBox, 4.5)
            check("$name error/surface", s.error, s.surface, 4.5)
        }
    }

    @Test fun brandFillsMeetTheLargeTextAndComponentThreshold() {
        // Solid coral buttons carry bold 14sp+ labels and icons: WCAG asks for 3:1 for components.
        check("light onPrimary/primary", LightScheme.onPrimary, LightScheme.primary, 3.0)
        check("dark onPrimary/primary", DarkScheme.onPrimary, DarkScheme.primary, 4.5)
        check("brand/white (links, outlines)", BrandCoral, Color.White, 3.0)
    }
}
