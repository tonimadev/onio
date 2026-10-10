package com.ipirangatech.onio.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ipirangatech.onio.core.data.db.CartDao
import com.ipirangatech.onio.core.data.db.FavoriteDao
import com.ipirangatech.onio.core.data.db.FavoriteEntity
import com.ipirangatech.onio.core.data.db.OrderDao
import com.ipirangatech.onio.core.data.db.OrderEntity
import com.ipirangatech.onio.core.data.db.encode
import com.ipirangatech.onio.core.data.db.toDomain
import com.ipirangatech.onio.core.data.db.toEntity
import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.CartItem
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.Category
import com.ipirangatech.onio.core.model.FavoriteItem
import com.ipirangatech.onio.core.model.FavoritesRepository
import com.ipirangatech.onio.core.model.CheckoutRequest
import com.ipirangatech.onio.core.model.ExchangeRates
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.Order
import com.ipirangatech.onio.core.model.OrderItem
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.OrderStatus
import com.ipirangatech.onio.core.model.Product
import com.ipirangatech.onio.core.model.ProductPage
import com.ipirangatech.onio.core.model.ProductRepository
import com.ipirangatech.onio.core.model.Savings
import com.ipirangatech.onio.core.model.SettingsRepository
import com.ipirangatech.onio.core.model.sum
import com.ipirangatech.onio.core.network.ProductRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

internal class ProductRepositoryImpl(private val remote: ProductRemoteSource) : ProductRepository {
    override suspend fun products(limit: Int, skip: Int): ProductPage = remote.products(limit, skip)
    override suspend fun search(query: String, limit: Int, skip: Int): ProductPage = remote.search(query, limit, skip)
    override suspend fun productsByCategory(slug: String, limit: Int, skip: Int): ProductPage =
        remote.productsByCategory(slug, limit, skip)
    override suspend fun categories(): List<Category> = remote.categories()
    override suspend fun product(id: Long): Product = remote.product(id)
}

internal class FavoritesRepositoryImpl(
    private val dao: FavoriteDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : FavoritesRepository {
    override val favorites: Flow<List<FavoriteItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun toggle(product: Product) {
        if (dao.count(product.id) > 0) {
            dao.delete(product.id)
        } else {
            dao.upsert(
                FavoriteEntity(
                    productId = product.id,
                    title = product.title,
                    thumbnailUrl = product.thumbnailUrl,
                    priceUsdCents = product.price.minorUnits,
                    discountPercentage = product.discountPercentage,
                    rating = product.rating,
                    addedAtMillis = clock(),
                ),
            )
        }
    }

    override suspend fun remove(productId: Long) = dao.delete(productId)
}

internal class CartRepositoryImpl(private val dao: CartDao) : CartRepository {
    override val items: Flow<List<CartItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun add(product: Product, quantity: Int) {
        val existing = dao.get(product.id)
        val newQuantity = ((existing?.quantity ?: 0) + quantity).coerceIn(1, MAX_QUANTITY)
        dao.upsert(
            CartItem(product.id, product.title, product.thumbnailUrl, product.price, newQuantity).toEntity(),
        )
    }

    override suspend fun setQuantity(productId: Long, quantity: Int) {
        if (quantity <= 0) return dao.delete(productId)
        val existing = dao.get(productId) ?: return
        dao.upsert(existing.copy(quantity = quantity.coerceAtMost(MAX_QUANTITY)))
    }

    override suspend fun remove(productId: Long) = dao.delete(productId)
    override suspend fun clear() = dao.clear()

    private companion object {
        const val MAX_QUANTITY = 99
    }
}

internal class OrderRepositoryImpl(
    private val dao: OrderDao,
    private val settings: SettingsRepository,
    private val scheduler: OrderCancellationScheduler,
    private val notifier: CancelledOrderNotifier,
    private val clock: () -> Long = System::currentTimeMillis,
) : OrderRepository {
    override val orders: Flow<List<Order>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override val savings: Flow<Savings> = orders.map { all ->
        val cancelled = all.filter { it.status == OrderStatus.CANCELLED }
        Savings(
            total = cancelled.map { it.totalUsd }.sum(),
            cancelledOrders = cancelled.size,
            itemsNotBought = cancelled.sumOf { order -> order.items.sumOf { it.quantity } },
        )
    }

    override fun order(id: Long): Flow<Order?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun placeOrder(request: CheckoutRequest): Order {
        require(request.items.isNotEmpty()) { "Cart is empty" }
        val minutes = settings.settings.first().cancelAfterMinutes
        val now = clock()
        val items = request.items.map { OrderItem(it.productId, it.title, it.thumbnailUrl, it.unitPrice, it.quantity) }
        val totalUsd = items.map { it.unitPrice * it.quantity }.sum()
        val charged = ExchangeRates.convert(totalUsd, request.currencyCode)
        val entity = OrderEntity(
            number = "ON-" + Random.nextLong(10_000_000, 99_999_999),
            itemsJson = items.encode(),
            totalUsdCents = totalUsd.minorUnits,
            chargedMinorUnits = charged.minorUnits,
            chargedCurrency = charged.currencyCode,
            cardBrand = request.card.brand.name,
            cardLast4 = request.card.last4,
            shippingName = request.shippingName,
            shippingAddress = request.shippingAddress,
            placedAtMillis = now,
            cancelAtMillis = now + minutes * 60_000L,
            status = OrderStatus.PROCESSING.name,
        )
        val id = dao.insert(entity)
        scheduler.schedule(id, entity.cancelAtMillis - now)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun cancelExpiredOrders() {
        dao.cancelExpired(clock()).forEach { entity ->
            // A failed notification must never fail the cancellation itself.
            runCatching { notifier.notifyCancelled(entity.toDomain()) }
        }
    }
}

private val Context.dataStore by preferencesDataStore(name = "onio_settings")

internal class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private val minutesKey = intPreferencesKey("cancel_after_minutes")
    private val currencyKey = stringPreferencesKey("currency_override")
    private val nameKey = stringPreferencesKey("user_name")
    private val onboardingKey = booleanPreferencesKey("onboarding_completed")

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            cancelAfterMinutes = prefs[minutesKey] ?: AppSettings.DEFAULT_CANCEL_MINUTES,
            currencyOverride = prefs[currencyKey],
            userName = prefs[nameKey],
            onboardingCompleted = prefs[onboardingKey] ?: false,
        )
    }

    override suspend fun setCancelAfterMinutes(minutes: Int) {
        context.dataStore.edit { it[minutesKey] = minutes.coerceIn(1, AppSettings.MAX_CANCEL_MINUTES) }
    }

    override suspend fun setCurrencyOverride(code: String?) {
        context.dataStore.edit { if (code == null) it.remove(currencyKey) else it[currencyKey] = code }
    }

    override suspend fun completeOnboarding(userName: String?, cancelAfterMinutes: Int) {
        context.dataStore.edit {
            val name = userName?.trim().orEmpty()
            if (name.isEmpty()) it.remove(nameKey) else it[nameKey] = name.take(MAX_NAME_LENGTH)
            it[minutesKey] = cancelAfterMinutes.coerceIn(1, AppSettings.MAX_CANCEL_MINUTES)
            it[onboardingKey] = true
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
