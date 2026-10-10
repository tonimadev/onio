package com.ipirangatech.onio.core.network.cep

import com.ipirangatech.onio.core.model.Address
import com.ipirangatech.onio.core.model.Cep
import com.ipirangatech.onio.core.model.CepSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@Serializable
internal data class ViaCepDto(
    val cep: String? = null,
    val logradouro: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    /** Present (as true or "true") when the CEP does not exist. */
    val erro: JsonElement? = null,
)

internal interface ViaCepApi {
    @GET("{cep}/json/")
    suspend fun find(@Path("cep") cep: String): ViaCepDto
}

@Serializable
internal data class BrasilApiDto(
    val cep: String? = null,
    val state: String? = null,
    val city: String? = null,
    val neighborhood: String? = null,
    val street: String? = null,
)

internal interface BrasilApiApi {
    @GET("api/cep/v2/{cep}")
    suspend fun find(@Path("cep") cep: String): BrasilApiDto
}

internal fun ViaCepDto.toDomain(requested: String): Address? =
    if (erro != null || localidade.isNullOrBlank() || uf.isNullOrBlank()) {
        null
    } else {
        Address(requested, logradouro.orEmpty(), bairro.orEmpty(), localidade, uf)
    }

internal fun BrasilApiDto.toDomain(requested: String): Address? =
    if (city.isNullOrBlank() || state.isNullOrBlank()) null else Address(requested, street.orEmpty(), neighborhood.orEmpty(), city, state)

private fun retrofit(baseUrl: String): Retrofit {
    val json = Json { ignoreUnknownKeys = true }
    val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
    return Retrofit.Builder().baseUrl(baseUrl).client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
}

/** ViaCEP first (it also knows the region); BrasilAPI is the fallback when ViaCEP is down or rate limited. */
internal class FallbackCepSource(
    private val viaCep: ViaCepApi,
    private val brasilApi: BrasilApiApi,
) : CepSource {
    override suspend fun find(cep: String): Address? {
        val digits = Cep.digits(cep)
        val primary = runCatching { viaCep.find(digits).toDomain(digits) to true }
        primary.getOrNull()?.let { (address, answered) -> if (answered) return address }
        return try {
            brasilApi.find(digits).toDomain(digits)
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
    }
}

fun createCepSource(): CepSource = FallbackCepSource(
    viaCep = retrofit("https://viacep.com.br/ws/").create(ViaCepApi::class.java),
    brasilApi = retrofit("https://brasilapi.com.br/").create(BrasilApiApi::class.java),
)
