package com.ipirangatech.onio.core.model

data class Product(
    val id: Long,
    val title: String,
    val description: String,
    val categorySlug: String,
    val brand: String?,
    val price: Money,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val thumbnailUrl: String,
    val imageUrls: List<String>,
    val reviews: List<Review>,
) {
    /** Price before the advertised discount, for the strike-through "was" price. */
    val originalPrice: Money
        get() = if (discountPercentage <= 0.0) price
        else Money((price.minorUnits / (1 - discountPercentage / 100.0)).toLong(), price.currencyCode)
}

data class Review(
    val rating: Int,
    val comment: String,
    val reviewerName: String,
)

data class Category(val slug: String, val name: String)

data class ProductPage(val products: List<Product>, val total: Int, val skip: Int)
