package com.ipirangatech.onio.core.model

import kotlinx.coroutines.flow.Flow

/** A Brazilian postal address resolved from a CEP. */
data class Address(
    val cep: String,
    val street: String,
    val district: String,
    val city: String,
    val state: String,
) {
    /** One-line text for the shipping address field, e.g. "Praça da Sé, Sé, São Paulo - SP". */
    fun asLine(): String = listOf(street, district).filter(String::isNotBlank).joinToString(", ")
        .let { first -> listOf(first, "$city - $state").filter(String::isNotBlank).joinToString(", ") }
}

/** How long a parcel takes, in calendar days from today. [official] is true only for data from the Correios API. */
data class DeliveryEstimate(val minDays: Int, val maxDays: Int, val official: Boolean = false)

object Cep {
    fun digits(input: String): String = input.filter(Char::isDigit).take(8)
    fun isValid(input: String): Boolean = digits(input).length == 8 && digits(input).any { it != '0' }
    fun format(input: String): String = digits(input).let { if (it.length > 5) it.take(5) + "-" + it.drop(5) else it }
}

/**
 * Offline delivery estimate by region, assuming the parcel ships from São Paulo. It is an estimate and is
 * labelled as one in the UI; [ShippingQuoteSource] is where the real Correios figure plugs in.
 */
object DeliveryEstimator {
    const val ORIGIN_STATE = "SP"

    private val southeast = setOf("SP", "RJ", "MG", "ES")
    private val south = setOf("PR", "SC", "RS")
    private val midwest = setOf("DF", "GO", "MT", "MS")
    private val northeast = setOf("BA", "SE", "AL", "PE", "PB", "RN", "CE", "PI", "MA")
    private val north = setOf("AC", "AM", "AP", "PA", "RO", "RR", "TO")

    /** Extra days for the capital-less interior, applied when the CEP is not a state capital range. */
    fun estimate(destinationState: String, sameCity: Boolean = false): DeliveryEstimate {
        val uf = destinationState.trim().uppercase()
        return when {
            sameCity && uf == ORIGIN_STATE -> DeliveryEstimate(1, 2)
            uf == ORIGIN_STATE -> DeliveryEstimate(2, 4)
            uf in southeast -> DeliveryEstimate(3, 5)
            uf in south -> DeliveryEstimate(4, 7)
            uf in midwest -> DeliveryEstimate(4, 8)
            uf in northeast -> DeliveryEstimate(6, 11)
            uf in north -> DeliveryEstimate(8, 14)
            else -> DeliveryEstimate(5, 10)
        }
    }
}

/** Looks a CEP up in a public registry. Null means "no such CEP". */
fun interface CepSource {
    suspend fun find(cep: String): Address?
}

/**
 * Optional official quote (Correios Prazo API). Needs a commercial contract and a token, so it must live behind a
 * backend, never in the app. When it returns null the offline estimate is used.
 */
fun interface ShippingQuoteSource {
    suspend fun quote(destination: Address): DeliveryEstimate?
}

interface ShippingRepository {
    /** The CEP the user last used, to pre-fill checkout and tell the product page when it arrives. */
    val savedAddress: Flow<Address?>

    /** Null if the CEP does not exist; throws on network failure so the UI can offer a retry. */
    suspend fun lookup(cep: String): Address?

    suspend fun estimate(address: Address): DeliveryEstimate

    suspend fun save(address: Address)
}
