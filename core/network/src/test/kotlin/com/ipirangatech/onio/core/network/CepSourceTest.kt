package com.ipirangatech.onio.core.network

import com.ipirangatech.onio.core.network.cep.BrasilApiApi
import com.ipirangatech.onio.core.network.cep.BrasilApiDto
import com.ipirangatech.onio.core.network.cep.FallbackCepSource
import com.ipirangatech.onio.core.network.cep.ViaCepApi
import com.ipirangatech.onio.core.network.cep.ViaCepDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CepSourceTest {
    private fun source(via: suspend (String) -> ViaCepDto, brasil: suspend (String) -> BrasilApiDto = { error("not called") }) =
        FallbackCepSource(
            object : ViaCepApi { override suspend fun find(cep: String) = via(cep) },
            object : BrasilApiApi { override suspend fun find(cep: String) = brasil(cep) },
        )

    @Test fun parsesViaCepAndMapsToTheDomain() = runBlocking {
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<ViaCepDto>(
            """{"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé","localidade":"São Paulo","uf":"SP","ibge":"3550308"}""",
        )
        val address = source({ dto }).find("01001000")!!
        assertEquals("São Paulo", address.city)
        assertEquals("SP", address.state)
        assertEquals("Praça da Sé, Sé, São Paulo - SP", address.asLine())
    }

    @Test fun anUnknownCepIsNullEvenWhenErroIsABooleanOrAString() = runBlocking {
        val asBoolean = Json { ignoreUnknownKeys = true }.decodeFromString<ViaCepDto>("""{"erro":true}""")
        val asString = ViaCepDto(erro = JsonPrimitive("true"))
        assertNull(source({ asBoolean }).find("99999999"))
        assertNull(source({ asString }).find("99999999"))
    }

    @Test fun fallsBackToBrasilApiWhenViaCepFails() = runBlocking {
        val result = source(
            via = { throw java.io.IOException("down") },
            brasil = { BrasilApiDto(state = "RJ", city = "Rio de Janeiro", neighborhood = "Centro", street = "Rua A") },
        ).find("20000000")!!
        assertEquals("RJ", result.state)
    }

    @Test fun aBrasilApi404MeansTheCepDoesNotExist() = runBlocking {
        val notFound = HttpException(Response.error<BrasilApiDto>(404, "{}".toResponseBody()))
        assertNull(source({ throw java.io.IOException("down") }, { throw notFound }).find("00000001"))
    }

    @Test fun bothServicesDownSurfacesTheFailure() {
        try {
            runBlocking { source({ throw java.io.IOException("a") }, { throw java.io.IOException("b") }).find("01001000") }
            fail("expected an exception")
        } catch (e: java.io.IOException) {
            assertEquals("b", e.message)
        }
    }
}
