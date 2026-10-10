package com.ipirangatech.onio.feature.product.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.DeliveryEstimate
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.Review
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt

@Immutable
data class UiReview(val rating: Int, val comment: String, val reviewer: String)

@Immutable
data class UiProductDetail(
    val id: Long,
    val title: String,
    val description: String,
    val categorySlug: String,
    val brand: String?,
    val price: Money,
    val originalPrice: Money?,
    val discountPercent: Int,
    val rating: Double,
    val stock: Int,
    val images: ImmutableList<String>,
    val reviews: ImmutableList<UiReview>,
)

internal fun Review.toUiModel() = UiReview(rating, comment, reviewerName)

internal fun Product.toUiModel() = UiProductDetail(
    id = id,
    title = title,
    description = description,
    categorySlug = categorySlug,
    brand = brand,
    price = price,
    originalPrice = originalPrice.takeIf { discountPercentage > 0 },
    discountPercent = discountPercentage.roundToInt(),
    rating = rating,
    stock = stock,
    images = imageUrls.toImmutableList(),
    reviews = reviews.map(Review::toUiModel).toImmutableList(),
)

sealed interface ProductEffect {
    data object ShowAdded : ProductEffect
    data object NavigateToCart : ProductEffect
    data object NavigateBack : ProductEffect
}

sealed interface ProductIntent {
    data object Load : ProductIntent
    data object AddToCart : ProductIntent
    data object BuyNow : ProductIntent
    data object BackClicked : ProductIntent
    data object ToggleFavorite : ProductIntent
    data object ConsumeEffect : ProductIntent
}

@Immutable
data class ProductState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val product: UiProductDetail? = null,
    val isFavorite: Boolean = false,
    /** From the CEP the customer already used; null falls back to a generic message. */
    val delivery: DeliveryEstimate? = null,
    val deliveryCity: String? = null,
    val effect: ProductEffect? = null,
    val reviews: ImmutableList<UiReview> = persistentListOf(),
)
