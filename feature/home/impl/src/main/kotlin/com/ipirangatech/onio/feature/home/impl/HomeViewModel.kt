package com.ipirangatech.onio.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.FavoritesRepository
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductRepository
import com.ipirangatech.onio.core.model.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val products: ProductRepository,
    private val cart: CartRepository,
    private val favorites: FavoritesRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    private val loaded = LinkedHashMap<Long, Product>()

    init {
        onIntent(HomeIntent.Load)
        viewModelScope.launch {
            runCatching {
                favorites.favorites.collect { list ->
                    _uiState.update { it.copy(favoriteIds = list.map { f -> f.productId }.toImmutableSet()) }
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                settings.settings.collect { value -> _uiState.update { it.copy(userName = value.userName) } }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load()
            HomeIntent.LoadMore -> loadMore()
            HomeIntent.SearchClicked -> emit(HomeEffect.NavigateToSearch)
            HomeIntent.ViewCartClicked -> emit(HomeEffect.NavigateToCart)
            is HomeIntent.AddToCart -> addToCart(intent.productId)
            is HomeIntent.ToggleFavorite -> toggleFavorite(intent.productId)
            is HomeIntent.ProductClicked -> emit(HomeEffect.NavigateToProduct(intent.productId))
            is HomeIntent.CategoryClicked -> emit(HomeEffect.NavigateToCategory(intent.slug))
            HomeIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun emit(effect: HomeEffect) = _uiState.update { it.copy(effect = effect) }

    private fun toggleFavorite(productId: Long) {
        val product = loaded[productId] ?: return
        viewModelScope.launch {
            runCatching { favorites.toggle(product) }.onFailure { _uiState.update { s -> s.copy(hasError = true) } }
        }
    }

    private fun addToCart(productId: Long) {
        val product = loaded[productId] ?: return
        viewModelScope.launch {
            runCatching { cart.add(product) }
                .onSuccess { emit(HomeEffect.ShowAdded) }
                .onFailure { _uiState.update { s -> s.copy(hasError = true) } }
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            runCatching { products.products(PAGE_SIZE, 0) to products.categories() }
                .onSuccess { (page, categories) ->
                    loaded.clear()
                    page.products.forEach { loaded[it.id] = it }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = categories.map { c -> c.toUiModel() }.toImmutableList(),
                            deals = page.products.sortedByDescending { p -> p.discountPercentage }
                                .take(DEALS).map { p -> p.toUiModel() }.toImmutableList(),
                            products = page.products.map { p -> p.toUiModel() }.toImmutableList(),
                            canLoadMore = page.products.size < page.total,
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        }
    }

    private fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isLoadingMore || !current.canLoadMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            runCatching { products.products(PAGE_SIZE, current.products.size) }
                .onSuccess { page ->
                    page.products.forEach { loaded[it.id] = it }
                    _uiState.update {
                        val merged = (it.products + page.products.map { p -> p.toUiModel() }).distinctBy(UiProduct::id)
                        it.copy(isLoadingMore = false, products = merged.toImmutableList(), canLoadMore = merged.size < page.total)
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoadingMore = false, canLoadMore = false) } }
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val DEALS = 8
    }
}
