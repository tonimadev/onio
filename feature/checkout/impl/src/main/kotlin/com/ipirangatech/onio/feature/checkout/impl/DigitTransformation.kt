package com.ipirangatech.onio.feature.checkout.impl

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Shows a digits-only value with [separator] inserted before each index in [separatorsBefore]
 * (e.g. 4/8/12 for a card number). A separator only appears once a digit follows it, so there is
 * never a trailing one and the cursor keeps mapping 1:1 with what the user typed.
 */
internal class DigitSeparatorTransformation(
    private val separator: Char,
    private val separatorsBefore: List<Int>,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()
        raw.forEachIndexed { index, c ->
            if (index in separatorsBefore) out.append(separator)
            out.append(c)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + separatorsBefore.count { it < offset }

            override fun transformedToOriginal(offset: Int): Int {
                // Separator number k (0-based) sits at transformed index separatorsBefore[k] + k.
                val before = separatorsBefore.withIndex().count { (k, s) -> s < raw.length && s + k < offset }
                return (offset - before).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

internal val CardNumberTransformation = DigitSeparatorTransformation(' ', listOf(4, 8, 12, 16))
internal val ExpiryTransformation = DigitSeparatorTransformation('/', listOf(2))
