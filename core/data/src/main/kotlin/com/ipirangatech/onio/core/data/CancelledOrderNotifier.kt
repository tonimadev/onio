package com.ipirangatech.onio.core.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.ipirangatech.onio.core.model.MoneyFormatter
import com.ipirangatech.onio.core.model.Order
import com.ipirangatech.onio.core.model.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Locale

/** Tells the user an order was cancelled automatically. */
internal fun interface CancelledOrderNotifier {
    suspend fun notifyCancelled(order: Order)
}

internal class AndroidCancelledOrderNotifier(
    private val context: Context,
    private val settings: SettingsRepository,
) : CancelledOrderNotifier {

    @SuppressLint("MissingPermission") // checked in canNotify()
    override suspend fun notifyCancelled(order: Order) {
        // Nothing to announce while the user is already looking at the app.
        if (isAppInForeground() || !canNotify()) return
        ensureChannel()

        val appSettings = settings.settings.first()
        val locale = Locale.getDefault()
        val saved = MoneyFormatter.forLocale(locale, appSettings.currencyOverride).format(order.totalUsd)
        val name = appSettings.userName
        val text = if (name.isNullOrBlank()) context.getString(R.string.notif_cancelled_text, saved)
        else context.getString(R.string.notif_cancelled_text_named, name, saved)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(context, order.id.toInt(), it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_onio)
            .setContentTitle(context.getString(R.string.notif_cancelled_title, order.number))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(order.id.toInt(), notification)
    }

    private fun isAppInForeground() =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private companion object {
        const val CHANNEL_ID = "order_updates"
    }
}
