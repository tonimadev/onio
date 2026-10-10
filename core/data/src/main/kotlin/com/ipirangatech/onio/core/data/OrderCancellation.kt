package com.ipirangatech.onio.core.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ipirangatech.onio.core.model.OrderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Schedules the "magic" cancellation of an order after [delayMillis]. */
internal fun interface OrderCancellationScheduler {
    fun schedule(orderId: Long, delayMillis: Long)
}

/** WorkManager-backed scheduler: survives process death. */
internal class WorkManagerCancellationScheduler(private val workManager: WorkManager) : OrderCancellationScheduler {
    override fun schedule(orderId: Long, delayMillis: Long) {
        // A tiny grace period guarantees the deadline has passed when the worker runs.
        val request = OneTimeWorkRequestBuilder<OrderCancellationWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0) + 1_000, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork("cancel-order-$orderId", ExistingWorkPolicy.REPLACE, request)
    }
}

@HiltWorker
internal class OrderCancellationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val orders: OrderRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching { orders.cancelExpiredOrders() }
        .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
