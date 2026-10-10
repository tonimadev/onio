package com.ipirangatech.onio.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY rowid")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun get(productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun delete(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}

@Dao
internal abstract class OrderDao {
    @Query("SELECT * FROM orders ORDER BY placedAtMillis DESC")
    abstract fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    abstract fun observe(id: Long): Flow<OrderEntity?>

    @Insert
    abstract suspend fun insert(order: OrderEntity): Long

    @Query("SELECT * FROM orders WHERE status = 'PROCESSING' AND cancelAtMillis <= :now")
    abstract suspend fun expiredProcessing(now: Long): List<OrderEntity>

    @Query("UPDATE orders SET status = 'CANCELLED' WHERE id IN (:ids)")
    abstract suspend fun markCancelled(ids: List<Long>)

    /**
     * Cancels every overdue order and returns exactly the ones this call cancelled. Running in one
     * transaction means concurrent callers (worker + app start) never both "win" the same order.
     */
    @Transaction
    open suspend fun cancelExpired(now: Long): List<OrderEntity> {
        val expired = expiredProcessing(now)
        if (expired.isNotEmpty()) markCancelled(expired.map { it.id })
        return expired
    }
}

@Database(entities = [CartItemEntity::class, OrderEntity::class], version = 1, exportSchema = false)
internal abstract class OnioDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
}
