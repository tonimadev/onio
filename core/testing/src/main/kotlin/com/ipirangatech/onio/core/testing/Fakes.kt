package com.ipirangatech.onio.core.testing

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.CardBrand
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.model.CheckoutRequest
import com.ipirangatech.onio.core.model.FavoriteItem
import com.ipirangatech.onio.core.model.FavoritesRepository
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Order
import com.ipirangatech.onio.core.model.OrderItem
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.OrderStatus
import com.ipirangatech.onio.core.model.PaymentCard
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductPage
import com.ipirangatech.onio.core.model.ProductRepository
import com.ipirangatech.onio.core.model.Savings
import com.ipirangatech.onio.core.model.SettingsRepository
import com.ipirangatech.onio.core.model.sum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun product(
    id: Long,
    cents: Long = 1_000,
    discount: Double = 0.0,
    rating: Double = 4.0,
    category: String = "beauty",
    title: String = "Product $id",
) = Product(
    id = id, title = title, description = "desc", categorySlug = category, brand = null,
    price = Money.usd(cents), discountPercentage = discount, rating = rating, stock = 10,
    thumbnailUrl = "https://img/$id.png", imageUrls = listOf("https://img/$id.png"), reviews = emptyList(),
)

fun order(
    id: Long,
    status: OrderStatus = OrderStatus.PROCESSING,
    cents: Long = 1_000,
    items: Int = 1,
    placedAt: Long = 0,
    cancelAt: Long = 60_000,
) = Order(
    id = id, number = "ON-$id",
    items = List(items) { OrderItem(it + 1L, "Item", "", Money.usd(cents / items), 1) },
    totalUsd = Money.usd(cents), charged = Money.usd(cents),
    card = PaymentCard(CardBrand.VISA, "4242"), shippingName = "Ana", shippingAddress = "Rua A",
    placedAtMillis = placedAt, cancelAtMillis = cancelAt, status = status,
)

class FakeProductRepository(
    var products: List<Product> = emptyList(),
    var categories: List<Category> = emptyList(),
) : ProductRepository {
    var failure: Throwable? = null
    val searches = mutableListOf<String>()
    val categoryRequests = mutableListOf<String>()

    private fun page(list: List<Product>) = ProductPage(list, total = list.size, skip = 0)
    private fun <T> guard(block: () -> T): T = failure?.let { throw it } ?: block()

    override suspend fun products(limit: Int, skip: Int) = guard { page(products) }
    override suspend fun search(query: String, limit: Int, skip: Int) = guard {
        searches += query
        page(products.filter { it.title.contains(query, ignoreCase = true) })
    }
    override suspend fun productsByCategory(slug: String, limit: Int, skip: Int) = guard {
        categoryRequests += slug
        page(products.filter { it.categorySlug == slug })
    }
    override suspend fun categories() = guard { categories }
    override suspend fun product(id: Long) = guard { products.first { it.id == id } }
}

class FakeCartRepository(initial: List<CartItem> = emptyList()) : CartRepository {
    val state = MutableStateFlow(initial)
    var failure: Throwable? = null
    override val items: Flow<List<CartItem>> = state

    override suspend fun add(product: Product, quantity: Int) {
        failure?.let { throw it }
        val existing = state.value.firstOrNull { it.productId == product.id }
        state.value = if (existing == null) {
            state.value + CartItem(product.id, product.title, product.thumbnailUrl, product.price, quantity)
        } else {
            state.value.map { if (it.productId == product.id) it.copy(quantity = it.quantity + quantity) else it }
        }
    }

    override suspend fun setQuantity(productId: Long, quantity: Int) {
        state.value = if (quantity <= 0) state.value.filterNot { it.productId == productId }
        else state.value.map { if (it.productId == productId) it.copy(quantity = quantity) else it }
    }

    override suspend fun remove(productId: Long) {
        state.value = state.value.filterNot { it.productId == productId }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

class FakeFavoritesRepository : FavoritesRepository {
    val state = MutableStateFlow<List<FavoriteItem>>(emptyList())
    override val favorites: Flow<List<FavoriteItem>> = state

    override suspend fun toggle(product: Product) {
        state.value = if (state.value.any { it.productId == product.id }) state.value.filterNot { it.productId == product.id }
        else listOf(FavoriteItem(product.id, product.title, product.thumbnailUrl, product.price, product.discountPercentage, product.rating)) + state.value
    }

    override suspend fun remove(productId: Long) {
        state.value = state.value.filterNot { it.productId == productId }
    }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state

    override suspend fun setCancelAfterMinutes(minutes: Int) {
        state.value = state.value.copy(cancelAfterMinutes = minutes)
    }

    override suspend fun setCurrencyOverride(code: String?) {
        state.value = state.value.copy(currencyOverride = code)
    }

    override suspend fun completeOnboarding(userName: String?, cancelAfterMinutes: Int) {
        state.value = state.value.copy(
            userName = userName?.trim()?.ifEmpty { null },
            cancelAfterMinutes = cancelAfterMinutes,
            onboardingCompleted = true,
        )
    }
}

class FakeOrderRepository(initial: List<Order> = emptyList()) : OrderRepository {
    val state = MutableStateFlow(initial)
    var failure: Throwable? = null
    var cancelExpiredCalls = 0
    var cancelFailure: Throwable? = null
    val placed = mutableListOf<CheckoutRequest>()

    override val orders: Flow<List<Order>> = state
    override val savings: Flow<Savings> = state.map { all ->
        val cancelled = all.filter { it.status == OrderStatus.CANCELLED }
        Savings(cancelled.map { it.totalUsd }.sum(), cancelled.size, cancelled.sumOf { o -> o.items.sumOf { it.quantity } })
    }

    override fun order(id: Long): Flow<Order?> = state.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun placeOrder(request: CheckoutRequest): Order {
        failure?.let { throw it }
        placed += request
        val total = request.items.map { it.lineTotal }.sum()
        val created = order(id = state.value.size + 1L, cents = total.minorUnits)
        state.value = listOf(created) + state.value
        return created
    }

    override suspend fun cancelExpiredOrders() {
        cancelExpiredCalls++
        cancelFailure?.let { throw it }
    }
}
