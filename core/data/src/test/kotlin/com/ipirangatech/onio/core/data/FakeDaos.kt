package com.ipirangatech.onio.core.data

import com.ipirangatech.onio.core.data.db.CartDao
import com.ipirangatech.onio.core.data.db.CouponDao
import com.ipirangatech.onio.core.data.db.CouponEntity
import com.ipirangatech.onio.core.data.db.CartItemEntity
import com.ipirangatech.onio.core.data.db.FavoriteDao
import com.ipirangatech.onio.core.data.db.FavoriteEntity
import com.ipirangatech.onio.core.data.db.OrderDao
import com.ipirangatech.onio.core.data.db.OrderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory DAOs. [FakeOrderDao] inherits the real transactional `cancelExpired` logic on purpose. */
internal class FakeCartDao : CartDao {
    val rows = MutableStateFlow<List<CartItemEntity>>(emptyList())
    override fun observeAll(): Flow<List<CartItemEntity>> = rows
    override suspend fun get(productId: Long) = rows.value.firstOrNull { it.productId == productId }
    override suspend fun upsert(item: CartItemEntity) {
        rows.value = rows.value.filterNot { it.productId == item.productId } + item
    }
    override suspend fun delete(productId: Long) {
        rows.value = rows.value.filterNot { it.productId == productId }
    }
    override suspend fun clear() {
        rows.value = emptyList()
    }
}

internal class FakeFavoriteDao : FavoriteDao {
    val rows = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    override fun observeAll(): Flow<List<FavoriteEntity>> = rows.map { list -> list.sortedByDescending { it.addedAtMillis } }
    override suspend fun count(productId: Long) = rows.value.count { it.productId == productId }
    override suspend fun upsert(favorite: FavoriteEntity) {
        rows.value = rows.value.filterNot { it.productId == favorite.productId } + favorite
    }
    override suspend fun delete(productId: Long) {
        rows.value = rows.value.filterNot { it.productId == productId }
    }
}

internal class FakeOrderDao : OrderDao() {
    val rows = MutableStateFlow<List<OrderEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<OrderEntity>> = rows.map { list -> list.sortedByDescending { it.placedAtMillis } }
    override fun observe(id: Long): Flow<OrderEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun insert(order: OrderEntity): Long {
        val id = nextId++
        rows.value = rows.value + order.copy(id = id)
        return id
    }
    override suspend fun expiredProcessing(now: Long) =
        rows.value.filter { it.status == "PROCESSING" && it.cancelAtMillis <= now }
    override suspend fun markCancelled(ids: List<Long>) {
        rows.value = rows.value.map { if (it.id in ids) it.copy(status = "CANCELLED") else it }
    }
}

internal class FakeCouponDao : CouponDao {
    val rows = MutableStateFlow<List<CouponEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<CouponEntity>> =
        rows.map { list -> list.sortedWith(compareBy({ it.usedAtMillis != null }, { it.expiresAtMillis })) }
    override suspend fun insert(coupon: CouponEntity): Long {
        val id = nextId++
        rows.value = rows.value + coupon.copy(id = id)
        return id
    }
    override suspend fun markUsed(id: Long, now: Long) {
        rows.value = rows.value.map { if (it.id == id && it.usedAtMillis == null) it.copy(usedAtMillis = now) else it }
    }
    override suspend fun activeCount(code: String, now: Long) =
        rows.value.count { it.code == code && it.usedAtMillis == null && it.expiresAtMillis > now }
}
