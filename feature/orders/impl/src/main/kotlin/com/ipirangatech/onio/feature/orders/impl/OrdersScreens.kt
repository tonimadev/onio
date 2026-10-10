package com.ipirangatech.onio.feature.orders.impl

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.component.LoadingBox
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.Pill
import com.ipirangatech.onio.core.ui.component.PriceText
import com.ipirangatech.onio.core.ui.component.ProductImage
import com.ipirangatech.onio.core.ui.formatCountdown
import kotlinx.coroutines.delay
import java.util.Locale

/** Wall clock that ticks every second while composed. */
@Composable
private fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1_000)
    }
}

@Composable
private fun formatDate(millis: Long): String =
    DateUtils.formatDateTime(LocalContext.current, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH)

@Composable
fun OrdersRoute(viewModel: OrdersViewModel, onOpenOrder: (Long) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(OrdersIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            is OrdersEffect.NavigateToOrder -> onOpenOrder(effect.orderId)
        }
    }
    OrdersScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrdersScreen(state: OrdersState, onIntent: (OrdersIntent) -> Unit, modifier: Modifier = Modifier) {
    val now by rememberNow()
    // Fire once when the earliest pending deadline passes so the list flips to "cancelled".
    val pendingDeadline = state.orders.filter { !it.isCancelled }.minOfOrNull { it.cancelAtMillis }
    LaunchedEffect(pendingDeadline, pendingDeadline != null && now >= pendingDeadline) {
        if (pendingDeadline != null && now >= pendingDeadline) onIntent(OrdersIntent.DeadlineReached)
    }
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.orders_title)) }) }) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            state.orders.isEmpty() -> MessageState(stringResource(R.string.orders_empty), Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.orders, key = UiOrder::id) { order ->
                    OrderCard(order, now, onClick = { onIntent(OrdersIntent.OrderClicked(order.id)) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderCard(order: UiOrder, now: Long, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.number, style = MaterialTheme.typography.titleMedium)
                StatusPill(order, now)
            }
            Text(formatDate(order.placedAtMillis), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                order.items.take(4).forEach { ProductImage(it.thumbnailUrl, Modifier.size(48.dp)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(pluralStringResource(R.plurals.orders_items, order.itemCount, order.itemCount))
                PriceText(order.total)
            }
        }
    }
}

@Composable
private fun StatusPill(order: UiOrder, now: Long) {
    if (order.isCancelled) {
        Pill(stringResource(R.string.orders_status_cancelled), container = MaterialTheme.colorScheme.tertiaryContainer)
    } else {
        Pill(stringResource(R.string.orders_status_processing))
    }
}

@Composable
fun OrderDetailRoute(viewModel: OrderDetailViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(OrderDetailIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            OrderDetailEffect.NavigateBack -> onBack()
        }
    }
    OrderDetailScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderDetailScreen(state: OrderDetailState, onIntent: (OrderDetailIntent) -> Unit, modifier: Modifier = Modifier) {
    val now by rememberNow()
    val order = state.order
    LaunchedEffect(order?.isCancelled, order != null && now >= order.cancelAtMillis) {
        if (order != null && !order.isCancelled && now >= order.cancelAtMillis) onIntent(OrderDetailIntent.DeadlineReached)
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(order?.number.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(OrderDetailIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            order == null -> MessageState(stringResource(UiR.string.error_generic), Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (order.isCancelled) {
                                Text(stringResource(R.string.orders_cancelled_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary)
                                Text(
                                    if (state.userName.isNullOrBlank()) stringResource(R.string.orders_cancelled_body)
                                    else stringResource(R.string.orders_cancelled_body_named, state.userName),
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.orders_you_saved))
                                    PriceText(order.total, style = MaterialTheme.typography.titleLarge)
                                }
                            } else {
                                Text(stringResource(R.string.orders_processing_title), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(R.string.orders_processing_body))
                                Text(
                                    stringResource(R.string.orders_countdown, formatCountdown(order.cancelAtMillis - now, Locale.getDefault())),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
                items(order.items, key = UiOrderItem::productId) { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProductImage(item.thumbnailUrl, Modifier.size(56.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                            Text("× " + item.quantity, style = MaterialTheme.typography.bodySmall)
                        }
                        PriceText(item.unitPrice * item.quantity)
                    }
                }
                item {
                    HorizontalDivider()
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.orders_total), style = MaterialTheme.typography.titleMedium)
                            PriceText(order.total)
                        }
                        Text(stringResource(R.string.orders_ship_to, order.shippingName, order.shippingAddress), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.orders_paid_with, order.cardLast4), style = MaterialTheme.typography.bodySmall)
                        Text(formatDate(order.placedAtMillis), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
