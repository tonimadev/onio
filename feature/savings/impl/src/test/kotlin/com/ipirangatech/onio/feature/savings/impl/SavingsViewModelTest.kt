package com.ipirangatech.onio.feature.savings.impl

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.OrderStatus
import com.ipirangatech.onio.core.testing.FakeOrderRepository
import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import com.ipirangatech.onio.core.testing.order
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SavingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val orders = FakeOrderRepository(
        listOf(
            order(1, OrderStatus.CANCELLED, cents = 2_000, items = 2),
            order(2, OrderStatus.CANCELLED, cents = 500),
            order(3, OrderStatus.PROCESSING, cents = 9_999),
        ),
    )
    private val settings = FakeSettingsRepository()

    @Test fun onlyCancelledOrdersCountAsSavings() {
        val savings = SavingsViewModel(orders, settings).uiState.value.savings!!
        assertEquals(2_500L, savings.total.minorUnits)
        assertEquals(2, savings.cancelledOrders)
        assertEquals(3, savings.itemsNotBought)
    }

    @Test fun theTotalGrowsWhenAnotherOrderIsCancelled() {
        val vm = SavingsViewModel(orders, settings)
        orders.state.value = orders.state.value.map { it.copy(status = OrderStatus.CANCELLED) }
        assertEquals(12_499L, vm.uiState.value.savings!!.total.minorUnits)
    }

    @Test fun usesTheStoredNameAndAlsoChecksForOverdueOrders() {
        settings.state.value = AppSettings(userName = "Ana")
        val vm = SavingsViewModel(orders, settings)
        assertEquals("Ana", vm.uiState.value.userName)
        assertEquals(1, orders.cancelExpiredCalls)
    }

    @Test fun navigationIntentsBecomeEffects() {
        val vm = SavingsViewModel(orders, settings)
        vm.onIntent(SavingsIntent.SettingsClicked)
        assertEquals(SavingsEffect.NavigateToSettings, vm.uiState.value.effect)
        vm.onIntent(SavingsIntent.SeeOrdersClicked)
        assertEquals(SavingsEffect.NavigateToOrders, vm.uiState.value.effect)
        vm.onIntent(SavingsIntent.ConsumeEffect)
        assertNull(vm.uiState.value.effect)
    }
}
