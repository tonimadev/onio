package com.ipirangatech.onio.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ipirangatech.onio.core.model.Address
import com.ipirangatech.onio.core.model.Cep
import com.ipirangatech.onio.core.model.CepSource
import com.ipirangatech.onio.core.model.DeliveryEstimate
import com.ipirangatech.onio.core.model.DeliveryEstimator
import com.ipirangatech.onio.core.model.ShippingQuoteSource
import com.ipirangatech.onio.core.model.ShippingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shippingStore by preferencesDataStore(name = "onio_shipping")

internal fun shippingStore(context: Context): DataStore<Preferences> = context.shippingStore

/**
 * CEP lookup, the remembered address and the delivery estimate. An official [quoteSource] (Correios, behind a
 * backend) wins when it answers; otherwise the regional offline estimate is used.
 */
internal class ShippingRepositoryImpl(
    private val store: DataStore<Preferences>,
    private val cepSource: CepSource,
    private val quoteSource: ShippingQuoteSource? = null,
) : ShippingRepository {
    private val cepKey = stringPreferencesKey("cep")
    private val streetKey = stringPreferencesKey("street")
    private val districtKey = stringPreferencesKey("district")
    private val cityKey = stringPreferencesKey("city")
    private val stateKey = stringPreferencesKey("state")

    override val savedAddress: Flow<Address?> = store.data.map { prefs ->
        val cep = prefs[cepKey]
        val city = prefs[cityKey]
        val state = prefs[stateKey]
        if (cep == null || city == null || state == null) {
            null
        } else {
            Address(cep, prefs[streetKey].orEmpty(), prefs[districtKey].orEmpty(), city, state)
        }
    }

    override suspend fun lookup(cep: String): Address? {
        val digits = Cep.digits(cep)
        if (!Cep.isValid(digits)) return null
        return cepSource.find(digits)
    }

    override suspend fun estimate(address: Address): DeliveryEstimate {
        quoteSource?.let { source -> runCatching { source.quote(address) }.getOrNull()?.let { return it } }
        return DeliveryEstimator.estimate(address.state, sameCity = address.state == DeliveryEstimator.ORIGIN_STATE && address.city.equals("São Paulo", ignoreCase = true))
    }

    override suspend fun save(address: Address) {
        store.edit {
            it[cepKey] = address.cep
            it[streetKey] = address.street
            it[districtKey] = address.district
            it[cityKey] = address.city
            it[stateKey] = address.state
        }
    }
}
