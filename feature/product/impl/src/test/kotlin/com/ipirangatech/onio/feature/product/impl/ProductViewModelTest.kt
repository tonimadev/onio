package com.ipirangatech.onio.feature.product.impl

import com.ipirangatech.onio.core.testing.FakeCartRepository
import com.ipirangatech.onio.core.testing.FakeFavoritesRepository
import com.ipirangatech.onio.core.testing.FakeProductRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import com.ipirangatech.onio.core.testing.product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProductViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val products = FakeProductRepository(products = listOf(product(7, cents = 2_500, discount = 20.0)))
    private val cart = FakeCartRepository()
    private val favorites = FakeFavoritesRepository()

    private fun viewModel(id: Long = 7) = ProductViewModel(id, products, cart, favorites)

    @Test fun loadsTheProductWithAStrikeThroughPriceWhenDiscounted() {
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        val detail = state.product!!
        assertEquals(7L, detail.id)
        assertEquals(20, detail.discountPercent)
        assertNotNull(detail.originalPrice)
    }

    @Test fun unknownProductOrNetworkErrorLeavesRetryAvailable() {
        products.failure = RuntimeException("offline")
        val vm = viewModel()
        assertTrue(vm.uiState.value.hasError)
        assertNull(vm.uiState.value.product)
        products.failure = null
        vm.onIntent(ProductIntent.Load)
        assertNotNull(vm.uiState.value.product)
    }

    @Test fun addToCartShowsFeedbackWhileBuyNowGoesStraightToTheCart() {
        val vm = viewModel()
        vm.onIntent(ProductIntent.AddToCart)
        assertEquals(ProductEffect.ShowAdded, vm.uiState.value.effect)
        vm.onIntent(ProductIntent.ConsumeEffect)
        vm.onIntent(ProductIntent.BuyNow)
        assertEquals(ProductEffect.NavigateToCart, vm.uiState.value.effect)
        assertEquals(2, cart.state.value.single().quantity)
    }

    @Test fun favoriteStateFollowsTheRepository() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.isFavorite)
        vm.onIntent(ProductIntent.ToggleFavorite)
        assertTrue(vm.uiState.value.isFavorite)
        favorites.state.value = emptyList()
        assertFalse(vm.uiState.value.isFavorite)
    }

    @Test fun backBecomesAnEffect() {
        val vm = viewModel()
        vm.onIntent(ProductIntent.BackClicked)
        assertEquals(ProductEffect.NavigateBack, vm.uiState.value.effect)
    }
}
