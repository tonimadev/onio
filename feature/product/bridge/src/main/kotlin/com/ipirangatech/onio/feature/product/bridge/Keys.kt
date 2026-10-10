package com.ipirangatech.onio.feature.product.bridge

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ProductKey(val productId: Long) : NavKey
