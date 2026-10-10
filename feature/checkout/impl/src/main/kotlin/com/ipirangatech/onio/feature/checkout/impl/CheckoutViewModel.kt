package com.ipirangatech.onio.feature.checkout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CardValidation
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.CheckoutRequest
import com.ipirangatech.onio.core.model.ExchangeRates
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.PaymentCard
import com.ipirangatech.onio.core.model.SettingsRepository
import com.ipirangatech.onio.core.model.sum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cart: CartRepository,
    private val orders: OrderRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckoutState())
    val uiState: StateFlow<CheckoutState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    private var items: List<CartItem> = emptyList()

    init {
        viewModelScope.launch {
            runCatching { cart.items.first() }
                .onSuccess { loaded ->
                    items = loaded
                    _uiState.update {
                        it.copy(
                            total = loaded.map(CartItem::lineTotal).sum(),
                            itemCount = loaded.sumOf(CartItem::quantity),
                            thumbnails = loaded.map(CartItem::thumbnailUrl),
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(hasError = true) } }
        }
        // Pre-fill the shipping name with how the user asked to be called.
        viewModelScope.launch {
            runCatching { settings.settings.first().userName }.getOrNull()?.let { name ->
                _uiState.update { if (it.name.isBlank()) it.copy(name = name) else it }
            }
        }
    }

    fun onIntent(intent: CheckoutIntent) {
        when (intent) {
            is CheckoutIntent.FieldChanged -> onField(intent.field, intent.value)
            CheckoutIntent.Submit -> submit()
            CheckoutIntent.BackClicked -> _uiState.update { it.copy(effect = CheckoutEffect.NavigateBack) }
            CheckoutIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun onField(field: CheckoutField, raw: String) {
        _uiState.update { s ->
            val cleared = s.errors - field
            when (field) {
                CheckoutField.NAME -> s.copy(name = raw, errors = cleared)
                CheckoutField.ADDRESS -> s.copy(address = raw, errors = cleared)
                CheckoutField.CARD_NUMBER -> s.copy(cardNumber = formatCard(raw), errors = cleared)
                CheckoutField.EXPIRY -> s.copy(expiry = formatExpiry(raw), errors = cleared)
                CheckoutField.CVV -> s.copy(cvv = CardValidation.digitsOnly(raw).take(4), errors = cleared)
            }
        }
    }

    private fun submit() {
        val s = _uiState.value
        if (s.isProcessing || items.isEmpty()) return
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isProcessing = true, hasError = false) }
        viewModelScope.launch {
            runCatching {
                // Looks like a real bank round-trip.
                delay(PROCESSING_MILLIS)
                val digits = CardValidation.digitsOnly(s.cardNumber)
                val currency = settings.settings.first().currencyOverride
                    ?: Locale.getDefault().let { ExchangeRates.defaultFor(it.language, it.country) }
                val order = orders.placeOrder(
                    CheckoutRequest(
                        items = items,
                        card = PaymentCard(CardValidation.brandOf(digits), digits.takeLast(4)),
                        shippingName = s.name.trim(),
                        shippingAddress = s.address.trim(),
                        currencyCode = currency,
                    ),
                )
                cart.clear()
                order
            }.onSuccess { order ->
                _uiState.update { it.copy(isProcessing = false, effect = CheckoutEffect.OrderPlaced(order.id)) }
            }.onFailure {
                _uiState.update { it.copy(isProcessing = false, hasError = true) }
            }
        }
    }

    private fun validate(s: CheckoutState): Set<CheckoutField> = buildSet {
        if (s.name.isBlank()) add(CheckoutField.NAME)
        if (s.address.isBlank()) add(CheckoutField.ADDRESS)
        if (!CardValidation.passesLuhn(s.cardNumber)) add(CheckoutField.CARD_NUMBER)
        val parts = s.expiry.split("/")
        val now = Calendar.getInstance()
        val month = parts.getOrNull(0)?.toIntOrNull()
        val year = parts.getOrNull(1)?.toIntOrNull()
        if (month == null || year == null || !CardValidation.isValidExpiry(month, year, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)) {
            add(CheckoutField.EXPIRY)
        }
        if (!CardValidation.isValidCvv(s.cvv, CardValidation.brandOf(s.cardNumber))) add(CheckoutField.CVV)
    }

    private fun formatCard(raw: String): String =
        CardValidation.digitsOnly(raw).take(19).chunked(4).joinToString(" ")

    private fun formatExpiry(raw: String): String {
        val d = CardValidation.digitsOnly(raw).take(4)
        return if (d.length <= 2) d else d.take(2) + "/" + d.drop(2)
    }

    private companion object {
        const val PROCESSING_MILLIS = 2_500L
    }
}
