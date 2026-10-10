package com.ipirangatech.onio.feature.cart.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.component.LoadingBox
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.PriceText
import com.ipirangatech.onio.core.ui.component.ProductImage
import java.text.NumberFormat

@Composable
fun CartRoute(
    viewModel: CartViewModel,
    onCheckout: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(CartIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            CartEffect.NavigateToCheckout -> onCheckout()
            is CartEffect.NavigateToProduct -> onOpenProduct(effect.productId)
        }
    }
    CartScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartScreen(state: CartState, onIntent: (CartIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.cart_title)) }) },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.cart_subtotal), style = MaterialTheme.typography.titleMedium)
                            PriceText(state.subtotal, style = MaterialTheme.typography.titleLarge)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.cart_shipping))
                            Text(stringResource(R.string.cart_shipping_free), color = MaterialTheme.colorScheme.tertiary)
                        }
                        Button(onClick = { onIntent(CartIntent.CheckoutClicked) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.cart_checkout))
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            state.items.isEmpty() -> MessageState(
                title = stringResource(R.string.cart_empty),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = UiCartItem::productId) { item ->
                    CartRow(item, onIntent)
                }
            }
        }
    }
}

@Composable
private fun CartRow(item: UiCartItem, onIntent: (CartIntent) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.clickable { onIntent(CartIntent.OpenProduct(item.productId)) }.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(item.thumbnailUrl, Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F1F8)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                PriceText(item.lineTotal)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onIntent(CartIntent.Decrement(item.productId)) }) {
                        Icon(Icons.Filled.Remove, stringResource(R.string.cart_decrease))
                    }
                    Text(NumberFormat.getIntegerInstance().format(item.quantity), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onIntent(CartIntent.Increment(item.productId)) }) {
                        Icon(Icons.Filled.Add, stringResource(R.string.cart_increase))
                    }
                }
            }
            IconButton(onClick = { onIntent(CartIntent.Remove(item.productId)) }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.cart_remove))
            }
        }
    }
}
