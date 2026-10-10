package com.ipirangatech.onio.feature.rewards.impl

import com.ipirangatech.onio.core.model.RewardsWallet
import com.ipirangatech.onio.core.model.WheelPrize
import com.ipirangatech.onio.core.testing.FakeCouponRepository
import com.ipirangatech.onio.core.testing.FakeOrderRepository
import com.ipirangatech.onio.core.testing.FakeRewardsRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import com.ipirangatech.onio.core.testing.order
import com.ipirangatech.onio.core.testing.walletCoupon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RewardsViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    private val rewards = FakeRewardsRepository()
    private val coupons = FakeCouponRepository(listOf(walletCoupon(1, code = "A"), walletCoupon(2, code = "B", usedAt = 5)))

    // ---- hub ----

    @Test fun hubGrantsTheDailySpinAndShowsWalletAndCoupons() {
        val vm = RewardsViewModel(rewards, coupons)
        assertEquals(1, rewards.dailyRefreshes)
        assertEquals(2, vm.uiState.value.coupons.size)
    }

    @Test fun checkingInCreditsCoinsAndOnlyWorksOncePerDay() {
        val vm = RewardsViewModel(rewards, coupons)
        vm.onIntent(RewardsIntent.CheckIn)
        assertEquals(5, vm.uiState.value.wallet.coins)
        assertEquals(5, vm.uiState.value.lastCheckIn!!.coins)
        assertTrue(vm.uiState.value.wallet.checkedInToday)
        vm.onIntent(RewardsIntent.DismissCheckIn)
        assertNull(vm.uiState.value.lastCheckIn)
        vm.onIntent(RewardsIntent.CheckIn) // already done today: nothing happens
        assertEquals(5, vm.uiState.value.wallet.coins)
        assertNull(vm.uiState.value.lastCheckIn)
    }

    @Test fun daysCollectedFollowsTheStreakCycle() {
        rewards.state.value = RewardsWallet(streak = 3, checkedInToday = true)
        assertEquals(3, RewardsViewModel(rewards, coupons).uiState.value.daysCollected)
        rewards.state.value = RewardsWallet(streak = 3, checkedInToday = false)
        assertEquals(3, RewardsViewModel(rewards, coupons).uiState.value.daysCollected)
        rewards.state.value = RewardsWallet(streak = 7, checkedInToday = false)
        assertEquals(0, RewardsViewModel(rewards, coupons).uiState.value.daysCollected) // a new cycle starts
    }

    @Test fun codesAreValidatedWithClearFeedback() {
        val vm = RewardsViewModel(rewards, coupons)
        vm.onIntent(RewardsIntent.CodeChanged("ONIO15"))
        vm.onIntent(RewardsIntent.RedeemCode)
        assertEquals(CodeMessage.ADDED, vm.uiState.value.codeMessage)
        assertEquals("", vm.uiState.value.code)
        vm.onIntent(RewardsIntent.CodeChanged("ONIO15"))
        vm.onIntent(RewardsIntent.RedeemCode)
        assertEquals(CodeMessage.ALREADY_HAVE, vm.uiState.value.codeMessage)
        vm.onIntent(RewardsIntent.CodeChanged("???"))
        vm.onIntent(RewardsIntent.RedeemCode)
        assertEquals(CodeMessage.UNKNOWN, vm.uiState.value.codeMessage)
    }

    @Test fun hubNavigationBecomesEffects() {
        val vm = RewardsViewModel(rewards, coupons)
        vm.onIntent(RewardsIntent.WheelClicked)
        assertEquals(RewardsEffect.NavigateToWheel, vm.uiState.value.effect)
        vm.onIntent(RewardsIntent.ConsumeEffect)
        vm.onIntent(RewardsIntent.BackClicked)
        assertEquals(RewardsEffect.NavigateBack, vm.uiState.value.effect)
    }

    // ---- wheel ----

    @Test fun spinningDecidesThePrizeFirstAndRevealsItWhenTheAnimationEnds() {
        rewards.state.value = RewardsWallet(spins = 2)
        rewards.nextSegment = 2 // +25 coins
        val vm = WheelViewModel(rewards)
        vm.onIntent(WheelIntent.Spin)
        val state = vm.uiState.value
        assertTrue(state.isSpinning)
        assertEquals(2, state.pending!!.segmentIndex)
        assertNull(state.result)
        assertEquals(1, state.wallet.spins)
        assertEquals(25, state.wallet.coins) // credited already, even if the app dies mid-spin

        vm.onIntent(WheelIntent.SpinFinished)
        assertFalse(vm.uiState.value.isSpinning)
        assertEquals(WheelPrize.Coins(25), vm.uiState.value.result!!.prize)
        vm.onIntent(WheelIntent.DismissResult)
        assertNull(vm.uiState.value.result)
    }

    @Test fun withoutSpinsNothingStartsAndTheButtonIsFreeAgain() {
        rewards.state.value = RewardsWallet(spins = 0)
        val vm = WheelViewModel(rewards)
        vm.onIntent(WheelIntent.Spin)
        assertFalse(vm.uiState.value.isSpinning)
        assertNull(vm.uiState.value.pending)
    }

    @Test fun aSecondTapWhileSpinningAndAFailureAreHandled() {
        rewards.state.value = RewardsWallet(spins = 3)
        val vm = WheelViewModel(rewards)
        vm.onIntent(WheelIntent.Spin)
        vm.onIntent(WheelIntent.Spin) // ignored: the first spin has not finished
        assertEquals(2, vm.uiState.value.wallet.spins)

        vm.onIntent(WheelIntent.SpinFinished)
        vm.onIntent(WheelIntent.DismissResult)
        rewards.failure = IllegalStateException("disk")
        vm.onIntent(WheelIntent.Spin)
        assertTrue(vm.uiState.value.hasError)
        assertFalse(vm.uiState.value.isSpinning)
    }

    @Test fun finishingWithoutAPendingSpinChangesNothing() {
        val vm = WheelViewModel(rewards)
        vm.onIntent(WheelIntent.SpinFinished)
        assertNull(vm.uiState.value.result)
    }

    // ---- celebration ----

    @Test fun celebrationShowsTheOrderAndTheRewardItEarned() {
        val orders = FakeOrderRepository(listOf(order(5, cents = 12_300)))
        val vm = OrderPlacedViewModel(5, 25, 1, orders)
        assertEquals("ON-5", vm.uiState.value.orderNumber)
        assertEquals(12_300L, vm.uiState.value.total!!.minorUnits)
        assertEquals(25, vm.uiState.value.coins)
        assertEquals(1, vm.uiState.value.spinsEarned)
        assertFalse(vm.uiState.value.hasError)
    }

    @Test fun celebrationActionsBecomeEffects() {
        val vm = OrderPlacedViewModel(5, 25, 1, FakeOrderRepository(listOf(order(5))))
        vm.onIntent(OrderPlacedIntent.SpinNow)
        assertEquals(OrderPlacedEffect.NavigateToWheel, vm.uiState.value.effect)
        vm.onIntent(OrderPlacedIntent.ViewOrder)
        assertEquals(OrderPlacedEffect.NavigateToOrder(5), vm.uiState.value.effect)
        vm.onIntent(OrderPlacedIntent.Continue)
        assertEquals(OrderPlacedEffect.ContinueShopping, vm.uiState.value.effect)
    }

    @Test fun anUnknownOrderIsAnError() {
        val vm = OrderPlacedViewModel(99, 0, 0, FakeOrderRepository())
        assertTrue(vm.uiState.value.hasError)
        assertNotNull(vm.uiState.value)
    }
}
