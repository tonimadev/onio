package com.ipirangatech.onio.feature.checkout.impl

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.CardBrand
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.testing.FakeCartRepository
import com.ipirangatech.onio.core.testing.FakeCouponRepository
import com.ipirangatech.onio.core.testing.FakeRewardsRepository
import com.ipirangatech.onio.core.testing.FakeShippingRepository
import com.ipirangatech.onio.core.testing.FakeOrderRepository
import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import com.ipirangatech.onio.core.testing.walletCoupon
import com.ipirangatech.onio.core.model.RewardsWallet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val cart = FakeCartRepository(listOf(CartItem(1, "Lamp", "t.png", Money.usd(2_000), 2), CartItem(2, "Mug", "m.png", Money.usd(500), 1)))
    private val orders = FakeOrderRepository()
    private val settings = FakeSettingsRepository(AppSettings(userName = "Ana", currencyOverride = "EUR"))

    private val coupons = FakeCouponRepository()
    private val rewards = FakeRewardsRepository()

    private val shipping = FakeShippingRepository()

    private fun viewModel() = CheckoutViewModel(cart, orders, settings, coupons, rewards, shipping)

    private fun CheckoutViewModel.fillValidForm() {
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.ADDRESS, "Rua A, 1"))
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.CARD_NUMBER, "4242 4242 4242 4242"))
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.EXPIRY, "12/99"))
        onIntent(CheckoutIntent.FieldChanged(CheckoutField.CVV, "123"))
    }

    @Test fun summarisesTheCartAndPrefillsTheNameTheUserChose() {
        val state = viewModel().uiState.value
        assertEquals(3, state.itemCount)
        assertEquals(4_500L, state.total.minorUnits)
        assertEquals(listOf("t.png", "m.png"), state.thumbnails)
        assertEquals("Ana", state.name)
    }

    @Test fun cardFieldsKeepDigitsOnlyAndRespectTheirMaximumLengths() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.CARD_NUMBER, "4242-4242 4242 4242 4242 99"))
        assertEquals("4242424242424242424", vm.uiState.value.cardNumber) // 22 digits typed, 19 kept
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.EXPIRY, "12/3x45"))
        assertEquals("1234", vm.uiState.value.expiry)
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.CVV, "12345"))
        assertEquals("1234", vm.uiState.value.cvv)
    }

    @Test fun submittingAnIncompleteFormMarksEveryInvalidFieldAndPlacesNothing() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.NAME, ""))
        vm.onIntent(CheckoutIntent.Submit)
        assertEquals(CheckoutField.entries.toSet(), vm.uiState.value.errors)
        assertTrue(orders.placed.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
    }

    @Test fun editingAFieldClearsItsOwnError() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.Submit)
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.ADDRESS, "Rua A"))
        assertFalse(CheckoutField.ADDRESS in vm.uiState.value.errors)
        assertTrue(CheckoutField.CARD_NUMBER in vm.uiState.value.errors)
    }

    @Test fun anExpiredCardIsRejected() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.EXPIRY, "0120"))
        vm.onIntent(CheckoutIntent.Submit)
        assertEquals(setOf(CheckoutField.EXPIRY), vm.uiState.value.errors)
    }

    @Test fun aValidPaymentTakesAMomentThenPlacesTheOrderAndEmptiesTheCart() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        assertTrue(vm.uiState.value.isProcessing)
        assertTrue(orders.placed.isEmpty())

        main.scheduler.advanceUntilIdle()

        val request = orders.placed.single()
        assertEquals("4242", request.card.last4)
        assertEquals(CardBrand.VISA, request.card.brand)
        assertEquals("EUR", request.currencyCode)
        assertEquals("Ana", request.shippingName)
        assertEquals(2, request.items.size)
        assertTrue(cart.state.value.isEmpty())
        assertFalse(vm.uiState.value.isProcessing)
        assertTrue(vm.uiState.value.effect is CheckoutEffect.OrderPlaced)
    }

    @Test fun doubleTappingPayDoesNotPlaceTwoOrders() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        vm.onIntent(CheckoutIntent.Submit)
        main.scheduler.advanceUntilIdle()
        assertEquals(1, orders.placed.size)
    }

    @Test fun aFailedOrderReportsAnErrorAndKeepsTheCart() {
        orders.failure = RuntimeException("disk full")
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        main.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.hasError)
        assertFalse(vm.uiState.value.isProcessing)
        assertNull(vm.uiState.value.effect)
        assertEquals(2, cart.state.value.size)
    }

    @Test fun backBecomesAnEffect() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.BackClicked)
        assertEquals(CheckoutEffect.NavigateBack, vm.uiState.value.effect)
    }

    // ---- coupons, coins and rewards ----

    @Test fun aSelectedCouponLowersTheTotalAndIsRecordedThenMarkedUsed() {
        coupons.state.value = listOf(walletCoupon(7, code = "WELCOME10", value = 10))
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.SelectCoupon(7))
        assertEquals(450L, vm.uiState.value.couponDiscount.minorUnits)
        assertEquals(4_050L, vm.uiState.value.total.minorUnits)
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        main.scheduler.advanceUntilIdle()
        val request = orders.placed.single()
        assertEquals(450L, request.discount.minorUnits)
        assertEquals("WELCOME10", request.couponCode)
        assertEquals(listOf(7L), coupons.used)
    }

    @Test fun aCouponBelowItsMinimumSpendIsDropped() {
        coupons.state.value = listOf(walletCoupon(8, minCents = 10_000))
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.SelectCoupon(8))
        assertNull(vm.uiState.value.selectedCouponId)
        assertEquals(0L, vm.uiState.value.couponDiscount.minorUnits)
    }

    @Test fun coinsPayTenCentsEachCappedAtHalfTheTotalAndAreSpentOnPurchase() {
        rewards.state.value = RewardsWallet(coins = 1_000)
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.ToggleCoins)
        // Half of $45.00 = $22.50 = 225 coins.
        assertEquals(225, vm.uiState.value.coinsUsed)
        assertEquals(2_250L, vm.uiState.value.coinDiscount.minorUnits)
        assertEquals(2_250L, vm.uiState.value.total.minorUnits)
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        main.scheduler.advanceUntilIdle()
        assertEquals(225, orders.placed.single().coinsUsed)
        assertEquals(225, rewards.spent)
    }

    @Test fun payingEarnsAFlatRewardAndTheCelebrationCarriesIt() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onIntent(CheckoutIntent.Submit)
        main.scheduler.advanceUntilIdle()
        val effect = vm.uiState.value.effect as CheckoutEffect.OrderPlaced
        assertEquals(25, effect.coinsEarned)
        assertEquals(1, effect.spinsEarned)
        assertEquals(1, rewards.purchases)
    }

    @Test fun redeemingACodeInTheSheetAddsTheCoupon() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.OpenCoupons)
        vm.onIntent(CheckoutIntent.CouponCodeChanged("welcome10"))
        vm.onIntent(CheckoutIntent.RedeemCouponCode)
        assertEquals(CouponCodeMessage.ADDED, vm.uiState.value.couponCodeMessage)
        assertEquals(1, vm.uiState.value.coupons.size)
        vm.onIntent(CheckoutIntent.CouponCodeChanged("nope"))
        vm.onIntent(CheckoutIntent.RedeemCouponCode)
        assertEquals(CouponCodeMessage.UNKNOWN, vm.uiState.value.couponCodeMessage)
    }

    // ---- postal code, country and delivery ----

    private fun CheckoutViewModel.brazil() = onIntent(CheckoutIntent.CountryChanged("BR"))

    @Test fun aBrazilianCepFillsTheAddressAndShowsADeliveryEstimate() {
        val vm = viewModel()
        vm.brazil()
        vm.onIntent(CheckoutIntent.PostalCodeChanged("01001-000"))
        val state = vm.uiState.value
        assertEquals("01001000", state.postalCode)
        assertEquals(PostalStatus.FOUND, state.postalStatus)
        assertEquals("Praça da Sé, Sé, São Paulo - SP", state.address)
        assertEquals(1, state.delivery!!.minDays) // São Paulo itself: same-city shipping
        assertEquals("01001000", shipping.state.value!!.postalCode)
    }

    @Test fun otherCountriesWaitForThePauseThenLookTheCodeUp() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.CountryChanged("US"))
        vm.onIntent(CheckoutIntent.PostalCodeChanged("90210"))
        assertEquals(PostalStatus.LOADING, vm.uiState.value.postalStatus)
        assertTrue(shipping.lookups.isEmpty()) // debounced
        main.scheduler.advanceTimeBy(500)
        main.scheduler.runCurrent()
        val state = vm.uiState.value
        assertEquals(PostalStatus.FOUND, state.postalStatus)
        assertEquals("Beverly Hills, California", state.address)
        assertEquals(7, state.delivery!!.minDays) // international estimate
        assertEquals(listOf("US:90210"), shipping.lookups)
    }

    @Test fun typingFastOnlyLooksUpTheFinalValue() {
        val vm = viewModel()
        vm.onIntent(CheckoutIntent.CountryChanged("US"))
        listOf("902", "9021", "90210").forEach {
            vm.onIntent(CheckoutIntent.PostalCodeChanged(it))
            main.scheduler.advanceTimeBy(100)
        }
        main.scheduler.advanceTimeBy(600)
        main.scheduler.runCurrent()
        assertEquals(listOf("US:90210"), shipping.lookups)
    }

    @Test fun changingTheCountryClearsTheCodeAndTheEstimate() {
        val vm = viewModel()
        vm.brazil()
        vm.onIntent(CheckoutIntent.PostalCodeChanged("01001000"))
        vm.onIntent(CheckoutIntent.CountryChanged("DE"))
        val state = vm.uiState.value
        assertEquals("DE", state.country)
        assertEquals("", state.postalCode)
        assertNull(state.delivery)
        assertEquals(PostalStatus.IDLE, state.postalStatus)
        vm.onIntent(CheckoutIntent.CountryChanged("ZZ")) // unsupported: ignored
        assertEquals("DE", vm.uiState.value.country)
    }

    @Test fun incompleteBrazilianCepsDoNotCallTheNetworkAndUnknownOnesSaySo() {
        val vm = viewModel()
        vm.brazil()
        vm.onIntent(CheckoutIntent.PostalCodeChanged("0100"))
        assertTrue(shipping.lookups.isEmpty())
        vm.onIntent(CheckoutIntent.PostalCodeChanged("99999999"))
        assertEquals(PostalStatus.NOT_FOUND, vm.uiState.value.postalStatus)
        assertNull(vm.uiState.value.delivery)
    }

    @Test fun whatTheUserTypedInTheAddressIsNeverOverwritten() {
        val vm = viewModel()
        vm.brazil()
        vm.onIntent(CheckoutIntent.FieldChanged(CheckoutField.ADDRESS, "Av. Paulista, 1000"))
        vm.onIntent(CheckoutIntent.PostalCodeChanged("01001000"))
        assertEquals("Av. Paulista, 1000", vm.uiState.value.address)
        assertEquals(PostalStatus.FOUND, vm.uiState.value.postalStatus)
    }

    @Test fun aSuggestedAddressIsReplacedWhenTheCodeChanges() {
        shipping.addresses = shipping.addresses + ("BR:20000000" to com.ipirangatech.onio.core.model.Address("BR", "20000000", "Rua B", "Centro", "Rio de Janeiro", "RJ"))
        val vm = viewModel()
        vm.brazil()
        vm.onIntent(CheckoutIntent.PostalCodeChanged("01001000"))
        vm.onIntent(CheckoutIntent.PostalCodeChanged("20000000"))
        assertEquals("Rua B, Centro, Rio de Janeiro - RJ", vm.uiState.value.address)
        assertTrue(vm.uiState.value.delivery!!.maxDays > 4)
    }

    @Test fun aNetworkFailureOffersARetry() {
        shipping.failure = java.io.IOException("offline")
        val vm = viewModel()
        vm.brazil()
        vm.onIntent(CheckoutIntent.PostalCodeChanged("01001000"))
        assertEquals(PostalStatus.ERROR, vm.uiState.value.postalStatus)
        shipping.failure = null
        vm.onIntent(CheckoutIntent.RetryPostalCode)
        assertEquals(PostalStatus.FOUND, vm.uiState.value.postalStatus)
    }

    @Test fun aSavedAddressRestoresItsCountryAndIsLookedUpAgain() {
        shipping.state.value = com.ipirangatech.onio.core.model.Address("BR", "01001000", "Praça da Sé", "Sé", "São Paulo", "SP")
        val vm = viewModel()
        assertEquals("BR", vm.uiState.value.country)
        assertEquals("01001000", vm.uiState.value.postalCode)
        assertEquals(PostalStatus.FOUND, vm.uiState.value.postalStatus)
    }
}
