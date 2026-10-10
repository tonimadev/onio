package com.ipirangatech.onio.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
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
internal interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY placedAtMillis DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun observe(id: Long): Flow<OrderEntity?>

    @Insert
    suspend fun insert(order: OrderEntity): Long

    @Query("UPDATE orders SET status = 'CANCELLED' WHERE status = 'PROCESSING' AND cancelAtMillis <= :now")
    suspend fun cancelExpired(now: Long): Int
}

@Database(entities = [CartItemEntity::class, OrderEntity::class], version = 1, exportSchema = false)
internal abstract class OnioDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
}
