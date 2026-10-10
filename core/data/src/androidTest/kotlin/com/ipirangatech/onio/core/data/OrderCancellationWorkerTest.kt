package com.ipirangatech.onio.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.ipirangatech.onio.core.testing.FakeOrderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderCancellationWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val orders = FakeOrderRepository()

    private fun worker() = TestListenableWorkerBuilder<OrderCancellationWorker>(context)
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                OrderCancellationWorker(appContext, workerParameters, orders)
        })
        .build()

    @Test fun cancelsOverdueOrdersAndReportsSuccess() = runBlocking {
        val result = worker().doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, orders.cancelExpiredCalls)
    }

    @Test fun asksWorkManagerToRetryWhenTheRepositoryFails() = runBlocking {
        orders.cancelFailure = IllegalStateException("database locked")
        val result = worker().doWork()
        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
