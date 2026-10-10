package com.ipirangatech.onio.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShippingTest {
    @Test fun cepNormalisationAndValidation() {
        assertEquals("01001000", Cep.digits("01001-000 abc"))
        assertEquals("01001-000", Cep.format("01001000"))
        assertEquals("0100", Cep.format("0100"))
        assertTrue(Cep.isValid("01001-000"))
        assertFalse(Cep.isValid("0100100"))
        assertFalse(Cep.isValid("00000000"))
    }

    @Test fun nearerRegionsArriveSoonerThanFarOnes() {
        val same = DeliveryEstimator.estimate("SP", sameCity = true)
        val sp = DeliveryEstimator.estimate("SP")
        val rj = DeliveryEstimator.estimate("rj")
        val ba = DeliveryEstimator.estimate("BA")
        val am = DeliveryEstimator.estimate("AM")
        assertTrue(same.maxDays < sp.maxDays && sp.maxDays <= rj.maxDays)
        assertTrue(rj.maxDays < ba.maxDays && ba.maxDays < am.maxDays)
        listOf(same, sp, rj, ba, am).forEach {
            assertTrue(it.minDays in 1..it.maxDays)
            assertFalse(it.official)
        }
        assertEquals(DeliveryEstimator.estimate("ZZ"), DeliveryEstimator.estimate("??"))
    }

    @Test fun theAddressLineSkipsEmptyParts() {
        assertEquals("São Paulo - SP", Address("01000000", "", "", "São Paulo", "SP").asLine())
        assertEquals("Rua A, Santos - SP", Address("11000000", "Rua A", "", "Santos", "SP").asLine())
    }
}
