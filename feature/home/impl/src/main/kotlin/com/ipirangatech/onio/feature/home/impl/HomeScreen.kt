package com.ipirangatech.onio.feature.home.impl

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.categoryIcon
import com.ipirangatech.onio.core.ui.categoryLabel
import com.ipirangatech.onio.core.ui.component.FlashCountdown
import com.ipirangatech.onio.core.ui.component.IconTile
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.ProductCard
import com.ipirangatech.onio.core.ui.component.ProductCardSkeleton
import com.ipirangatech.onio.core.ui.component.StatusBarIcons
import com.ipirangatech.onio.core.ui.motion.staggeredEntrance
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.absoluteValue

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

private data class Banner(val title: Int, val subtitle: Int, val start: Color, val end: Color)

private val Banners = listOf(
    Banner(R.string.home_banner1_title, R.string.home_banner1_sub, Color(0xFFFF7A59), Color(0xFFF23F5D)),
    Banner(R.string.home_banner2_title, R.string.home_banner2_sub, Color(0xFFFFB84D), Color(0xFFFF7A59)),
    Banner(R.string.home_banner3_title, R.string.home_banner3_sub, Color(0xFF00A890), Color(0xFF007A8A)),
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
        containerColor = MaterialTheme.colorScheme.background,
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
            horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header(userName = state.userName, onSearch = { onIntent(HomeIntent.SearchClicked) }) }
            item(span = { GridItemSpan(maxLineSpan) }) { PromoPager() }
            if (state.isLoading) {
                items(6) { ProductCardSkeleton(Modifier.padding(horizontal = Dimens.Space8)) }
                return@LazyVerticalGrid
            }
            item(span = { GridItemSpan(maxLineSpan) }) { CategoryStrip(state, onIntent) }
            if (state.deals.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { FlashSale(state, onIntent) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.home_recommended)) }
            items(state.products.size, key = { state.products[it].id }) { index ->
                val product = state.products[index]
                ProductCard(
                    title = product.title,
                    thumbnailUrl = product.thumbnailUrl,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    rating = product.rating,
                    discountPercent = product.discountPercent,
                    onClick = { onIntent(HomeIntent.ProductClicked(product.id)) },
                    onAddToCart = { onIntent(HomeIntent.AddToCart(product.id)) },
                    isFavorite = product.id in state.favoriteIds,
                    onToggleFavorite = { onIntent(HomeIntent.ToggleFavorite(product.id)) },
                    modifier = Modifier.padding(horizontal = Dimens.Space8).staggeredEntrance(index % 6),
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
    StatusBarIcons(light = true)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(OnioTheme.colors.headerStart, OnioTheme.colors.headerEnd)))
            .statusBarsPadding()
            .padding(start = Dimens.Space12, end = Dimens.Space12, top = Dimens.Space8, bottom = Dimens.Space12),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space8),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            Text(
                stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                color = Color.White,
            )
            if (!userName.isNullOrBlank()) {
                Text(
                    stringResource(R.string.home_greeting, userName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Surface(
            onClick = onSearch,
            shape = MaterialTheme.shapes.small,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Row(
                Modifier.padding(horizontal = Dimens.Space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
            ) {
                Text(
                    stringResource(UiR.string.label_search_hint),
                    color = Color(0xFF8A8A8A),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
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
    Box(Modifier.fillMaxWidth()) {
        HorizontalPager(
            pager,
            contentPadding = PaddingValues(horizontal = Dimens.Space12),
            pageSpacing = Dimens.Space8,
        ) { page ->
            val banner = Banners[page]
            // Side banners shrink and fade a little: a cheap carousel/parallax feel.
            val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            Box(
                Modifier
                    .graphicsLayer {
                        val scale = 1f - 0.06f * offset
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.35f * offset
                    }
                    .fillMaxWidth()
                    .height(124.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Brush.linearGradient(listOf(banner.start, banner.end)))
                    .padding(Dimens.Space16),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(banner.title), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(banner.subtitle), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.95f))
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
            repeat(Banners.size) { index ->
                val selected = pager.currentPage == index
                val width by androidx.compose.animation.core.animateDpAsState(if (selected) 14.dp else 5.dp, label = "dot")
                Box(
                    Modifier
                        .padding(2.dp)
                        .size(width, 5.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

@Composable
private fun CategoryStrip(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.Space12, vertical = Dimens.Space12),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            items(state.categories.size, key = { state.categories[it].slug }) { index ->
                val category = state.categories[index]
                Column(
                    Modifier
                        .width(64.dp)
                        .clickable { onIntent(HomeIntent.CategoryClicked(category.slug)) }
                        .staggeredEntrance(index),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconTile { Icon(categoryIcon(category.slug), null, tint = MaterialTheme.colorScheme.primary) }
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
}

@Composable
private fun FlashSale(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val left by rememberTimeLeftToday()
    val pulse = rememberInfiniteTransition(label = "bolt")
    val scale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "boltScale",
    )
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(vertical = Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.Space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
            ) {
                Icon(
                    Icons.Filled.FlashOn,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                )
                Text(
                    stringResource(R.string.home_deals),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = OnioTheme.colors.priceText,
                )
                Spacer(Modifier.weight(1f))
                FlashCountdown(left)
            }
            LazyRow(contentPadding = PaddingValues(horizontal = Dimens.Space12), horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
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
                        isFavorite = product.id in state.favoriteIds,
                        onToggleFavorite = { onIntent(HomeIntent.ToggleFavorite(product.id)) },
                        modifier = Modifier.width(152.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space12), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(3.dp, 16.dp).background(MaterialTheme.colorScheme.primary))
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = Dimens.Space8),
            )
        }
    }
}
