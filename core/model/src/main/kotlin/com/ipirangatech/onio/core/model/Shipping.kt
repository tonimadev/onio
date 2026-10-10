package com.ipirangatech.onio.core.model

import kotlinx.coroutines.flow.Flow
import java.util.Locale

/** A postal address resolved from a country and a postal code. Street and district are only known for Brazil. */
data class Address(
    val country: String,
    val postalCode: String,
    val street: String,
    val district: String,
    val city: String,
    val state: String,
) {
    /** One-line text for the shipping address field, e.g. "Praça da Sé, Sé, São Paulo - SP" or "Berlin, Berlin". */
    fun asLine(): String {
        val first = listOf(street, district).filter(String::isNotBlank).joinToString(", ")
        val place = when {
            state.isBlank() || state == city -> city
            country == Countries.BRAZIL -> "$city - $state"
            else -> "$city, $state"
        }
        return listOf(first, place).filter(String::isNotBlank).joinToString(", ")
    }
}

/** How long a parcel takes, in calendar days from today. [official] is true only for carrier-provided data. */
data class DeliveryEstimate(val minDays: Int, val maxDays: Int, val official: Boolean = false)

data class Country(val code: String, val flag: String) {
    /** Name in the user's language, from the platform's own data (so it is translated for free). */
    fun displayName(locale: Locale = Locale.getDefault()): String = Locale("", code).getDisplayCountry(locale).ifBlank { code }
}

object Countries {
    const val BRAZIL = "BR"
    private const val FALLBACK = "US"

    /** Countries the free postal-code registry (Zippopotam.us) answers for, plus Brazil via ViaCEP/BrasilAPI. */
    private val supportedCodes = listOf(
        "AD", "AR", "AS", "AT", "AU", "BD", "BE", "BG", "BR", "CA", "CH", "CO", "CZ", "DE", "DK", "DO", "DZ", "ES",
        "FI", "FO", "FR", "GB", "GL", "GT", "GU", "HR", "HU", "IE", "IN", "IS", "IT", "JP", "LI", "LK", "LT", "LU",
        "LV", "MC", "MD", "MH", "MK", "MP", "MT", "MX", "MY", "NL", "NO", "NZ", "PH", "PK", "PL", "PR", "PT", "RE",
        "RO", "RU", "SE", "SG", "SI", "SK", "SM", "TH", "TR", "US", "UY", "VA", "VI", "ZA",
    )

    private fun flagOf(code: String): String =
        code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")

    val all: List<Country> = supportedCodes.map { Country(it, flagOf(it)) }

    fun isSupported(code: String) = supportedCodes.contains(code.uppercase())

    /** Country to preselect: the device region when supported, Brazil for Portuguese, otherwise the US. */
    fun defaultFor(locale: Locale): String = when {
        isSupported(locale.country) -> locale.country.uppercase()
        locale.language == "pt" -> BRAZIL
        else -> FALLBACK
    }

    fun byCode(code: String): Country = all.firstOrNull { it.code == code.uppercase() } ?: Country(code.uppercase(), flagOf(code))
}

object PostalCodes {
    /** Brazilian CEPs are exactly 8 digits and get a mask; every other country accepts free text. */
    fun normalize(country: String, input: String): String = when (country.uppercase()) {
        Countries.BRAZIL -> input.filter(Char::isDigit).take(8)
        else -> input.filter { it.isLetterOrDigit() || it == ' ' || it == '-' }.uppercase().take(10)
    }

    /** True once enough has been typed to try a lookup; the registry decides whether it exists. */
    fun isLookupReady(country: String, postalCode: String): Boolean = when (country.uppercase()) {
        Countries.BRAZIL -> postalCode.length == 8 && postalCode.any { it != '0' }
        else -> lookupKey(country, postalCode).length >= 3
    }

