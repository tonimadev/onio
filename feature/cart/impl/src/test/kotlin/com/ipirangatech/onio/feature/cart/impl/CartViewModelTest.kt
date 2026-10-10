package com.ipirangatech.onio.feature.cart.impl

import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.testing.FakeCartRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun item(id: Long, cents: Long, qty: Int) = CartItem(id, "Item $id", "", Money.usd(cents), qty)

    private val cart = FakeCartRepository(listOf(item(1, 1_000, 2), item(2, 500, 1)))
    // Lazy: the ViewModel must be built after MainDispatcherRule has installed the test dispatcher.
    private val vm by lazy { CartViewModel(cart) }

    @Test fun showsLinesAndTheSubtotal() {
        val state = vm.uiState.value
        assertEquals(2, state.items.size)
        assertEquals(2_500L, state.subtotal.minorUnits)
    }

    @Test fun incrementAndDecrementChangeTheQuantity() {
        vm.onIntent(CartIntent.Increment(2))
        assertEquals(2, cart.state.value.first { it.productId == 2L }.quantity)
        vm.onIntent(CartIntent.Decrement(1))
        assertEquals(1, cart.state.value.first { it.productId == 1L }.quantity)
        assertEquals(2_000L, vm.uiState.value.subtotal.minorUnits) // 1 x 1000 + 2 x 500
    }

    @Test fun decrementingTheLastUnitRemovesTheLine() {
        vm.onIntent(CartIntent.Decrement(2))
        assertEquals(listOf(1L), cart.state.value.map { it.productId })
    }

    @Test fun removeDropsTheLineAndAnEmptyCartCannotCheckOut() {
        vm.onIntent(CartIntent.Remove(1))
        vm.onIntent(CartIntent.Remove(2))
        assertTrue(vm.uiState.value.items.isEmpty())
        vm.onIntent(CartIntent.CheckoutClicked)
        assertNull(vm.uiState.value.effect)
    }

    @Test fun checkoutWithItemsNavigatesAndOpenProductReportsTheId() {
        vm.onIntent(CartIntent.CheckoutClicked)
        assertEquals(CartEffect.NavigateToCheckout, vm.uiState.value.effect)
        vm.onIntent(CartIntent.ConsumeEffect)
        vm.onIntent(CartIntent.OpenProduct(2))
        assertEquals(CartEffect.NavigateToProduct(2), vm.uiState.value.effect)
    }
}
