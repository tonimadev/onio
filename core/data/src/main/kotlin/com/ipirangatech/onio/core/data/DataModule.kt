package com.ipirangatech.onio.core.data

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.ipirangatech.onio.core.data.db.CartDao
import com.ipirangatech.onio.core.data.db.FavoriteDao
import com.ipirangatech.onio.core.data.db.CouponDao
import com.ipirangatech.onio.core.data.db.MIGRATION_1_2
import com.ipirangatech.onio.core.data.db.MIGRATION_2_3
import com.ipirangatech.onio.core.data.db.OnioDatabase
import com.ipirangatech.onio.core.data.db.OrderDao
import com.ipirangatech.onio.core.model.CartRepository
import com.ipirangatech.onio.core.model.CepSource
import com.ipirangatech.onio.core.model.ShippingRepository
import com.ipirangatech.onio.core.model.CouponRepository
import com.ipirangatech.onio.core.model.RewardsRepository
import com.ipirangatech.onio.core.model.FavoritesRepository
import com.ipirangatech.onio.core.model.OrderRepository
import com.ipirangatech.onio.core.model.ProductRepository
import com.ipirangatech.onio.core.model.SettingsRepository
import com.ipirangatech.onio.core.network.ProductRemoteSource
import com.ipirangatech.onio.core.network.cep.createCepSource
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun cartDao(db: OnioDatabase): CartDao = db.cartDao()
    @Provides fun orderDao(db: OnioDatabase): OrderDao = db.orderDao()
    @Provides fun favoriteDao(db: OnioDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun couponDao(db: OnioDatabase): CouponDao = db.couponDao()

    @Provides @Singleton
    fun couponRepository(dao: CouponDao): CouponRepository = CouponRepositoryImpl(dao)

    @Provides @Singleton
    fun rewardsRepository(@ApplicationContext context: Context, coupons: CouponRepository): RewardsRepository =
        RewardsRepositoryImpl(rewardsStore(context), coupons)

    @Provides @Singleton
    fun favoritesRepository(dao: FavoriteDao): FavoritesRepository = FavoritesRepositoryImpl(dao)

    @Provides @Singleton
    fun remoteSource(): ProductRemoteSource = createDummyJsonSource()

    @Provides @Singleton
    fun cepSource(): CepSource = createCepSource()

    /** Official Correios quotes need a contract and a backend; bind a real source here once one exists. */
    @Provides @Singleton
    fun shippingRepository(@ApplicationContext context: Context, cepSource: CepSource): ShippingRepository =
        ShippingRepositoryImpl(shippingStore(context), cepSource, quoteSource = null)

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
