package com.ipirangatech.onio.core.network

import com.ipirangatech.onio.core.network.dummyjson.ProductListDto
import com.ipirangatech.onio.core.network.dummyjson.toDomain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {
    @Test fun parsesDummyJsonPayloadAndMapsToCents() {
        val payload = """{"products":[{"id":1,"title":"Mascara","price":9.99,"discountPercentage":7.17,
            "category":"beauty","thumbnail":"t.png","images":["a.png"],"unknown":true,
            "reviews":[{"rating":3,"comment":"ok","date":"x","reviewerName":"Eleanor","reviewerEmail":"e"}]}],
            "total":1,"skip":0,"limit":30}"""
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<ProductListDto>(payload)
        val page = dto.toDomain()
        assertEquals(999L, page.products.single().price.minorUnits)
        assertEquals("Eleanor", page.products.single().reviews.single().reviewerName)
    }
}
