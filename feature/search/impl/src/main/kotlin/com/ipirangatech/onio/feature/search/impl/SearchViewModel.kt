package com.ipirangatech.onio.feature.search.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
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

    init {
        viewModelScope.launch {
            runCatching { products.categories() }
                .onSuccess { list -> _uiState.update { it.copy(categories = list.map { c -> c.toUiModel() }.toImmutableList()) } }
        }
        search(debounce = false)
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                _uiState.update { it.copy(query = intent.query) }
                search(debounce = true)
            }
            is SearchIntent.CategorySelected -> {
                _uiState.update { it.copy(categorySlug = intent.slug) }
                search(debounce = false)
            }
            is SearchIntent.AddToCart -> {
                val product = loaded[intent.productId] ?: return
                viewModelScope.launch {
                    runCatching { cart.add(product) }
                        .onSuccess { _uiState.update { it.copy(effect = SearchEffect.ShowAdded) } }
                        .onFailure { _uiState.update { it.copy(hasError = true) } }
                }
            }
            SearchIntent.ViewCartClicked -> _uiState.update { it.copy(effect = SearchEffect.NavigateToCart) }
            is SearchIntent.ProductClicked -> _uiState.update { it.copy(effect = SearchEffect.NavigateToProduct(intent.productId)) }
            SearchIntent.Retry -> search(debounce = false)
            SearchIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
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
                _uiState.update { it.copy(isLoading = false, results = page.products.map { p -> p.toUiModel() }.toImmutableList()) }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 350L
        const val LIMIT = 60
    }
}
