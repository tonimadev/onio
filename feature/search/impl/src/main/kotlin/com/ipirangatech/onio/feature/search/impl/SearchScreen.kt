package com.ipirangatech.onio.feature.search.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.categoryLabel
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.ProductCard
import com.ipirangatech.onio.core.ui.component.ProductCardSkeleton

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
            ) {
                items(state.categories, key = UiFilter::slug) { category ->
                    FilterChip(
                        selected = state.categorySlug == category.slug,
                        onClick = {
                            onIntent(SearchIntent.CategorySelected(if (state.categorySlug == category.slug) null else category.slug))
                        },
                        label = { Text(categoryLabel(category.slug, category.name)) },
                    )
                }
            }
            when {
                state.isLoading -> LazyVerticalGrid(
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
                state.results.isEmpty() -> MessageState(stringResource(UiR.string.label_empty_results))
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
                            onAddToCart = { onIntent(SearchIntent.AddToCart(result.id)) },
                        )
                    }
                }
            }
        }
    }
}
