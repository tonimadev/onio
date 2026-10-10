package com.ipirangatech.onio.core.data

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.ipirangatech.onio.core.data.db.CartDao
import com.ipirangatech.onio.core.data.db.FavoriteDao
import com.ipirangatech.onio.core.data.db.MIGRATION_1_2
import com.ipirangatech.onio.core.data.db.OnioDatabase
import com.ipirangatech.onio.core.data.db.OrderDao
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.FavoritesRepository
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.ProductRepository
import com.ipirangatech.onio.core.model.SettingsRepository
import com.ipirangatech.onio.core.network.ProductRemoteSource
import com.ipirangatech.onio.core.network.dummyjson.createDummyJsonSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): OnioDatabase =
        Room.databaseBuilder(context, OnioDatabase::class.java, "onio.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun cartDao(db: OnioDatabase): CartDao = db.cartDao()
    @Provides fun orderDao(db: OnioDatabase): OrderDao = db.orderDao()
    @Provides fun favoriteDao(db: OnioDatabase): FavoriteDao = db.favoriteDao()

    @Provides @Singleton
    fun favoritesRepository(dao: FavoriteDao): FavoritesRepository = FavoritesRepositoryImpl(dao)

    @Provides @Singleton
    fun remoteSource(): ProductRemoteSource = createDummyJsonSource()

    @Provides @Singleton
    fun productRepository(remote: ProductRemoteSource): ProductRepository = ProductRepositoryImpl(remote)

    @Provides @Singleton
    fun cartRepository(dao: CartDao): CartRepository = CartRepositoryImpl(dao)

    @Provides @Singleton
    fun settingsRepository(@ApplicationContext context: Context): SettingsRepository = SettingsRepositoryImpl(context)

    @Provides @Singleton
    fun orderRepository(
        dao: OrderDao,
        settings: SettingsRepository,
        @ApplicationContext context: Context,
    ): OrderRepository = OrderRepositoryImpl(
        dao = dao,
        settings = settings,
        scheduler = WorkManagerCancellationScheduler(WorkManager.getInstance(context)),
        notifier = AndroidCancelledOrderNotifier(context, settings),
    )
}
