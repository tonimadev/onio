package com.ipirangatech.onio.feature.checkout.impl

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.CardBrand
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.testing.FakeCartRepository
import com.ipirangatech.onio.core.testing.FakeOrderRepository
import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val cart = FakeCartRepository(listOf(CartItem(1, "Lamp", "t.png", Money.usd(2_000), 2), CartItem(2, "Mug", "m.png", Money.usd(500), 1)))
    private val orders = FakeOrderRepository()
    private val settings = FakeSettingsRepository(AppSettings(userName = "Ana", currencyOverride = "EUR"))

    private fun viewModel() = CheckoutViewModel(cart, orders, settings)

    private fun CheckoutViewModel.fillValidForm() {
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.ADDRESS, "Rua A, 1"))
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.CARD_NUMBER, "4242 4242 4242 4242"))
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.EXPIRY, "12/99"))
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.CVV, "123"))
    }

    @Test fun summarisesTheCartAndPrefillsTheNameTheUserChose() {
        val state = viewModel().uiState.value
        assertEquals(3, state.itemCount)
        assertEquals(4_500L, state.total.minorUnits)
        assertEquals(listOf("t.png", "m.png"), state.thumbnails)
        assertEquals("Ana", state.name)
    }

    @Test fun cardFieldsKeepDigitsOnlyAndRespectTheirMaximumLengths() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.CARD_NUMBER, "4242-4242 4242 4242 4242 99"))
        assertEquals("4242424242424242424", vm.uiState.value.cardNumber) // 22 digits typed, 19 kept
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.EXPIRY, "12/3x45"))
        assertEquals("1234", vm.uiState.value.expiry)
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.CVV, "12345"))
        assertEquals("1234", vm.uiState.value.cvv)
    }

    @Test fun submittingAnIncompleteFormMarksEveryInvalidFieldAndPlacesNothing() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.NAME, ""))
        vm.onIntent(CheckoutIntent.Submit)
        assertEquals(CheckoutField.entries.toSet(), vm.uiState.value.errors)
        assertTrue(orders.placed.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
    }

    @Test fun editingAFieldClearsItsOwnError() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.Submit)
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.ADDRESS, "Rua A"))
        assertFalse(CheckoutField.ADDRESS in vm.uiState.value.errors)
        assertTrue(CheckoutField.CARD_NUMBER in vm.uiState.value.errors)
    }

    @Test fun anExpiredCardIsRejected() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.EXPIRY, "0120"))
        vm.onIntent(CheckoutIntent.Submit)
        assertEquals(setOf(CheckoutField.EXPIRY), vm.uiState.value.errors)
    }

    @Test fun aValidPaymentTakesAMomentThenPlacesTheOrderAndEmptiesTheCart() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        assertTrue(vm.uiState.value.isProcessing)
        assertTrue(orders.placed.isEmpty())

        main.scheduler.advanceUntilIdle()

        val request = orders.placed.single()
        assertEquals("4242", request.card.last4)
        assertEquals(CardBrand.VISA, request.card.brand)
        assertEquals("EUR", request.currencyCode)
        assertEquals("Ana", request.shippingName)
        assertEquals(2, request.items.size)
        assertTrue(cart.state.value.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
        assertTrue(vm.uiState.value.effect is CheckoutEffect.OrderPlaced)
    }

    @Test fun doubleTappingPayDoesNotPlaceTwoOrders() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        vm.onIntent(CheckoutIntent.Submit)
        main.scheduler.advanceUntilIdle()
        assertEquals(1, orders.placed.size)
    }

    @Test fun aFailedOrderReportsAnErrorAndKeepsTheCart() {
        orders.failure = RuntimeException("disk full")
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        main.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.hasError)
        assertFalse(vm.uiState.value.isProcessing)
        assertNull(vm.uiState.value.effect)
        assertEquals(2, cart.state.value.size)
    }

    @Test fun backBecomesAnEffect() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.BackClicked)
        assertEquals(CheckoutEffect.NavigateBack, vm.uiState.value.effect)
    }
}
