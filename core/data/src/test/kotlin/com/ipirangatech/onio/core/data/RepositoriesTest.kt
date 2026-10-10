package com.ipirangatech.onio.core.data

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.CardBrand
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.CheckoutRequest
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Order
import com.ipirangatech.onio.core.model.OrderStatus
import com.ipirangatech.onio.core.model.PaymentCard
import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import com.ipirangatech.onio.core.testing.product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryTest {
    private val dao = FakeOrderDao()
    private val settings = FakeSettingsRepository(AppSettings(cancelAfterMinutes = 15))
    private val scheduled = mutableListOf<Pair<Long, Long>>()
    private val notified = mutableListOf<Order>()
    private var now = 1_000_000L
    private var notifierFails = false

    private fun repo() = OrderRepositoryImpl(
        dao = dao,
        settings = settings,
        scheduler = { id, delay -> scheduled += id to delay },
        notifier = { order -> if (notifierFails) error("no permission") else notified += order },
        clock = { now },
    )

    private fun request(currency: String = "USD") = CheckoutRequest(
        items = listOf(CartItem(1, "Lamp", "l.png", Money.usd(2_000), 2), CartItem(2, "Mug", "m.png", Money.usd(500), 1)),
        card = PaymentCard(CardBrand.VISA, "4242"),
        shippingName = "Ana",
        shippingAddress = "Rua A",
        currencyCode = currency,
    )

    @Test fun placingAnOrderUsesTheConfiguredDelayAndSchedulesTheCancellation() = runTest {
        val order = repo().placeOrder(request())
        assertEquals(OrderStatus.PROCESSING, order.status)
        assertEquals(now + 15 * 60_000L, order.cancelAtMillis)
        assertEquals(listOf(order.id to 15 * 60_000L), scheduled)
        assertEquals(4_500L, order.totalUsd.minorUnits)
    }

    @Test fun theChargedAmountIsFrozenInTheCurrencyTheUserSaw() = runTest {
        val order = repo().placeOrder(request(currency = "BRL"))
        assertEquals("BRL", order.charged.currencyCode)
        assertEquals(22_500L, order.charged.minorUnits) // 45.00 USD * 5.0
        assertEquals("USD", order.totalUsd.currencyCode)
    }

    @Test fun emptyCartsAreRejected() = runTest {
        val failure = runCatching { repo().placeOrder(request().copy(items = emptyList())) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test fun onlyOverdueProcessingOrdersAreCancelledAndEachIsAnnouncedOnce() = runTest {
        val repo = repo()
        val early = repo.placeOrder(request())
        now += 20 * 60_000L // past the first deadline
        val late = repo.placeOrder(request()) // new deadline is in the future

        repo.cancelExpiredOrders()
        repo.cancelExpiredOrders() // idempotent: nothing new to cancel the second time

        assertEquals(listOf(early.id), notified.map { it.id })
        val statuses = repo.orders.first().associate { it.id to it.status }
        assertEquals(OrderStatus.CANCELLED, statuses[early.id])
        assertEquals(OrderStatus.PROCESSING, statuses[late.id])
    }

    @Test fun aFailingNotificationNeverBlocksTheCancellation() = runTest {
        notifierFails = true
        val repo = repo()
        val order = repo.placeOrder(request())
        now += 16 * 60_000L
        repo.cancelExpiredOrders()
        assertEquals(OrderStatus.CANCELLED, repo.order(order.id).first()!!.status)
    }

    @Test fun savingsAddUpCancelledOrdersOnly() = runTest {
        val repo = repo()
        repo.placeOrder(request())
        repo.placeOrder(request())
        now += 16 * 60_000L
        repo.cancelExpiredOrders()
        repo.placeOrder(request()) // still processing
        val savings = repo.savings.first()
        assertEquals(9_000L, savings.total.minorUnits)
        assertEquals(2, savings.cancelledOrders)
        assertEquals(6, savings.itemsNotBought)
    }
}

class CartRepositoryTest {
    private val dao = FakeCartDao()
    private val repo = CartRepositoryImpl(dao)

    @Test fun addingTheSameProductAgainIncreasesTheQuantity() = runTest {
        repo.add(product(1, cents = 700))
        repo.add(product(1, cents = 700), quantity = 2)
        val item = repo.items.first().single()
        assertEquals(3, item.quantity)
        assertEquals(2_100L, item.lineTotal.minorUnits)
    }

    @Test fun quantityIsCappedAndZeroRemovesTheLine() = runTest {
        repo.add(product(1), quantity = 98)
        repo.add(product(1), quantity = 50)
        assertEquals(99, repo.items.first().single().quantity)
        repo.setQuantity(1, 0)
        assertTrue(repo.items.first().isEmpty())
    }

    @Test fun clearEmptiesTheCart() = runTest {
        repo.add(product(1))
        repo.add(product(2))
        repo.clear()
        assertTrue(repo.items.first().isEmpty())
    }
}

class FavoritesRepositoryTest {
    private val dao = FakeFavoriteDao()
    private var now = 0L
    private val repo = FavoritesRepositoryImpl(dao) { now }

    @Test fun togglingSavesASnapshotAndTogglingAgainRemovesIt() = runTest {
        repo.toggle(product(5, cents = 1_234, discount = 10.0, rating = 4.5, title = "Vase"))
        val saved = repo.favorites.first().single()
        assertEquals("Vase", saved.title)
        assertEquals(1_234L, saved.price.minorUnits)
        repo.toggle(product(5))
        assertTrue(repo.favorites.first().isEmpty())
    }

    @Test fun newestFavoritesComeFirstAndRemoveByIdWorks() = runTest {
        now = 1; repo.toggle(product(1))
        now = 2; repo.toggle(product(2))
        assertEquals(listOf(2L, 1L), repo.favorites.first().map { it.productId })
        repo.remove(2)
        assertEquals(listOf(1L), repo.favorites.first().map { it.productId })
    }
}
