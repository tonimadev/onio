package com.ipirangatech.onio.feature.checkout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.CardValidation
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.CheckoutRequest
import com.ipirangatech.onio.core.model.CouponRepository
import com.ipirangatech.onio.core.model.ExchangeRates
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.PaymentCard
import com.ipirangatech.onio.core.model.PricingCalculator
import com.ipirangatech.onio.core.model.PurchaseReward
import com.ipirangatech.onio.core.model.RedeemResult
import com.ipirangatech.onio.core.model.RewardsRepository
import com.ipirangatech.onio.core.model.ShippingRepository
import com.ipirangatech.onio.core.model.Cep
import kotlinx.coroutines.Job
import com.ipirangatech.onio.core.model.SettingsRepository
import com.ipirangatech.onio.core.model.WalletCoupon
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
    private val couponRepository: CouponRepository,
    private val rewards: RewardsRepository,
    private val shipping: ShippingRepository,
) : ViewModel() {
    /** Time source for coupon expiry; tests replace it. */
    internal var clock: () -> Long = System::currentTimeMillis

    private val _uiState = MutableStateFlow(CheckoutState())
    val uiState: StateFlow<CheckoutState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    private var items: List<CartItem> = emptyList()
    private var cepJob: Job? = null
    /** The address line we wrote ourselves, so a later CEP change can replace it without touching what the user typed. */
    private var autoFilledLine: String? = null
    private var allCoupons: List<WalletCoupon> = emptyList()

    init {
        viewModelScope.launch {
            runCatching { cart.items.first() }
                .onSuccess { loaded ->
                    items = loaded
                    _uiState.update { it.copy(itemCount = loaded.sumOf(CartItem::quantity), thumbnails = loaded.map(CartItem::thumbnailUrl)) }
                    recompute()
                }
                .onFailure { _uiState.update { it.copy(hasError = true) } }
        }
        // Pre-fill the shipping name with how the user asked to be called.
        viewModelScope.launch {
            runCatching { settings.settings.first().userName }.getOrNull()?.let { name ->
                _uiState.update { if (it.name.isBlank()) it.copy(name = name) else it }
            }
        }
        // Remember the last CEP so returning customers do not retype it.
        viewModelScope.launch {
            runCatching { shipping.savedAddress.first() }.getOrNull()?.let { saved ->
                if (_uiState.value.cep.isEmpty()) onIntent(CheckoutIntent.CepChanged(saved.cep))
            }
        }
        viewModelScope.launch {
            runCatching {
                couponRepository.coupons.collect { list ->
                    allCoupons = list
                    recompute()
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                rewards.wallet.collect { wallet ->
                    _uiState.update { it.copy(coinBalance = wallet.coins) }
                    recompute()
                }
            }
        }
    }

    fun onIntent(intent: CheckoutIntent) {
        when (intent) {
            is CheckoutIntent.FieldChanged -> onField(intent.field, intent.value)
            CheckoutIntent.Submit -> submit()
            is CheckoutIntent.CepChanged -> onCep(intent.raw)
            CheckoutIntent.RetryCep -> lookupCep(_uiState.value.cep)
            CheckoutIntent.OpenCoupons -> _uiState.update { it.copy(couponSheetOpen = true, couponCodeMessage = null) }
            CheckoutIntent.CloseCoupons -> _uiState.update { it.copy(couponSheetOpen = false) }
            is CheckoutIntent.SelectCoupon -> {
                _uiState.update { it.copy(selectedCouponId = intent.couponId, couponSheetOpen = false) }
                recompute()
            }
            CheckoutIntent.ToggleCoins -> {
                _uiState.update { it.copy(useCoins = !it.useCoins) }
                recompute()
            }
            is CheckoutIntent.CouponCodeChanged -> _uiState.update { it.copy(couponCode = intent.code.take(MAX_CODE), couponCodeMessage = null) }
            CheckoutIntent.RedeemCouponCode -> redeemCode()
            CheckoutIntent.BackClicked -> _uiState.update { it.copy(effect = CheckoutEffect.NavigateBack) }
            CheckoutIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    /** Re-prices the cart from the selected coupon, coin toggle and wallet balance. */
    private fun recompute() {
        val subtotal = items.map(CartItem::lineTotal).sum()
        _uiState.update { s ->
            val now = clock()
            val usable = allCoupons.filter { it.isUsable(now) }
            // A coupon that stops being eligible (cart changed, expired) is dropped instead of silently kept.
            val selected = usable.firstOrNull { it.id == s.selectedCouponId && it.isEligible(subtotal, now) }
            val maxCoins = PricingCalculator.maxCoins(subtotal, selected, s.coinBalance)
            val breakdown = PricingCalculator.calculate(subtotal, selected, if (s.useCoins) maxCoins else 0, s.coinBalance)
            s.copy(
                subtotal = subtotal,
                total = breakdown.total,
                couponDiscount = breakdown.couponDiscount,
                coinDiscount = breakdown.coinDiscount,
                coinsUsed = breakdown.coinsUsed,
                maxCoins = maxCoins,
                coupons = usable,
                selectedCouponId = selected?.id,
            )
        }
    }

    private fun redeemCode() {
        val code = _uiState.value.couponCode
        if (code.isBlank()) return
        viewModelScope.launch {
            runCatching { couponRepository.redeem(code) }
                .onSuccess { result ->
                    val message = when (result) {
                        is RedeemResult.Added -> CouponCodeMessage.ADDED
                        RedeemResult.Unknown -> CouponCodeMessage.UNKNOWN
                        RedeemResult.AlreadyHave -> CouponCodeMessage.ALREADY_HAVE
                    }
                    _uiState.update { it.copy(couponCodeMessage = message, couponCode = if (result is RedeemResult.Added) "" else it.couponCode) }
                }
                .onFailure { _uiState.update { it.copy(hasError = true) } }
        }
    }

    private fun onCep(raw: String) {
        val digits = Cep.digits(raw)
        cepJob?.cancel()
        _uiState.update { it.copy(cep = digits, cepStatus = CepStatus.IDLE, resolvedAddress = null, delivery = null) }
        if (digits.length == 8) lookupCep(digits)
    }

    private fun lookupCep(digits: String) {
        if (digits.length != 8) return
        cepJob?.cancel()
        _uiState.update { it.copy(cepStatus = CepStatus.LOADING) }
        cepJob = viewModelScope.launch {
            runCatching { shipping.lookup(digits) }
                .onSuccess { address ->
                    if (address == null) {
                        _uiState.update { it.copy(cepStatus = CepStatus.NOT_FOUND, resolvedAddress = null, delivery = null) }
                        return@onSuccess
                    }
                    val estimate = runCatching { shipping.estimate(address) }.getOrNull()
                    runCatching { shipping.save(address) }
                    val line = address.asLine()
                    _uiState.update { s ->
                        // Only overwrite the address field if it is empty or still holds our previous suggestion.
                        val replace = s.address.isBlank() || s.address == autoFilledLine
                        if (replace) autoFilledLine = line
                        s.copy(
                            cepStatus = CepStatus.FOUND,
                            resolvedAddress = address,
                            delivery = estimate,
                            address = if (replace) line else s.address,
                            errors = if (replace) s.errors - CheckoutField.ADDRESS else s.errors,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    _uiState.update { it.copy(cepStatus = CepStatus.ERROR) }
                }
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
                val coupon = s.coupons.firstOrNull { it.id == s.selectedCouponId }
                val order = orders.placeOrder(
                    CheckoutRequest(
                        items = items,
                        card = PaymentCard(CardValidation.brandOf(digits), digits.takeLast(4)),
                        shippingName = s.name.trim(),
                        shippingAddress = s.address.trim(),
                        currencyCode = currency,
                        discount = Money(s.couponDiscount.minorUnits + s.coinDiscount.minorUnits),
                        couponCode = coupon?.code,
                        coinsUsed = s.coinsUsed,
                    ),
                )
                cart.clear()
                // The order exists now; bookkeeping around it must not turn a successful payment into an error.
                runCatching { coupon?.let { couponRepository.markUsed(it.id) } }
                runCatching { if (s.coinsUsed > 0) rewards.spendCoins(s.coinsUsed) }
                val reward = runCatching { rewards.awardPurchase() }.getOrDefault(PurchaseReward(0, 0))
                Triple(order.id, reward.coins, reward.spins)
            }.onSuccess { (id, coins, spins) ->
                _uiState.update { it.copy(isProcessing = false, effect = CheckoutEffect.OrderPlaced(id, coins, spins)) }
            }.onFailure {
                _uiState.update { it.copy(isProcessing = false, hasError = true) }
            }
        }
    }

    private fun validate(s: CheckoutState): Set<CheckoutField> = buildSet {
        if (s.name.isBlank()) add(CheckoutField.NAME)
        if (s.address.isBlank()) add(CheckoutField.ADDRESS)
        if (!CardValidation.passesLuhn(s.cardNumber)) add(CheckoutField.CARD_NUMBER)
        val now = Calendar.getInstance()
        val month = s.expiry.take(2).takeIf { it.length == 2 }?.toIntOrNull()
        val year = s.expiry.drop(2).takeIf { it.length == 2 }?.toIntOrNull()
        if (month == null || year == null || !CardValidation.isValidExpiry(month, year, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)) {
            add(CheckoutField.EXPIRY)
        }
        if (!CardValidation.isValidCvv(s.cvv, CardValidation.brandOf(s.cardNumber))) add(CheckoutField.CVV)
    }

    // The state keeps digits only; the UI inserts spaces and the slash with a VisualTransformation, so the
    // cursor never ends up in the wrong place while typing.
    private fun formatCard(raw: String): String = CardValidation.digitsOnly(raw).take(19)

    private fun formatExpiry(raw: String): String = CardValidation.digitsOnly(raw).take(4)

    private companion object {
        const val PROCESSING_MILLIS = 2_500L
        const val MAX_CODE = 24
    }
}
