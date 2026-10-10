package com.ipirangatech.onio.feature.product.impl

import androidx.compose.foundation.layout.Arrangement
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import com.ipirangatech.onio.core.ui.component.OnioButton
import com.ipirangatech.onio.core.ui.component.OnioOutlinedButton
import com.ipirangatech.onio.core.ui.component.OnioTopBar
import com.ipirangatech.onio.core.ui.component.SectionCard
import com.ipirangatech.onio.core.ui.motion.staggeredEntrance
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import java.text.NumberFormat
import com.ipirangatech.onio.core.model.DeliveryEstimate
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
import com.ipirangatech.onio.core.ui.component.FavoriteButton
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

@Composable
internal fun ProductScreen(
    state: ProductState,
    snackbar: SnackbarHostState,
    onIntent: (ProductIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            OnioTopBar(
                title = {},
                actions = {
                    if (state.product != null) FavoriteButton(state.isFavorite, { onIntent(ProductIntent.ToggleFavorite) }, Modifier.padding(end = 8.dp))
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ProductIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            if (state.product != null) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(Dimens.Space8),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                    ) {
                        OnioOutlinedButton(onClick = { onIntent(ProductIntent.AddToCart) }, modifier = Modifier.weight(1f), enabled = state.product.stock > 0) {
                            Text(stringResource(UiR.string.action_add_to_cart), maxLines = 1)
                        }
                        OnioButton(onClick = { onIntent(ProductIntent.BuyNow) }, modifier = Modifier.weight(1f), enabled = state.product.stock > 0) {
                            Text(stringResource(R.string.product_buy_now), maxLines = 1)
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
            else -> Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            ) {
                val pager = rememberPagerState { product.images.size }
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White)) {
                    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                        ProductImage(product.images[page], Modifier.fillMaxSize())
                    }
                    if (product.images.size > 1) {
                        // "2/5" counter, as in most marketplace apps.
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.Space12),
                            shape = MaterialTheme.shapes.large,
                            color = Color.Black.copy(alpha = 0.55f),
                        ) {
                            val counter = NumberFormat.getIntegerInstance()
                            Text(
                                counter.format(pager.currentPage + 1) + "/" + counter.format(product.images.size),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }
                }
                SectionCard(Modifier.fillMaxWidth().staggeredEntrance(0)) {
                    Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                            PriceText(product.price, originalPrice = product.originalPrice, style = MaterialTheme.typography.headlineMedium)
                            DiscountBadge(product.discountPercent)
                        }
                        Text(product.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                            RatingBadge(product.rating)
                            Text(
                                categoryLabel(product.categorySlug, product.categorySlug),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            if (product.stock > 0) pluralStringResource(R.plurals.product_in_stock, product.stock, product.stock)
                            else stringResource(R.string.product_out_of_stock),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (product.stock > 0) OnioTheme.colors.freeShipping else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                SectionCard(Modifier.fillMaxWidth().staggeredEntrance(1)) { DeliveryInfo(state.delivery, state.deliveryCity) }
                SectionCard(Modifier.fillMaxWidth().staggeredEntrance(2)) {
                    Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                        Text(product.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (product.reviews.isNotEmpty()) {
                    SectionCard(Modifier.fillMaxWidth().staggeredEntrance(3)) {
                        Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                            Text(stringResource(R.string.product_reviews), style = MaterialTheme.typography.titleMedium)
                            product.reviews.forEach { review ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                                        Text(review.reviewer, style = MaterialTheme.typography.labelLarge)
                                        RatingBadge(review.rating.toDouble())
                                    }
                                    Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Dimens.Space8))
            }
        }
    }
}

@Composable
private fun DeliveryInfo(estimate: DeliveryEstimate?, city: String?) {
    val context = LocalContext.current
    val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
    val subtitle = remember(estimate, city) {
        val now = System.currentTimeMillis()
        if (estimate == null) {
            DateUtils.formatDateTime(context, now + 4L * DateUtils.DAY_IN_MILLIS, flags) to false
        } else {
            val from = DateUtils.formatDateTime(context, now + estimate.minDays * DateUtils.DAY_IN_MILLIS, flags)
            val to = DateUtils.formatDateTime(context, now + estimate.maxDays * DateUtils.DAY_IN_MILLIS, flags)
            "$from|$to" to true
        }
    }
    Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
        val (text, personal) = subtitle
        val line = if (personal) {
            val (from, to) = text.split("|")
            stringResource(if (city != null) R.string.product_delivery_range_to else R.string.product_delivery_range, from, to, city.orEmpty())
        } else {
            stringResource(R.string.product_delivery_eta, text)
        }
        InfoRow(Icons.Filled.LocalShipping, stringResource(R.string.product_delivery_free), line)
        InfoRow(Icons.Filled.Replay, stringResource(R.string.product_returns), null)
        InfoRow(Icons.Filled.Lock, stringResource(R.string.product_secure), null)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
        Icon(icon, null, tint = OnioTheme.colors.freeShipping, modifier = Modifier.size(20.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