    /** What the registry wants: Canada and Ireland use the first 3 characters, the UK the outward code, etc. */
    fun lookupKey(country: String, postalCode: String): String {
        val text = postalCode.trim().uppercase()
        val compact = text.filter { it.isLetterOrDigit() }
        return when (country.uppercase()) {
            "CA", "IE" -> compact.take(3)
            "GB" -> text.substringBefore(' ').ifBlank { compact }.let { if (' ' !in text && compact.length > 4) compact.dropLast(3) else it }
            "NL" -> compact.take(4)
            "JP" -> compact.let { if (it.length > 3) it.take(3) + "-" + it.drop(3).take(4) else it }
            "PL" -> compact.let { if (it.length > 2) it.take(2) + "-" + it.drop(2).take(3) else it }
            "PT" -> compact.let { if (it.length > 4) it.take(4) + "-" + it.drop(4).take(3) else it }
            "SE" -> compact.take(5)
            else -> text.replace(' ', '-').ifBlank { compact }
        }
    }

    /** Display form for the text field (only Brazil is masked). */
    fun format(country: String, postalCode: String): String =
        if (country.equals(Countries.BRAZIL, true) && postalCode.length > 5) postalCode.take(5) + "-" + postalCode.drop(5) else postalCode
}

/**
 * Offline delivery estimate. Brazil is estimated by state (parcels ship from São Paulo); the rest of the world by
 * region as international standard shipping. It is an estimate and is labelled as one in the UI;
 * [ShippingQuoteSource] is where a real carrier figure plugs in.
 */
object DeliveryEstimator {
    const val ORIGIN_STATE = "SP"

    private val southeast = setOf("SP", "RJ", "MG", "ES")
    private val south = setOf("PR", "SC", "RS")
    private val midwest = setOf("DF", "GO", "MT", "MS")
    private val northeast = setOf("BA", "SE", "AL", "PE", "PB", "RN", "CE", "PI", "MA")
    private val north = setOf("AC", "AM", "AP", "PA", "RO", "RR", "TO")

    private val latinAmerica = setOf("AR", "CO", "UY", "MX", "GT", "DO", "PR", "CR")
    private val northAmerica = setOf("US", "CA", "GU", "VI", "MP", "AS", "MH", "GL")
    private val europe = setOf(
        "AD", "AT", "BE", "BG", "CH", "CZ", "DE", "DK", "ES", "FI", "FO", "FR", "GB", "HR", "HU", "IE", "IS", "IT",
        "LI", "LT", "LU", "LV", "MC", "MD", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI", "SK", "SM", "VA", "RE",
    )
    private val asia = setOf("BD", "IN", "JP", "LK", "MY", "PH", "PK", "SG", "TH", "TR", "RU")
    private val oceania = setOf("AU", "NZ")
    private val africa = setOf("DZ", "ZA")

    fun estimate(address: Address): DeliveryEstimate =
        if (address.country.equals(Countries.BRAZIL, true)) {
            estimateBrazil(address.state, sameCity = address.state == ORIGIN_STATE && address.city.equals("São Paulo", true))
        } else {
            estimateInternational(address.country)
        }

    fun estimateInternational(country: String): DeliveryEstimate {
        val code = country.uppercase()
        return when (code) {
            in latinAmerica -> DeliveryEstimate(7, 14)
            in northAmerica -> DeliveryEstimate(7, 12)
            in europe -> DeliveryEstimate(8, 14)
            in asia -> DeliveryEstimate(9, 16)
            in oceania -> DeliveryEstimate(10, 18)
            in africa -> DeliveryEstimate(10, 18)
            else -> DeliveryEstimate(9, 18)
        }
    }

    fun estimateBrazil(destinationState: String, sameCity: Boolean = false): DeliveryEstimate {
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

/** Looks a postal code up in a public registry. Null means "no such code". */
fun interface PostalCodeSource {
    suspend fun find(country: String, postalCode: String): Address?
}

/**
 * Optional carrier quote (Correios, DHL, EasyPost...). It needs credentials, so it must live behind a backend, never
 * in the app. When it returns null the offline estimate is used.
 */
fun interface ShippingQuoteSource {
    suspend fun quote(destination: Address): DeliveryEstimate?
}

interface ShippingRepository {
    /** The last address the user used, to pre-fill checkout and tell the product page when it arrives. */
    val savedAddress: Flow<Address?>

    /** Null if the postal code does not exist; throws on network failure so the UI can offer a retry. */
    suspend fun lookup(country: String, postalCode: String): Address?

    suspend fun estimate(address: Address): DeliveryEstimate

    suspend fun save(address: Address)
}
