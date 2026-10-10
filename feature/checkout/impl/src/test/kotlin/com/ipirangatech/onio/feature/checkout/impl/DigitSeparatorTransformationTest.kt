package com.ipirangatech.onio.feature.checkout.impl

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitSeparatorTransformationTest {
    private fun shown(t: DigitSeparatorTransformation, raw: String) = t.filter(AnnotatedString(raw)).text.text

    @Test fun cardNumberIsGroupedByFourWithoutTrailingSpace() {
        assertEquals("4242", shown(CardNumberTransformation, "4242"))
        assertEquals("4242 4", shown(CardNumberTransformation, "42424"))
        assertEquals("4242 4242 4242 4242", shown(CardNumberTransformation, "4242424242424242"))
    }

    @Test fun expiryGetsASlashOnlyAfterTheMonth() {
        assertEquals("12", shown(ExpiryTransformation, "12"))
        assertEquals("12/3", shown(ExpiryTransformation, "123"))
        assertEquals("12/30", shown(ExpiryTransformation, "1230"))
    }

    @Test fun cursorMappingRoundTripsAroundSeparators() {
        val raw = "4242424242"
        val mapping = CardNumberTransformation.filter(AnnotatedString(raw)).offsetMapping
        for (offset in 0..raw.length) {
            assertEquals(offset, mapping.transformedToOriginal(mapping.originalToTransformed(offset)))
        }
        // Cursor right after the 4th digit stays before the space; after the 5th it is past it.
        assertEquals(4, mapping.originalToTransformed(4))
        assertEquals(6, mapping.originalToTransformed(5))
    }
}
