package com.ipirangatech.onio.feature.search.impl

import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.testing.FakeCartRepository
import com.ipirangatech.onio.core.testing.FakeFavoritesRepository
import com.ipirangatech.onio.core.testing.FakeProductRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import com.ipirangatech.onio.core.testing.product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val products = FakeProductRepository(
        products = listOf(
            product(1, cents = 3_000, discount = 0.0, rating = 4.5, category = "beauty", title = "Red lipstick"),
            product(2, cents = 1_000, discount = 20.0, rating = 3.0, category = "beauty", title = "Mascara"),
            product(3, cents = 9_000, discount = 10.0, rating = 4.9, category = "laptops", title = "Laptop pro"),
        ),
        categories = listOf(Category("beauty", "Beauty"), Category("laptops", "Laptops")),
    )
    private val cart = FakeCartRepository()
    private val favorites = FakeFavoritesRepository()

    private fun viewModel(category: String? = null) = SearchViewModel(category, products, cart, favorites)

    @Test fun startsWithEveryProductAndTheCategoryChips() {
        val state = viewModel().uiState.value
        assertEquals(listOf(1L, 2L, 3L), state.results.map { it.id })
        assertEquals(2, state.categories.size)
        assertEquals(1_000L..9_000L, state.priceBounds)
    }

    @Test fun anInitialCategoryUsesTheCategoryEndpoint() {
        val vm = viewModel("laptops")
        assertEquals(listOf("laptops"), products.categoryRequests)
        assertEquals(listOf(3L), vm.uiState.value.results.map { it.id })
    }

    @Test fun typingIsDebouncedIntoASingleRequest() {
        val vm = viewModel()
        vm.onIntent(SearchIntent.QueryChanged("m"))
        main.scheduler.advanceTimeBy(100)
        vm.onIntent(SearchIntent.QueryChanged("ma"))
        main.scheduler.advanceTimeBy(100)
        vm.onIntent(SearchIntent.QueryChanged("mascara"))
        main.scheduler.advanceTimeBy(500)
        assertEquals(listOf("mascara"), products.searches)
        assertEquals(listOf(2L), vm.uiState.value.results.map { it.id })
    }

    @Test fun sortingAndFiltersAreAppliedLocallyAndCanBeReset() {
        val vm = viewModel()
        vm.onIntent(SearchIntent.SortSelected(SortOption.PRICE_ASC))
        assertEquals(listOf(2L, 1L, 3L), vm.uiState.value.results.map { it.id })
        vm.onIntent(SearchIntent.ToggleTopRated)
        assertEquals(listOf(1L, 3L), vm.uiState.value.results.map { it.id })
        assertEquals(2, vm.uiState.value.filters.activeCount)
        vm.onIntent(SearchIntent.ResetFilters)
        assertEquals(0, vm.uiState.value.filters.activeCount)
        assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.results.map { it.id })
    }

    @Test fun favoritesModeListsSavedItemsAndFiltersThemByTheQueryWithoutTheNetwork() {
        val vm = viewModel()
        vm.onIntent(SearchIntent.ToggleFavorite(1))
        vm.onIntent(SearchIntent.ToggleFavorite(3))
        vm.onIntent(SearchIntent.ToggleFavoritesOnly)
        assertEquals(setOf(1L, 3L), vm.uiState.value.results.map { it.id }.toSet())
        val requestsBefore = products.searches.size
        vm.onIntent(SearchIntent.QueryChanged("laptop"))
        assertEquals(listOf(3L), vm.uiState.value.results.map { it.id })
        assertEquals(requestsBefore, products.searches.size)
    }

    @Test fun unfavoritingAnItemOnlyKnownFromAnOlderSessionStillWorks() {
        favorites.state.value = listOf(
            com.ipirangatech.onio.core.model.FavoriteItem(99, "Old find", "", com.ipirangatech.onio.core.model.Money.usd(500), 0.0, 4.0),
        )
        val vm = viewModel()
        vm.onIntent(SearchIntent.ToggleFavoritesOnly)
        assertEquals(listOf(99L), vm.uiState.value.results.map { it.id })
        vm.onIntent(SearchIntent.ToggleFavorite(99))
        assertTrue(vm.uiState.value.results.isEmpty())
    }

    @Test fun addToCartAndFailureHandling() {
        val vm = viewModel()
        vm.onIntent(SearchIntent.AddToCart(2))
        assertEquals(listOf(2L), cart.state.value.map { it.productId })
        assertEquals(SearchEffect.ShowAdded, vm.uiState.value.effect)
        cart.failure = RuntimeException("x")
        vm.onIntent(SearchIntent.AddToCart(1))
        assertTrue(vm.uiState.value.hasError)
    }

    @Test fun loadErrorCanBeRetried() {
        products.failure = RuntimeException("offline")
        val vm = viewModel()
        assertTrue(vm.uiState.value.hasError)
        assertFalse(vm.uiState.value.isLoading)
        products.failure = null
        vm.onIntent(SearchIntent.Retry)
        assertFalse(vm.uiState.value.hasError)
        assertEquals(3, vm.uiState.value.results.size)
    }
}
