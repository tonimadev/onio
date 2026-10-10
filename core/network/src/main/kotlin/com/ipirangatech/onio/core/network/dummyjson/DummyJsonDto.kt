package com.ipirangatech.onio.core.network.dummyjson

import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductPage
import com.ipirangatech.onio.core.model.Review
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

@Serializable
internal data class ProductListDto(
    val products: List<ProductDto> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
)

@Serializable
internal data class ProductDto(
    val id: Long,
    val title: String,
    val description: String = "",
    val category: String = "",
    val brand: String? = null,
    val price: Double,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val thumbnail: String = "",
    val images: List<String> = emptyList(),
    val reviews: List<ReviewDto> = emptyList(),
)

@Serializable
internal data class ReviewDto(
    val rating: Int = 0,
    val comment: String = "",
    val reviewerName: String = "",
)

@Serializable
internal data class CategoryDto(val slug: String, val name: String)

internal fun ReviewDto.toDomain() = Review(rating, comment, reviewerName)

internal fun ProductDto.toDomain() = Product(
    id = id,
    title = title,
    description = description,
    categorySlug = category,
    brand = brand,
    price = Money.usd((price * 100).roundToLong()),
    discountPercentage = discountPercentage,
    rating = rating,
    stock = stock,
    thumbnailUrl = thumbnail,
    imageUrls = images.ifEmpty { listOf(thumbnail) },
    reviews = reviews.map(ReviewDto::toDomain),
)

internal fun ProductListDto.toDomain() = ProductPage(products.map(ProductDto::toDomain), total, skip)

internal fun CategoryDto.toDomain() = Category(slug, name)
