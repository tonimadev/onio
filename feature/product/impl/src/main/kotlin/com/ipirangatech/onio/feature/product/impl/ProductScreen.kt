package com.ipirangatech.onio.feature.product.impl

import androidx.compose.foundation.layout.Arrangement
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.ipirangatech.onio.core.ui.component.DiscountBadge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.categoryLabel
import com.ipirangatech.onio.core.ui.component.LoadingBox
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.Pill
import com.ipirangatech.onio.core.ui.component.PriceText
import com.ipirangatech.onio.core.ui.component.ProductImage
import com.ipirangatech.onio.core.ui.component.RatingBadge

@Composable
fun ProductRoute(
    viewModel: ProductViewModel,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val addedMessage = stringResource(R.string.product_added)
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(ProductIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            ProductEffect.ShowAdded -> snackbar.showSnackbar(addedMessage)
            ProductEffect.NavigateToCart -> onOpenCart()
            ProductEffect.NavigateBack -> onBack()
        }
    }
    ProductScreen(state = state, snackbar = snackbar, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductScreen(
    state: ProductState,
    snackbar: SnackbarHostState,
    onIntent: (ProductIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onIntent(ProductIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            if (state.product != null) {
                BottomAppBar {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { onIntent(ProductIntent.AddToCart) }, modifier = Modifier.weight(1f), enabled = state.product.stock > 0) {
                            Text(stringResource(UiR.string.action_add_to_cart))
                        }
                        Button(onClick = { onIntent(ProductIntent.BuyNow) }, modifier = Modifier.weight(1f), enabled = state.product.stock > 0) {
                            Text(stringResource(R.string.product_buy_now))
                        }
                    }
                }
            }
        },
    ) { padding ->
        val product = state.product
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            product == null -> MessageState(
                title = stringResource(UiR.string.error_generic),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(UiR.string.action_retry),
                onAction = { onIntent(ProductIntent.Load) },
            )
            else -> Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                val pager = rememberPagerState { product.images.size }
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFF3F1F8))) {
                    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                        ProductImage(product.images[page], Modifier.fillMaxSize())
                    }
                    DiscountBadge(product.discountPercent, Modifier.align(Alignment.TopStart))
                    if (product.images.size > 1) {
                        Row(Modifier.align(Alignment.BottomCenter).padding(12.dp)) {
                            repeat(product.images.size) { index ->
                                Box(
                                    Modifier
                                        .padding(3.dp)
                                        .size(if (pager.currentPage == index) 18.dp else 7.dp, 7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pager.currentPage == index) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                )
                            }
                        }
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.title, style = MaterialTheme.typography.headlineSmall)
                    PriceText(product.price, originalPrice = product.originalPrice, style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(categoryLabel(product.categorySlug, product.categorySlug))
                        RatingBadge(product.rating)
                    }
                    DeliveryCard()
                    Text(
                        if (product.stock > 0) pluralStringResource(R.plurals.product_in_stock, product.stock, product.stock)
                        else stringResource(R.string.product_out_of_stock),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(product.description, style = MaterialTheme.typography.bodyMedium)
                    if (product.reviews.isNotEmpty()) {
                        HorizontalDivider()
                        Text(stringResource(R.string.product_reviews), style = MaterialTheme.typography.titleMedium)
                        product.reviews.forEach { review ->
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(review.reviewer, style = MaterialTheme.typography.labelLarge)
                                    RatingBadge(review.rating.toDouble())
                                }
                                Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DeliveryCard() {
    val context = LocalContext.current
    val eta = remember {
        DateUtils.formatDateTime(
            context,
            System.currentTimeMillis() + 4L * DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL,
        )
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow(Icons.Filled.LocalShipping, stringResource(R.string.product_delivery_free), stringResource(R.string.product_delivery_eta, eta))
            InfoRow(Icons.Filled.Replay, stringResource(R.string.product_returns), null)
            InfoRow(Icons.Filled.Lock, stringResource(R.string.product_secure), null)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.tertiary)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
