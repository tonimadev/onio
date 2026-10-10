package com.ipirangatech.onio.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ipirangatech.onio.core.model.CouponCatalog
import com.ipirangatech.onio.core.model.CouponTemplate
import com.ipirangatech.onio.core.model.DiscountKind
import com.ipirangatech.onio.core.model.RedeemResult
import com.ipirangatech.onio.core.model.RewardsRepository
import com.ipirangatech.onio.core.model.Wheel
import com.ipirangatech.onio.core.model.WheelPrize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class RewardsRepositoryTest {
    private val job = Job()
    private val dir = kotlin.io.path.createTempDirectory("rewards").toFile()
    private var day = 20_000L
    private val couponDao = FakeCouponDao()
    private var now = 1_000L
    private val coupons = CouponRepositoryImpl(couponDao) { now }

    private fun store(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(UnconfinedTestDispatcher() + job),
        produceFile = { File(dir, "rewards.preferences_pb") },
    )

    private fun repo(seed: Int = 1, store: DataStore<Preferences> = store()) =
        RewardsRepositoryImpl(store, coupons, Random(seed)) { day }

    @After fun tearDown() {
        job.cancel()
        dir.deleteRecursively()
    }

    @Test fun theDailySpinIsGrantedOncePerDayAndBankedUpToTheCap() = runTest {
        val repo = repo()
        repo.refreshDaily()
        repo.refreshDaily()
        assertEquals(1, repo.wallet.first().spins)
        repeat(10) {
            day++
            repo.refreshDaily()
        }
        assertEquals(RewardsRepository.MAX_SPINS, repo.wallet.first().spins)
    }

    @Test fun checkInBuildsAStreakPaysMoreEachDayAndOnlyWorksOncePerDay() = runTest {
        val repo = repo()
        val first = repo.checkIn()!!
        assertEquals(1, first.day)
        assertEquals(5, first.coins)
        assertNull(repo.checkIn()) // same day
        assertTrue(repo.wallet.first().checkedInToday)

        day++
        val second = repo.checkIn()!!
        assertEquals(2, second.day)
        assertEquals(2, repo.wallet.first().streak)
    }

    @Test fun theSeventhDayPaysTheBonusSpinAndTheStreakWrapsAround() = runTest {
        val repo = repo()
        repeat(6) {
            repo.checkIn()
            day++
        }
        val seventh = repo.checkIn()!!
        assertEquals(7, seventh.day)
        assertTrue(seventh.bonusSpin)
        assertEquals(1, repo.wallet.first().spins)
        day++
        assertEquals(1, repo.checkIn()!!.day)
    }

    @Test fun missingADayResetsTheStreak() = runTest {
        val repo = repo()
        repo.checkIn()
        day++
        repo.checkIn()
        day += 3
        assertEquals(0, repo.wallet.first().streak) // no longer alive
        assertEquals(1, repo.checkIn()!!.day)
    }

    @Test fun purchasesPayAFlatRewardWhateverTheyCost() = runTest {
        val repo = repo()
        val reward = repo.awardPurchase()
        assertEquals(RewardsRepository.PURCHASE_COINS, reward.coins)
        val wallet = repo.wallet.first()
        assertEquals(25, wallet.coins)
        assertEquals(1, wallet.spins)
        repeat(10) { repo.awardPurchase() }
        assertEquals(RewardsRepository.MAX_SPINS, repo.wallet.first().spins)
    }

    @Test fun spinningNeedsASpinAndCreditsThePrize() = runTest {
        val repo = repo(seed = 7)
        assertNull(repo.spin())
        repeat(5) { repo.awardPurchase() }
        val before = repo.wallet.first()
        val result = repo.spin()!!
        val after = repo.wallet.first()
        assertEquals(before.spins - 1, after.spins)
        when (val prize = result.prize) {
            is WheelPrize.Coins -> assertEquals(before.coins + prize.amount, after.coins)
            is WheelPrize.Discount -> {
                assertNotNull(result.coupon)
                assertEquals(prize.template.code, result.coupon!!.code)
                assertEquals(before.coins, after.coins)
            }
        }
        assertEquals(Wheel.segments[result.segmentIndex].prize, result.prize)
    }

    @Test fun spendingCoinsNeverGoesBelowZero() = runTest {
        val repo = repo()
        repo.awardPurchase()
        assertTrue(repo.spendCoins(10))
        assertEquals(15, repo.wallet.first().coins)
        assertFalse(repo.spendCoins(16))
        assertEquals(15, repo.wallet.first().coins)
    }

    @Test fun stateSurvivesANewRepositoryOverTheSameStorage() = runTest {
        val shared = store()
        repo(store = shared).awardPurchase()
        assertEquals(25, repo(store = shared).wallet.first().coins)
    }

    // ---- coupons ----

    @Test fun grantingACouponSetsItsExpiry() = runTest {
        val coupon = coupons.grant(CouponTemplate("T", DiscountKind.PERCENT, 5, validDays = 2))
        assertEquals(now + 2 * 86_400_000L, coupon.expiresAtMillis)
        assertTrue(coupon.isUsable(now))
    }

    @Test fun redeemingACodeAddsItOnceWhileItIsStillUsable() = runTest {
        assertTrue(coupons.redeem(" welcome10 ") is RedeemResult.Added)
        assertEquals(RedeemResult.AlreadyHave, coupons.redeem("WELCOME10"))
        assertEquals(RedeemResult.Unknown, coupons.redeem("NOPE"))
        // Once used, the same code can be redeemed again.
        val id = coupons.coupons.first().single().id
        coupons.markUsed(id)
        assertTrue(coupons.redeem("WELCOME10") is RedeemResult.Added)
    }

    @Test fun usableCouponsComeBeforeUsedOnes() = runTest {
        val a = coupons.grant(CouponCatalog.Percent5)
        coupons.grant(CouponCatalog.Percent10)
        coupons.markUsed(a.id)
        val list = coupons.coupons.first()
        assertEquals(listOf(false, true), list.map { it.usedAtMillis != null })
    }
}
