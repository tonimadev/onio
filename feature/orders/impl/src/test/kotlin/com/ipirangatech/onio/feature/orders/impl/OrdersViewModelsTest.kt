package com.ipirangatech.onio.feature.orders.impl

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.OrderStatus
import com.ipirangatech.onio.core.testing.FakeOrderRepository
import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import com.ipirangatech.onio.core.testing.order
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrdersViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeOrderRepository(
        listOf(order(2, OrderStatus.PROCESSING, cents = 3_000, items = 3), order(1, OrderStatus.CANCELLED, cents = 1_000)),
    )

    @Test fun listShowsOrdersWithTheirStatusAndChecksForOverdueOnes() {
        val vm = OrdersViewModel(repo)
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(2L, 1L), state.orders.map { it.id })
        assertEquals(listOf(false, true), state.orders.map { it.isCancelled })
        assertEquals(3, state.orders.first().itemCount)
        assertEquals(1, repo.cancelExpiredCalls)
    }

    @Test fun reachingTheDeadlineAsksTheRepositoryToCancelAndAClickNavigates() {
        val vm = OrdersViewModel(repo)
        vm.onIntent(OrdersIntent.DeadlineReached)
        assertEquals(2, repo.cancelExpiredCalls)
        vm.onIntent(OrdersIntent.OrderClicked(2))
        assertEquals(OrdersEffect.NavigateToOrder(2), vm.uiState.value.effect)
        vm.onIntent(OrdersIntent.ConsumeEffect)
        assertNull(vm.uiState.value.effect)
    }

    @Test fun detailFollowsTheOrderWhenItGetsCancelled() {
        val vm = OrderDetailViewModel(2, repo, FakeSettingsRepository(AppSettings(userName = "Ana")))
        assertFalse(vm.uiState.value.order!!.isCancelled)
        assertEquals("Ana", vm.uiState.value.userName)

        repo.state.value = repo.state.value.map { if (it.id == 2L) it.copy(status = OrderStatus.CANCELLED) else it }
        assertTrue(vm.uiState.value.order!!.isCancelled)
    }

    @Test fun detailOfAnUnknownOrderIsAnError() {
        val vm = OrderDetailViewModel(99, repo, FakeSettingsRepository())
        assertTrue(vm.uiState.value.hasError)
        assertNull(vm.uiState.value.order)
    }

    @Test fun detailBackBecomesAnEffectAndDeadlineChecksAgain() {
        val vm = OrderDetailViewModel(1, repo, FakeSettingsRepository())
        vm.onIntent(OrderDetailIntent.DeadlineReached)
        assertEquals(2, repo.cancelExpiredCalls)
        vm.onIntent(OrderDetailIntent.BackClicked)
        assertEquals(OrderDetailEffect.NavigateBack, vm.uiState.value.effect)
    }
}
