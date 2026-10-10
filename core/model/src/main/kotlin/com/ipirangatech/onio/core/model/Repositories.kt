package com.ipirangatech.onio.core.model

import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun products(limit: Int, skip: Int): ProductPage
    suspend fun search(query: String, limit: Int, skip: Int): ProductPage
    suspend fun productsByCategory(slug: String, limit: Int, skip: Int): ProductPage
    suspend fun categories(): List<Category>
    suspend fun product(id: Long): Product
}

/** What a saved favourite needs to be shown offline (no catalog round trip). */
data class FavoriteItem(
    val productId: Long,
    val title: String,
    val thumbnailUrl: String,
    val price: Money,
    val discountPercentage: Double,
    val rating: Double,
)

interface FavoritesRepository {
    /** Most recently saved first. */
    val favorites: Flow<List<FavoriteItem>>

    /** Saves the product, or removes it if it was already a favourite. */
    suspend fun toggle(product: Product)

    suspend fun remove(productId: Long)
}

interface CartRepository {
    val items: Flow<List<CartItem>>
    suspend fun add(product: Product, quantity: Int = 1)
    suspend fun setQuantity(productId: Long, quantity: Int)
    suspend fun remove(productId: Long)
    suspend fun clear()
}

data class CheckoutRequest(
    val items: List<CartItem>,
    val card: PaymentCard,
    val shippingName: String,
    val shippingAddress: String,
    val currencyCode: String,
)

interface OrderRepository {
    val orders: Flow<List<Order>>
    val savings: Flow<Savings>
    fun order(id: Long): Flow<Order?>
    suspend fun placeOrder(request: CheckoutRequest): Order

    /** Cancels every PROCESSING order whose deadline has passed. Safe to call repeatedly. */
    suspend fun cancelExpiredOrders()
}

data class AppSettings(
    val cancelAfterMinutes: Int = DEFAULT_CANCEL_MINUTES,
    /** Null means "derive from the current language". */
    val currencyOverride: String? = null,
    /** How the user asked to be called (onboarding); null when skipped. */
    val userName: String? = null,
    val onboardingCompleted: Boolean = false,
) {
    companion object {
        const val DEFAULT_CANCEL_MINUTES = 60

        /** Longest cancel delay a user can configure: 7 days. */
        const val MAX_CANCEL_MINUTES = 7 * 24 * 60

        /** Delays offered in onboarding and Settings, in minutes (1 min … 24 h). */
        val CANCEL_DELAY_OPTIONS_MINUTES: List<Int> = listOf(1, 5, 15, 30, 60, 240, 1440)
    }
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setCancelAfterMinutes(minutes: Int)
    suspend fun setCurrencyOverride(code: String?)

    /** Marks onboarding as done; stores the optional name and how soon orders get cancelled. */
    suspend fun completeOnboarding(userName: String?, cancelAfterMinutes: Int)
}
