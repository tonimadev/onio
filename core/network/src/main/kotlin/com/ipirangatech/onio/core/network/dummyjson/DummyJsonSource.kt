package com.ipirangatech.onio.core.network.dummyjson

import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductPage
import com.ipirangatech.onio.core.network.ProductRemoteSource
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

internal interface DummyJsonApi {
    @GET("products")
    suspend fun products(@Query("limit") limit: Int, @Query("skip") skip: Int): ProductListDto

    @GET("products/search")
    suspend fun search(@Query("q") query: String, @Query("limit") limit: Int, @Query("skip") skip: Int): ProductListDto

    @GET("products/category/{slug}")
    suspend fun byCategory(@Path("slug") slug: String, @Query("limit") limit: Int, @Query("skip") skip: Int): ProductListDto

    @GET("products/categories")
    suspend fun categories(): List<CategoryDto>

    @GET("products/{id}")
    suspend fun product(@Path("id") id: Long): ProductDto
}

internal class DummyJsonSource(private val api: DummyJsonApi) : ProductRemoteSource {
    override suspend fun products(limit: Int, skip: Int): ProductPage = api.products(limit, skip).toDomain()
    override suspend fun search(query: String, limit: Int, skip: Int): ProductPage = api.search(query, limit, skip).toDomain()
    override suspend fun productsByCategory(slug: String, limit: Int, skip: Int): ProductPage = api.byCategory(slug, limit, skip).toDomain()
    override suspend fun categories(): List<Category> = api.categories().map(CategoryDto::toDomain)
    override suspend fun product(id: Long): Product = api.product(id).toDomain()
}

/** Factory used by the DI graph; keeps Retrofit/DTO types out of other modules. */
fun createDummyJsonSource(baseUrl: String = "https://dummyjson.com/"): ProductRemoteSource {
    val json = Json { ignoreUnknownKeys = true }
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(DummyJsonApi::class.java)
    return DummyJsonSource(api)
}
