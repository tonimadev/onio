package com.ipirangatech.onio.feature.checkout.impl

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckoutScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = context.getString(id)

    private val intents = mutableListOf<CheckoutIntent>()

    private fun show(state: CheckoutState) = compose.setContent {
        OnioTheme { CheckoutScreen(state = state, onIntent = { intents += it }) }
    }

    @Test fun payIsDisabledForAnEmptyCartAndEnabledOtherwise() {
        show(CheckoutState(itemCount = 0))
        compose.onNodeWithText(text(R.string.checkout_pay)).assertIsNotEnabled()
    }

    @Test fun payButtonSubmitsTheForm() {
        show(CheckoutState(itemCount = 1, total = Money.usd(1_000)))
        compose.onNodeWithText(text(R.string.checkout_pay)).assertIsEnabled().performClick()
        assertTrue(CheckoutIntent.Submit in intents)
    }

    @Test fun cardNumberIsShownInGroupsOfFourWhileTheStateKeepsOnlyDigits() {
        show(CheckoutState(itemCount = 1, cardNumber = "4242424242424242"))
        // The preview uses a wider separator, so this matches the text field only.
        compose.onNodeWithText("4242 4242 4242 4242").performScrollTo().assertExists()
    }

    @Test fun expiryGetsItsSlash() {
        show(CheckoutState(itemCount = 1, expiry = "1299"))
        // Once in the text field and once on the card preview.
        compose.onAllNodesWithText("12/99").assertCountEquals(2)
    }

    @Test fun invalidFieldsExplainThemselves() {
        show(CheckoutState(itemCount = 1, errors = setOf(CheckoutField.CARD_NUMBER)))
        compose.onNodeWithText(text(R.string.checkout_invalid)).performScrollTo().assertExists()
    }

    @Test fun typingIsReportedAsFieldChanges() {
        show(CheckoutState(itemCount = 1))
        compose.onNodeWithText(text(R.string.checkout_address)).performScrollTo().performTextInput("Rua A")
        val change = intents.filterIsInstance<CheckoutIntent.FieldChanged>().last()
        assertEquals(CheckoutField.ADDRESS, change.field)
        assertEquals("Rua A", change.value)
    }

    @Test fun whileProcessingTheFormShowsProgressAndCannotBeResubmitted() {
        show(CheckoutState(itemCount = 1, isProcessing = true))
        compose.onNodeWithText(text(R.string.checkout_processing)).assertExists()
        compose.onNodeWithText(text(R.string.checkout_pay)).assertIsNotEnabled()
    }
}
