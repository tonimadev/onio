package com.ipirangatech.onio.core.network

import com.ipirangatech.onio.core.network.cep.BrasilApiApi
import com.ipirangatech.onio.core.network.cep.BrasilApiDto
import com.ipirangatech.onio.core.network.cep.BrazilCepSource
import com.ipirangatech.onio.core.network.cep.RoutingPostalCodeSource
import com.ipirangatech.onio.core.network.cep.WorldPostalSource
import com.ipirangatech.onio.core.network.cep.ZippoApi
import com.ipirangatech.onio.core.network.cep.ZippoDto
import com.ipirangatech.onio.core.network.cep.ZippoPlaceDto
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
    private fun source(
        via: suspend (String) -> ViaCepDto,
        brasil: suspend (String) -> BrasilApiDto = { error("not called") },
        zippo: suspend (String, String) -> ZippoDto = { _, _ -> error("not called") },
    ) = RoutingPostalCodeSource(
        BrazilCepSource(
            object : ViaCepApi { override suspend fun find(cep: String) = via(cep) },
            object : BrasilApiApi { override suspend fun find(cep: String) = brasil(cep) },
        ),
        WorldPostalSource(object : ZippoApi { override suspend fun find(country: String, code: String) = zippo(country, code) }),
    )

    @Test fun parsesViaCepAndMapsToTheDomain() = runBlocking {
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<ViaCepDto>(
            """{"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé","localidade":"São Paulo","uf":"SP","ibge":"3550308"}""",
        )
        val address = source({ dto }).find("BR", "01001000")!!
        assertEquals("São Paulo", address.city)
        assertEquals("SP", address.state)
        assertEquals("Praça da Sé, Sé, São Paulo - SP", address.asLine())
    }

    @Test fun anUnknownCepIsNullEvenWhenErroIsABooleanOrAString() = runBlocking {
        val asBoolean = Json { ignoreUnknownKeys = true }.decodeFromString<ViaCepDto>("""{"erro":true}""")
        val asString = ViaCepDto(erro = JsonPrimitive("true"))
        assertNull(source({ asBoolean }).find("BR", "99999999"))
        assertNull(source({ asString }).find("BR", "99999999"))
    }

    @Test fun fallsBackToBrasilApiWhenViaCepFails() = runBlocking {
        val result = source(
            via = { throw java.io.IOException("down") },
            brasil = { BrasilApiDto(state = "RJ", city = "Rio de Janeiro", neighborhood = "Centro", street = "Rua A") },
        ).find("BR", "20000000")!!
        assertEquals("RJ", result.state)
    }

    @Test fun aBrasilApi404MeansTheCepDoesNotExist() = runBlocking {
        val notFound = HttpException(Response.error<BrasilApiDto>(404, "{}".toResponseBody()))
        assertNull(source({ throw java.io.IOException("down") }, { throw notFound }).find("BR", "00000001"))
    }

    @Test fun bothServicesDownSurfacesTheFailure() {
        try {
            runBlocking { source({ throw java.io.IOException("a") }, { throw java.io.IOException("b") }).find("BR", "01001000") }
            fail("expected an exception")
        } catch (e: java.io.IOException) {
            assertEquals("b", e.message)
        }
    }

    @Test fun otherCountriesGoToZippopotamWithTheRightKey() = runBlocking {
        val calls = mutableListOf<Pair<String, String>>()
        val result = source(via = { error("brazil only") }, zippo = { country, code ->
            calls += country to code
            ZippoDto(postCode = "SW1A", places = listOf(ZippoPlaceDto("Westminster Abbey", "England", "ENG")))
        }).find("GB", "SW1A 1AA")!!
        assertEquals(listOf("gb" to "SW1A"), calls)
        assertEquals("GB", result.country)
        assertEquals("Westminster Abbey, England", result.asLine())
    }

    @Test fun zippopotamParsingHandlesSpacesInFieldNamesAndAMissingStateName() = runBlocking {
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<ZippoDto>(
            """{"post code":"100-0001","country":"Japan","places":[{"place name":"Chiyoda","state":"","state abbreviation":"13"}]}""",
        )
        val a = source(via = { error("x") }, zippo = { _, _ -> dto }).find("JP", "1000001")!!
        assertEquals("Chiyoda", a.city)
        assertEquals("13", a.state)
    }

    @Test fun aWorldwide404MeansTheCodeDoesNotExistButOtherFailuresSurface() {
        val notFound = HttpException(Response.error<ZippoDto>(404, "{}".toResponseBody()))
        assertNull(runBlocking { source(via = { error("x") }, zippo = { _, _ -> throw notFound }).find("US", "00000") })
        try {
            runBlocking { source(via = { error("x") }, zippo = { _, _ -> throw java.io.IOException("offline") }).find("US", "90210") }
            fail("expected an exception")
        } catch (e: java.io.IOException) {
            assertEquals("offline", e.message)
        }
    }
}
