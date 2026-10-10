package com.ipirangatech.onio.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ShippingTest {
    private fun address(country: String, state: String = "", city: String = "X") = Address(country, "1", "", "", city, state)

    @Test fun brazilKeepsDigitsOnlyAndIsReadyAtEightDigits() {
        assertEquals("01001000", PostalCodes.normalize("BR", "01001-000 abc"))
        assertEquals("01001-000", PostalCodes.format("BR", "01001000"))
        assertEquals("0100", PostalCodes.format("BR", "0100"))
        assertTrue(PostalCodes.isLookupReady("BR", "01001000"))
        assertFalse(PostalCodes.isLookupReady("BR", "0100100"))
        assertFalse(PostalCodes.isLookupReady("BR", "00000000"))
    }

    @Test fun otherCountriesAcceptLettersAndSpacesInUpperCase() {
        assertEquals("SW1A 1AA", PostalCodes.normalize("GB", "sw1a 1aa"))
        assertEquals("M5V 3L9", PostalCodes.normalize("CA", "m5v 3l9"))
        assertEquals("90210-1234", PostalCodes.normalize("US", "90210-1234"))
    }

    @Test fun lookupKeysMatchWhatTheRegistryExpects() {
        assertEquals("SW1A", PostalCodes.lookupKey("GB", "SW1A 1AA"))
        assertEquals("SW1A", PostalCodes.lookupKey("GB", "SW1A1AA"))
        assertEquals("M5V", PostalCodes.lookupKey("CA", "M5V 3L9"))
        assertEquals("1011", PostalCodes.lookupKey("NL", "1011 AB"))
        assertEquals("100-0001", PostalCodes.lookupKey("JP", "1000001"))
        assertEquals("00-001", PostalCodes.lookupKey("PL", "00001"))
        assertEquals("90210", PostalCodes.lookupKey("US", "90210"))
    }

    @Test fun aLookupNeedsAtLeastThreeCharacters() {
        assertFalse(PostalCodes.isLookupReady("US", "90"))
        assertTrue(PostalCodes.isLookupReady("US", "902"))
    }

    @Test fun theDefaultCountryComesFromTheDeviceThenTheLanguage() {
        assertEquals("DE", Countries.defaultFor(Locale("de", "DE")))
        assertEquals("BR", Countries.defaultFor(Locale("pt", "BR")))
        assertEquals("BR", Countries.defaultFor(Locale("pt", "AO"))) // Portuguese but unsupported region
        assertEquals("US", Countries.defaultFor(Locale("en", "ZZ")))
    }

    @Test fun countriesHaveFlagsAndLocalisedNames() {
        val br = Countries.byCode("br")
        assertEquals("🇧🇷", br.flag)
        assertEquals("Brazil", br.displayName(Locale.ENGLISH))
        assertEquals("Brasil", br.displayName(Locale("pt", "BR")))
        assertTrue(Countries.all.size > 60 && Countries.all.any { it.code == "JP" })
    }

    @Test fun brazilIsEstimatedByStateAndTheWorldByRegion() {
        val sp = DeliveryEstimator.estimate(address("BR", "SP", "São Paulo"))
        val am = DeliveryEstimator.estimate(address("BR", "AM"))
        assertTrue(sp.maxDays < am.maxDays)
        val us = DeliveryEstimator.estimate(address("US", "CA"))
        val de = DeliveryEstimator.estimate(address("DE"))
        val au = DeliveryEstimator.estimate(address("AU"))
        assertTrue(us.maxDays < au.maxDays && de.maxDays < au.maxDays)
        listOf(sp, am, us, de, au).forEach {
            assertTrue(it.minDays in 1..it.maxDays)
            assertFalse(it.official)
        }
        assertEquals(DeliveryEstimator.estimate(address("ZZ")), DeliveryEstimator.estimate(address("QQ")))
    }

    @Test fun theAddressLineAdaptsToTheCountry() {
        assertEquals("Praça da Sé, Sé, São Paulo - SP", Address("BR", "01001000", "Praça da Sé", "Sé", "São Paulo", "SP").asLine())
        assertEquals("Beverly Hills, California", Address("US", "90210", "", "", "Beverly Hills", "California").asLine())
        assertEquals("Berlin", Address("DE", "10115", "", "", "Berlin", "Berlin").asLine())
        assertEquals("Tokyo", Address("JP", "100-0001", "", "", "Tokyo", "").asLine())
    }
}
