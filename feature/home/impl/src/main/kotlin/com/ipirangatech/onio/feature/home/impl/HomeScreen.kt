package com.ipirangatech.onio.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.categoryIcon
import com.ipirangatech.onio.core.ui.categoryLabel
import com.ipirangatech.onio.core.ui.component.CircleIcon
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.ProductCard
import com.ipirangatech.onio.core.ui.component.ProductCardSkeleton
import com.ipirangatech.onio.core.ui.component.SaleRed
import com.ipirangatech.onio.core.ui.formatCountdown
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenProduct: (Long) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val addedMessage = stringResource(UiR.string.msg_added_to_cart)
    val viewCart = stringResource(UiR.string.action_view_cart)
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(HomeIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            is HomeEffect.NavigateToProduct -> onOpenProduct(effect.productId)
            is HomeEffect.NavigateToCategory -> onOpenCategory(effect.slug)
            HomeEffect.NavigateToSearch -> onOpenSearch()
            HomeEffect.NavigateToCart -> onOpenCart()
            HomeEffect.ShowAdded -> {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(addedMessage, actionLabel = viewCart)
                if (result == SnackbarResult.ActionPerformed) viewModel.onIntent(HomeIntent.ViewCartClicked)
            }
        }
    }
    HomeScreen(state = state, snackbar = snackbar, onIntent = viewModel::onIntent, modifier = modifier)
}

private data class Banner(val title: Int, val subtitle: Int, val colors: List<Color>)

private val Banners = listOf(
    Banner(R.string.home_banner1_title, R.string.home_banner1_sub, listOf(Color(0xFF6C3CE9), Color(0xFFE040A0))),
    Banner(R.string.home_banner2_title, R.string.home_banner2_sub, listOf(Color(0xFFFF7A1A), Color(0xFFFFB400))),
    Banner(R.string.home_banner3_title, R.string.home_banner3_sub, listOf(Color(0xFF0E9F6E), Color(0xFF2BB0D8))),
)

/** Milliseconds left until local midnight, ticking every second. */
@Composable
private fun rememberTimeLeftToday(): State<Long> = produceState(0L) {
    while (true) {
        val now = Calendar.getInstance()
        val end = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        value = end.timeInMillis - now.timeInMillis
        delay(1_000)
    }
}

@Composable
internal fun HomeScreen(
    state: HomeState,
    snackbar: SnackbarHostState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.hasError && state.products.isEmpty()) {
            MessageState(
                title = stringResource(UiR.string.error_generic),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(UiR.string.action_retry),
                onAction = { onIntent(HomeIntent.Load) },
            )
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header(userName = state.userName, onSearch = { onIntent(HomeIntent.SearchClicked) }) }
            item(span = { GridItemSpan(maxLineSpan) }) { PromoPager() }
            if (state.isLoading) {
                items(6) { ProductCardSkeleton(Modifier.padding(horizontal = 16.dp)) }
                return@LazyVerticalGrid
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.categories, key = UiCategory::slug) { category ->
                        Column(
                            Modifier.width(72.dp).clickable { onIntent(HomeIntent.CategoryClicked(category.slug)) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CircleIcon {
                                Icon(categoryIcon(category.slug), null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                categoryLabel(category.slug, category.name),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                minLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (state.deals.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { DealsHeader() }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.deals, key = UiProduct::id) { product ->
                            ProductCard(
                                title = product.title,
                                thumbnailUrl = product.thumbnailUrl,
                                price = product.price,
                                originalPrice = product.originalPrice,
                                rating = product.rating,
                                discountPercent = product.discountPercent,
                                onClick = { onIntent(HomeIntent.ProductClicked(product.id)) },
                                onAddToCart = { onIntent(HomeIntent.AddToCart(product.id)) },
                                modifier = Modifier.width(160.dp),
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.home_recommended),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(state.products, key = UiProduct::id) { product ->
                ProductCard(
                    title = product.title,
                    thumbnailUrl = product.thumbnailUrl,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    rating = product.rating,
                    discountPercent = product.discountPercent,
                    onClick = { onIntent(HomeIntent.ProductClicked(product.id)) },
                    onAddToCart = { onIntent(HomeIntent.AddToCart(product.id)) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            if (state.canLoadMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LaunchedEffect(state.products.size) { onIntent(HomeIntent.LoadMore) }
                    Box(Modifier.fillMaxWidth().height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(userName: String?, onSearch: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))))
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        if (!userName.isNullOrBlank()) {
            Text(
                stringResource(R.string.home_greeting, userName),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            onClick = onSearch,
            shape = RoundedCornerShape(50),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Search, null, tint = Color.Gray)
                Text(stringResource(UiR.string.label_search_hint), color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun PromoPager() {
    val pager = rememberPagerState { Banners.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(4_000)
            pager.animateScrollToPage((pager.currentPage + 1) % Banners.size)
        }
    }
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 12.dp) { page ->
            val banner = Banners[page]
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(banner.colors))
                    .padding(20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(banner.title), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(banner.subtitle), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.92f))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(Banners.size) { index ->
                val selected = pager.currentPage == index
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(if (selected) 18.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun DealsHeader() {
    val left by rememberTimeLeftToday()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.home_deals), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(8.dp), color = SaleRed) {
            Text(
                stringResource(R.string.home_ends_in, formatCountdown(left, Locale.getDefault())),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
