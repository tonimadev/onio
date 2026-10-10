package com.ipirangatech.onio.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon for a DummyJSON category slug; unknown slugs get a neutral star. */
fun categoryIcon(slug: String): ImageVector = when (slug) {
    "beauty" -> Icons.Filled.Face
    "fragrances", "skin-care" -> Icons.Filled.Spa
    "furniture" -> Icons.Filled.Chair
    "groceries" -> Icons.Filled.LocalGroceryStore
    "home-decoration" -> Icons.Filled.Home
    "kitchen-accessories" -> Icons.Filled.Kitchen
    "laptops" -> Icons.Filled.Laptop
    "mens-shirts", "tops", "womens-dresses" -> Icons.Filled.Checkroom
    "mens-shoes", "womens-shoes" -> Icons.Filled.ShoppingBag
    "mens-watches", "womens-watches" -> Icons.Filled.Watch
    "mobile-accessories", "smartphones" -> Icons.Filled.PhoneAndroid
    "motorcycle" -> Icons.Filled.TwoWheeler
    "sports-accessories" -> Icons.Filled.SportsSoccer
    "sunglasses" -> Icons.Filled.Visibility
    "tablets" -> Icons.Filled.TabletAndroid
    "vehicle" -> Icons.Filled.DirectionsCar
    "womens-bags" -> Icons.Filled.ShoppingBag
    "womens-jewellery" -> Icons.Filled.Diamond
    else -> Icons.Filled.Star
}
