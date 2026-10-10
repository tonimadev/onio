package com.ipirangatech.onio.feature.home.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Product
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Immutable
data class UiProduct(
    val id: Long,
    val title: String,
    val thumbnailUrl: String,
    val price: Money,
    val originalPrice: Money?,
    val discountPercent: Int,
    val rating: Double,
)

@Immutable
data class UiCategory(val slug: String, val name: String)

internal fun Product.toUiModel() = UiProduct(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    price = price,
    originalPrice = originalPrice.takeIf { discountPercentage > 0 },
    discountPercent = discountPercentage.roundToInt(),
    rating = rating,
)

internal fun Category.toUiModel() = UiCategory(slug, name)

sealed interface HomeEffect {
    data class NavigateToProduct(val productId: Long) : HomeEffect
    data class NavigateToCategory(val slug: String) : HomeEffect
    data object NavigateToSearch : HomeEffect
    data object NavigateToCart : HomeEffect
    data object ShowAdded : HomeEffect
}

sealed interface HomeIntent {
    data object Load : HomeIntent
    data object LoadMore : HomeIntent
    data object SearchClicked : HomeIntent
    data object ViewCartClicked : HomeIntent
    data class AddToCart(val productId: Long) : HomeIntent
    data class ProductClicked(val productId: Long) : HomeIntent
    data class CategoryClicked(val slug: String) : HomeIntent
    data object ConsumeEffect : HomeIntent
}

@Immutable
data class HomeState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasError: Boolean = false,
    val categories: ImmutableList<UiCategory> = persistentListOf(),
    val deals: ImmutableList<UiProduct> = persistentListOf(),
    val products: ImmutableList<UiProduct> = persistentListOf(),
    val canLoadMore: Boolean = false,
    val userName: String? = null,
    val effect: HomeEffect? = null,
)
