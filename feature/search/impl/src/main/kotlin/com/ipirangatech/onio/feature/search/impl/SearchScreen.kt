package com.ipirangatech.onio.feature.search.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.categoryLabel
import com.ipirangatech.onio.core.ui.component.LocalCurrencyOverride
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.ProductCard
import com.ipirangatech.onio.core.ui.component.ProductCardSkeleton
import com.ipirangatech.onio.core.ui.rememberMoneyFormatter

@Composable
fun SearchRoute(
    viewModel: SearchViewModel,
    onOpenProduct: (Long) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val addedMessage = stringResource(UiR.string.msg_added_to_cart)
    val viewCart = stringResource(UiR.string.action_view_cart)
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(SearchIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            is SearchEffect.NavigateToProduct -> onOpenProduct(effect.productId)
            SearchEffect.NavigateToCart -> onOpenCart()
            SearchEffect.ShowAdded -> {
                snackbar.currentSnackbarData?.dismiss()
                if (snackbar.showSnackbar(addedMessage, actionLabel = viewCart) == SnackbarResult.ActionPerformed) {
                    viewModel.onIntent(SearchIntent.ViewCartClicked)
                }
            }
        }
    }
    SearchScreen(state = state, snackbar = snackbar, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    state: SearchState,
    snackbar: SnackbarHostState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.search_title)) }) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(SearchIntent.QueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(key = "filters") {
                    FilterChip(
                        selected = state.filterPanelOpen || state.filters.activeCount > 0,
                        onClick = { onIntent(SearchIntent.ToggleFilterPanel) },
                        label = { Text(stringResource(R.string.search_filters)) },
                        leadingIcon = {
                            BadgedBox(badge = { if (state.filters.activeCount > 0) Badge { Text(state.filters.activeCount.toString()) } }) {
                                Icon(Icons.Filled.Tune, null)
                            }
                        },
                    )
                }
                item(key = "favorites") {
                    FilterChip(
                        selected = state.favoritesOnly,
                        onClick = { onIntent(SearchIntent.ToggleFavoritesOnly) },
                        label = { Text(stringResource(R.string.search_favorites)) },
                        leadingIcon = {
                            Icon(if (state.favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null)
                        },
                    )
                }
                items(state.categories, key = UiFilter::slug) { category ->
                    FilterChip(
                        selected = state.categorySlug == category.slug && !state.favoritesOnly,
                        onClick = {
                            onIntent(SearchIntent.CategorySelected(if (state.categorySlug == category.slug) null else category.slug))
                        },
                        label = { Text(categoryLabel(category.slug, category.name)) },
                    )
                }
            }
            AnimatedVisibility(state.filterPanelOpen) { FilterPanel(state, onIntent) }
            when {
                state.isLoading && !state.favoritesOnly -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { items(6) { ProductCardSkeleton() } }
                state.hasError -> MessageState(
                    stringResource(UiR.string.error_generic),
                    actionLabel = stringResource(UiR.string.action_retry),
                    onAction = { onIntent(SearchIntent.Retry) },
                )
                state.results.isEmpty() -> MessageState(
                    if (state.favoritesOnly && state.filters.activeCount == 0 && state.query.isBlank()) stringResource(R.string.search_no_favorites)
                    else stringResource(UiR.string.label_empty_results),
                    actionLabel = if (state.filters.activeCount > 0) stringResource(R.string.filter_reset) else null,
                    onAction = if (state.filters.activeCount > 0) ({ onIntent(SearchIntent.ResetFilters) }) else null,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.results, key = UiResult::id) { result ->
                        ProductCard(
                            title = result.title,
                            thumbnailUrl = result.thumbnailUrl,
                            price = result.price,
                            originalPrice = result.originalPrice,
                            rating = result.rating,
                            discountPercent = result.discountPercent,
                            onClick = { onIntent(SearchIntent.ProductClicked(result.id)) },
                            onAddToCart = if (state.favoritesOnly) null else ({ onIntent(SearchIntent.AddToCart(result.id)) }),
                            isFavorite = result.id in state.favoriteIds,
                            onToggleFavorite = { onIntent(SearchIntent.ToggleFavorite(result.id)) },
                        )
                    }
                }
            }
        }
    }
}

private val SortLabels = mapOf(
    SortOption.RELEVANCE to R.string.sort_relevance,
    SortOption.PRICE_ASC to R.string.sort_price_asc,
    SortOption.PRICE_DESC to R.string.sort_price_desc,
    SortOption.DISCOUNT to R.string.sort_discount,
    SortOption.RATING to R.string.sort_rating,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.search_sort), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOption.entries.forEach { option ->
                FilterChip(
                    selected = state.filters.sort == option,
                    onClick = { onIntent(SearchIntent.SortSelected(option)) },
                    label = { Text(stringResource(SortLabels.getValue(option))) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filters.onSaleOnly,
                onClick = { onIntent(SearchIntent.ToggleOnSale) },
                label = { Text(stringResource(R.string.filter_on_sale)) },
            )
            FilterChip(
                selected = state.filters.topRatedOnly,
                onClick = { onIntent(SearchIntent.ToggleTopRated) },
                label = { Text(stringResource(R.string.filter_top_rated)) },
            )
        }
        val bounds = state.priceBounds
        if (bounds != null && bounds.last > bounds.first) PriceRange(state, bounds, onIntent)
        TextButton(onClick = { onIntent(SearchIntent.ResetFilters) }, enabled = state.filters.activeCount > 0) {
            Text(stringResource(R.string.filter_reset))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceRange(state: SearchState, bounds: LongRange, onIntent: (SearchIntent) -> Unit) {
    val formatter = rememberMoneyFormatter(LocalCurrencyOverride.current)
    val low = (state.filters.minPriceCents ?: bounds.first).coerceIn(bounds.first, bounds.last)
    val high = (state.filters.maxPriceCents ?: bounds.last).coerceIn(low, bounds.last)
    Text(stringResource(R.string.filter_price_range), style = MaterialTheme.typography.titleSmall)
    RangeSlider(
        value = low.toFloat()..high.toFloat(),
        onValueChange = { range -> onIntent(SearchIntent.PriceRangeChanged(range.start.toLong(), range.endInclusive.toLong())) },
        valueRange = bounds.first.toFloat()..bounds.last.toFloat(),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatter.format(Money.usd(low)), style = MaterialTheme.typography.labelMedium)
        Text(formatter.format(Money.usd(high)), style = MaterialTheme.typography.labelMedium)
    }
}
