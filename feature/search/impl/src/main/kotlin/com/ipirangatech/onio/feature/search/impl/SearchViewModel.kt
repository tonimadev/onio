package com.ipirangatech.onio.feature.search.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.FavoritesRepository
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel @AssistedInject constructor(
    @Assisted initialCategory: String?,
    private val products: ProductRepository,
    private val cart: CartRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(initialCategory: String?): SearchViewModel
    }

    private val _uiState = MutableStateFlow(SearchState(categorySlug = initialCategory))
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    private var searchJob: Job? = null
    private val loaded = HashMap<Long, Product>()
    private var remote: List<UiResult> = emptyList()
    private var saved: List<UiResult> = emptyList()

    init {
        viewModelScope.launch {
            runCatching { products.categories() }
                .onSuccess { list -> _uiState.update { it.copy(categories = list.map { c -> c.toUiModel() }.toImmutableList()) } }
        }
        viewModelScope.launch {
            runCatching {
                favorites.favorites.collect { list ->
                    saved = list.map { it.toUiModel() }
                    _uiState.update { it.copy(favoriteIds = list.map { f -> f.productId }.toImmutableSet()) }
                    recompute()
                }
            }
        }
        search(debounce = false)
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                _uiState.update { it.copy(query = intent.query) }
                if (_uiState.value.favoritesOnly) recompute() else search(debounce = true)
            }
            is SearchIntent.CategorySelected -> {
                _uiState.update { it.copy(categorySlug = intent.slug, favoritesOnly = false) }
                search(debounce = false)
            }
            SearchIntent.ToggleFavoritesOnly -> {
                _uiState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
                recompute()
            }
            is SearchIntent.ToggleFavorite -> toggleFavorite(intent.productId)
            is SearchIntent.AddToCart -> addToCart(intent.productId)
            SearchIntent.ViewCartClicked -> emit(SearchEffect.NavigateToCart)
            is SearchIntent.ProductClicked -> emit(SearchEffect.NavigateToProduct(intent.productId))
            SearchIntent.ToggleFilterPanel -> _uiState.update { it.copy(filterPanelOpen = !it.filterPanelOpen) }
            is SearchIntent.SortSelected -> updateFilters { it.copy(sort = intent.sort) }
            SearchIntent.ToggleOnSale -> updateFilters { it.copy(onSaleOnly = !it.onSaleOnly) }
            SearchIntent.ToggleTopRated -> updateFilters { it.copy(topRatedOnly = !it.topRatedOnly) }
            is SearchIntent.PriceRangeChanged -> updateFilters { it.copy(minPriceCents = intent.minCents, maxPriceCents = intent.maxCents) }
            SearchIntent.ResetFilters -> updateFilters { SearchFilters() }
            SearchIntent.Retry -> search(debounce = false)
            SearchIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun emit(effect: SearchEffect) = _uiState.update { it.copy(effect = effect) }

    private fun updateFilters(change: (SearchFilters) -> SearchFilters) {
        _uiState.update { it.copy(filters = change(it.filters)) }
        recompute()
    }

    /** Rebuilds the visible list from the right source (search results or saved items) and the filters. */
    private fun recompute() {
        _uiState.update { s ->
            val source = if (s.favoritesOnly) {
                saved.filter { s.query.isBlank() || it.title.contains(s.query.trim(), ignoreCase = true) }
            } else {
                remote
            }
            val prices = source.map(UiResult::priceCents)
            s.copy(
                results = source.applyFilters(s.filters).toImmutableList(),
                priceBounds = if (prices.isEmpty()) null else prices.min()..prices.max(),
            )
        }
    }

    private fun toggleFavorite(productId: Long) {
        viewModelScope.launch {
            runCatching {
                val product = loaded[productId]
                if (product != null) favorites.toggle(product) else favorites.remove(productId)
            }.onFailure { _uiState.update { it.copy(hasError = true) } }
        }
    }

    private fun addToCart(productId: Long) {
        val product = loaded[productId] ?: return
        viewModelScope.launch {
            runCatching { cart.add(product) }
                .onSuccess { emit(SearchEffect.ShowAdded) }
                .onFailure { _uiState.update { it.copy(hasError = true) } }
        }
    }

    private fun search(debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(DEBOUNCE_MILLIS)
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            val s = _uiState.value
            runCatching {
                when {
                    s.query.isNotBlank() -> products.search(s.query.trim(), LIMIT, 0)
                    s.categorySlug != null -> products.productsByCategory(s.categorySlug, LIMIT, 0)
                    else -> products.products(LIMIT, 0)
                }
            }.onSuccess { page ->
                page.products.forEach { loaded[it.id] = it }
                remote = page.products.map { it.toUiModel() }
                _uiState.update { it.copy(isLoading = false) }
                recompute()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 350L
        const val LIMIT = 60
    }
}
