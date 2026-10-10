package com.ipirangatech.onio.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ipirangatech.onio.core.data.db.CouponDao
import com.ipirangatech.onio.core.data.db.CouponEntity
import com.ipirangatech.onio.core.data.db.toDomain
import com.ipirangatech.onio.core.model.CheckInResult
import com.ipirangatech.onio.core.model.CouponCatalog
import com.ipirangatech.onio.core.model.CouponRepository
import com.ipirangatech.onio.core.model.CouponTemplate
import com.ipirangatech.onio.core.model.PurchaseReward
import com.ipirangatech.onio.core.model.RedeemResult
import com.ipirangatech.onio.core.model.RewardsRepository
import com.ipirangatech.onio.core.model.RewardsWallet
import com.ipirangatech.onio.core.model.SpinResult
import com.ipirangatech.onio.core.model.WalletCoupon
import com.ipirangatech.onio.core.model.Wheel
import com.ipirangatech.onio.core.model.WheelPrize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.random.Random

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

internal class CouponRepositoryImpl(
    private val dao: CouponDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : CouponRepository {
    override val coupons: Flow<List<WalletCoupon>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun grant(template: CouponTemplate): WalletCoupon {
        val now = clock()
        val entity = CouponEntity(
            code = template.code,
            kind = template.kind.name,
            value = template.value,
            minSubtotalCents = template.minSubtotalCents,
            expiresAtMillis = now + template.validDays * DAY_MILLIS,
            usedAtMillis = null,
            grantedAtMillis = now,
        )
        return entity.copy(id = dao.insert(entity)).toDomain()
    }

    override suspend fun redeem(code: String): RedeemResult {
        val template = CouponCatalog.find(code) ?: return RedeemResult.Unknown
        if (dao.activeCount(template.code, clock()) > 0) return RedeemResult.AlreadyHave
        return RedeemResult.Added(grant(template))
    }

    override suspend fun markUsed(id: Long) = dao.markUsed(id, clock())
}

private val Context.rewardsDataStore by preferencesDataStore(name = "onio_rewards")

internal fun rewardsStore(context: Context): DataStore<Preferences> = context.rewardsDataStore

/** Coins, spins and check-in streak. State lives in DataStore; every mutation is one atomic `edit`. */
internal class RewardsRepositoryImpl(
    private val store: DataStore<Preferences>,
    private val coupons: CouponRepository,
    private val random: Random = Random.Default,
    private val today: () -> Long = { LocalDate.now().toEpochDay() },
) : RewardsRepository {
    private val coinsKey = intPreferencesKey("coins")
    private val spinsKey = intPreferencesKey("spins")
    private val streakKey = intPreferencesKey("streak")
    private val lastCheckInKey = longPreferencesKey("last_check_in_day")
    private val lastDailySpinKey = longPreferencesKey("last_daily_spin_day")

    override val wallet: Flow<RewardsWallet> = store.data.map { prefs ->
        val today = today()
        val last = prefs[lastCheckInKey]
        // A streak only counts while it is alive (checked in today or yesterday).
        val alive = last != null && today - last <= 1
        RewardsWallet(
            coins = prefs[coinsKey] ?: 0,
            spins = prefs[spinsKey] ?: 0,
            streak = if (alive) prefs[streakKey] ?: 0 else 0,
            checkedInToday = last == today,
        )
    }

    override suspend fun refreshDaily() {
        val day = today()
        store.edit { prefs ->
            if (prefs[lastDailySpinKey] != day) {
                prefs[spinsKey] = minOf((prefs[spinsKey] ?: 0) + 1, RewardsRepository.MAX_SPINS)
                prefs[lastDailySpinKey] = day
            }
        }
    }

    override suspend fun checkIn(): CheckInResult? {
        val day = today()
        var result: CheckInResult? = null
        store.edit { prefs ->
            val last = prefs[lastCheckInKey]
            if (last == day) return@edit
            val previous = prefs[streakKey] ?: 0
            val streak = if (last == day - 1) previous % 7 + 1 else 1
            val coins = RewardsRepository.CHECK_IN_COINS[streak - 1]
            val bonus = streak == 7
            prefs[coinsKey] = (prefs[coinsKey] ?: 0) + coins
            if (bonus) prefs[spinsKey] = minOf((prefs[spinsKey] ?: 0) + 1, RewardsRepository.MAX_SPINS)
            prefs[streakKey] = streak
            prefs[lastCheckInKey] = day
            result = CheckInResult(streak, coins, bonus)
        }
        return result
    }

    override suspend fun awardPurchase(): PurchaseReward {
        store.edit { prefs ->
            prefs[coinsKey] = (prefs[coinsKey] ?: 0) + RewardsRepository.PURCHASE_COINS
            prefs[spinsKey] = minOf((prefs[spinsKey] ?: 0) + RewardsRepository.PURCHASE_SPINS, RewardsRepository.MAX_SPINS)
        }
        return PurchaseReward(RewardsRepository.PURCHASE_COINS, RewardsRepository.PURCHASE_SPINS)
    }

    override suspend fun spin(): SpinResult? {
        var index = -1
        store.edit { prefs ->
            val spins = prefs[spinsKey] ?: 0
            if (spins <= 0) return@edit
            index = Wheel.draw(random)
            prefs[spinsKey] = spins - 1
            (Wheel.segments[index].prize as? WheelPrize.Coins)?.let { prefs[coinsKey] = (prefs[coinsKey] ?: 0) + it.amount }
        }
        if (index < 0) return null
        val prize = Wheel.segments[index].prize
        val coupon = (prize as? WheelPrize.Discount)?.let { coupons.grant(it.template) }
        return SpinResult(index, prize, coupon)
    }

    override suspend fun spendCoins(amount: Int): Boolean {
        var ok = false
        store.edit { prefs ->
            val coins = prefs[coinsKey] ?: 0
            if (amount in 0..coins) {
                prefs[coinsKey] = coins - amount
                ok = true
            }
        }
        return ok
    }
}
