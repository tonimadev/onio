package com.ipirangatech.onio.feature.cart.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.vector.ImageVector
import com.ipirangatech.onio.core.ui.component.OnioButton
import com.ipirangatech.onio.core.ui.component.OnioTopBar
import com.ipirangatech.onio.core.ui.component.SectionCard
import com.ipirangatech.onio.core.ui.motion.AnimatedCountText
import com.ipirangatech.onio.core.ui.motion.animateMoneyAsState
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import androidx.compose.ui.text.style.TextOverflow

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

@Composable
internal fun CartScreen(state: CartState, onIntent: (CartIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { OnioTopBar(title = { Text(stringResource(R.string.cart_title)) }) },
        bottomBar = {
            AnimatedVisibility(
                visible = state.items.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = Dimens.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = Dimens.Space8)) {
                            Text(stringResource(R.string.cart_subtotal), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PriceText(animateMoneyAsState(state.subtotal), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.cart_shipping) + ": " + stringResource(R.string.cart_shipping_free),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnioTheme.colors.freeShipping,
                            )
                        }
                        OnioButton(
                            onClick = { onIntent(CartIntent.CheckoutClicked) },
                            modifier = Modifier.padding(Dimens.Space8).height(48.dp),
                        ) { Text(stringResource(R.string.cart_checkout), maxLines = 1) }
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
                contentPadding = PaddingValues(Dimens.Space8),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            ) {
                items(state.items, key = UiCartItem::productId) { item ->
                    CartRow(item, onIntent, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun CartRow(item: UiCartItem, onIntent: (CartIntent) -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier.fillMaxWidth(), onClick = { onIntent(CartIntent.OpenProduct(item.productId)) }) {
        Row(Modifier.padding(Dimens.Space8), horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            ProductImage(item.thumbnailUrl, Modifier.size(88.dp).clip(MaterialTheme.shapes.extraSmall).background(Color.White))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onIntent(CartIntent.Remove(item.productId)) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.cart_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceText(item.lineTotal, modifier = Modifier.weight(1f))
                    QuantityStepper(item, onIntent)
                }
            }
        }
    }
}

/** Rectangular "- 2 +" control with a hairline border, the way marketplace carts draw it. */
@Composable
private fun QuantityStepper(item: UiCartItem, onIntent: (CartIntent) -> Unit) {
    Row(
        Modifier.border(Dimens.Hairline, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Filled.Remove, stringResource(R.string.cart_decrease)) { onIntent(CartIntent.Decrement(item.productId)) }
        Box(
            Modifier.width(34.dp).height(28.dp).border(Dimens.Hairline, MaterialTheme.colorScheme.outlineVariant),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedCountText(NumberFormat.getIntegerInstance().format(item.quantity), style = MaterialTheme.typography.labelLarge)
        }
        StepButton(Icons.Filled.Add, stringResource(R.string.cart_increase)) { onIntent(CartIntent.Increment(item.productId)) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(icon, description, modifier = Modifier.size(16.dp))
    }
}
