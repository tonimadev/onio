package com.ipirangatech.onio.feature.home.impl

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.testing.FakeCartRepository
import com.ipirangatech.onio.core.testing.FakeFavoritesRepository
import com.ipirangatech.onio.core.testing.FakeProductRepository
import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import com.ipirangatech.onio.core.testing.product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val products = FakeProductRepository(
        products = (1L..10L).map { product(it, discount = it.toDouble()) },
        categories = listOf(Category("beauty", "Beauty")),
    )
    private val cart = FakeCartRepository()
    private val favorites = FakeFavoritesRepository()
    private val settings = FakeSettingsRepository()

    private fun viewModel() = HomeViewModel(products, cart, favorites, settings)

    @Test fun loadsCatalogCategoriesAndTheBiggestDiscountsAsDeals() {
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        assertEquals(10, state.products.size)
        assertEquals(listOf("beauty"), state.categories.map { it.slug })
        assertEquals(listOf(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L), state.deals.map { it.id })
    }

    @Test fun failureShowsErrorAndRetryRecovers() {
        products.failure = RuntimeException("offline")
        val vm = viewModel()
        assertTrue(vm.uiState.value.hasError)
        products.failure = null
        vm.onIntent(HomeIntent.Load)
        assertFalse(vm.uiState.value.hasError)
        assertEquals(10, vm.uiState.value.products.size)
    }

    @Test fun addToCartStoresTheProductAndAsksToShowFeedback() {
        val vm = viewModel()
        vm.onIntent(HomeIntent.AddToCart(3))
        assertEquals(listOf(3L), cart.state.value.map { it.productId })
        assertEquals(HomeEffect.ShowAdded, vm.uiState.value.effect)
        vm.onIntent(HomeIntent.ConsumeEffect)
        assertNull(vm.uiState.value.effect)
    }

    @Test fun addToCartFailureIsReflectedInTheState() {
        cart.failure = IllegalStateException("db")
        val vm = viewModel()
        vm.onIntent(HomeIntent.AddToCart(3))
        assertTrue(vm.uiState.value.hasError)
    }

    @Test fun togglingAFavoriteUpdatesTheFavoriteIds() {
        val vm = viewModel()
        vm.onIntent(HomeIntent.ToggleFavorite(2))
        assertEquals(setOf(2L), vm.uiState.value.favoriteIds)
        vm.onIntent(HomeIntent.ToggleFavorite(2))
        assertTrue(vm.uiState.value.favoriteIds.isEmpty())
    }

    @Test fun greetsTheUserByTheirStoredNameAndFollowsChanges() {
        val vm = viewModel()
        assertNull(vm.uiState.value.userName)
        settings.state.value = AppSettings(userName = "Ana")
        assertEquals("Ana", vm.uiState.value.userName)
    }

    @Test fun navigationIntentsBecomeEffects() {
        val vm = viewModel()
        vm.onIntent(HomeIntent.ProductClicked(4))
        assertEquals(HomeEffect.NavigateToProduct(4), vm.uiState.value.effect)
        vm.onIntent(HomeIntent.CategoryClicked("beauty"))
        assertEquals(HomeEffect.NavigateToCategory("beauty"), vm.uiState.value.effect)
        vm.onIntent(HomeIntent.SearchClicked)
        assertEquals(HomeEffect.NavigateToSearch, vm.uiState.value.effect)
    }
}
