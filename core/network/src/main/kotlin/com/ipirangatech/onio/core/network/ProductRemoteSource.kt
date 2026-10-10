package com.ipirangatech.onio.core.network

import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductPage

/**
 * Contract of any catalog backend. Only domain types cross this boundary (DTOs stay internal), so a
 * real-price provider can replace DummyJSON by implementing this interface.
 */
interface ProductRemoteSource {
    suspend fun products(limit: Int, skip: Int): ProductPage
    suspend fun search(query: String, limit: Int, skip: Int): ProductPage
    suspend fun productsByCategory(slug: String, limit: Int, skip: Int): ProductPage
    suspend fun categories(): List<Category>
    suspend fun product(id: Long): Product
}
