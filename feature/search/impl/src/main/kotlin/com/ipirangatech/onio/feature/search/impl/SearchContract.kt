package com.ipirangatech.onio.feature.search.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Product
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Immutable
data class UiResult(
    val id: Long,
    val title: String,
    val thumbnailUrl: String,
    val price: Money,
    val originalPrice: Money?,
    val discountPercent: Int,
    val rating: Double,
)

@Immutable
data class UiFilter(val slug: String, val name: String)

internal fun Product.toUiModel() = UiResult(id, title, thumbnailUrl, price, originalPrice.takeIf { discountPercentage > 0 }, discountPercentage.roundToInt(), rating)
internal fun Category.toUiModel() = UiFilter(slug, name)

sealed interface SearchEffect {
    data class NavigateToProduct(val productId: Long) : SearchEffect
    data object NavigateToCart : SearchEffect
    data object ShowAdded : SearchEffect
}

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    /** Null clears the category filter. */
    data class CategorySelected(val slug: String?) : SearchIntent
    data class ProductClicked(val productId: Long) : SearchIntent
    data class AddToCart(val productId: Long) : SearchIntent
    data object ViewCartClicked : SearchIntent
    data object Retry : SearchIntent
    data object ConsumeEffect : SearchIntent
}

@Immutable
data class SearchState(
    val query: String = "",
    val categorySlug: String? = null,
    val categories: ImmutableList<UiFilter> = persistentListOf(),
    val results: ImmutableList<UiResult> = persistentListOf(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val effect: SearchEffect? = null,
)
