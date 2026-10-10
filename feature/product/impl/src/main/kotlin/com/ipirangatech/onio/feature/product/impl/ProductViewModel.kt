package com.ipirangatech.onio.feature.product.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ProductViewModel.Factory::class)
class ProductViewModel @AssistedInject constructor(
    @Assisted private val productId: Long,
    private val products: ProductRepository,
    private val cart: CartRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(productId: Long): ProductViewModel
    }

    private val _uiState = MutableStateFlow(ProductState())
    val uiState: StateFlow<ProductState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    private var loaded: Product? = null

    init {
        onIntent(ProductIntent.Load)
    }

    fun onIntent(intent: ProductIntent) {
        when (intent) {
            ProductIntent.Load -> load()
            ProductIntent.AddToCart -> addToCart(ProductEffect.ShowAdded)
            ProductIntent.BuyNow -> addToCart(ProductEffect.NavigateToCart)
            ProductIntent.BackClicked -> _uiState.update { it.copy(effect = ProductEffect.NavigateBack) }
            ProductIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            runCatching { products.product(productId) }
                .onSuccess { product ->
                    loaded = product
                    _uiState.update { it.copy(isLoading = false, product = product.toUiModel()) }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        }
    }

    private fun addToCart(then: ProductEffect) {
        val product = loaded ?: return
        viewModelScope.launch {
            runCatching { cart.add(product) }
                .onSuccess { _uiState.update { it.copy(effect = then) } }
                .onFailure { _uiState.update { it.copy(hasError = true) } }
        }
    }
}
