package com.ipirangatech.onio.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.ui.R
import com.ipirangatech.onio.core.ui.rememberMoneyFormatter
import java.text.NumberFormat

/** User-chosen currency (null = derive from locale). Provided once by the app's composition root. */
val LocalCurrencyOverride = compositionLocalOf<String?> { null }

val SaleRed = Color(0xFFE53935)
val StarYellow = Color(0xFFFFB400)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceText(
    price: Money,
    modifier: Modifier = Modifier,
    originalPrice: Money? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val formatter = rememberMoneyFormatter(LocalCurrencyOverride.current)
    // FlowRow moves the struck-through price to its own line instead of splitting the number.
    FlowRow(modifier, verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(formatter.format(price), style = style, fontWeight = FontWeight.Bold, color = color, softWrap = false)
        if (originalPrice != null && originalPrice.minorUnits > price.minorUnits) {
            Text(
                formatter.format(originalPrice),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
            )
        }
    }
}

@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    val text = NumberFormat.getNumberInstance().also {
        it.maximumFractionDigits = 1
        it.minimumFractionDigits = 1
    }.format(rating)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

/** Red "-10%" tag used on cards and the product page. */
@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    if (percent <= 0) return
    Surface(modifier = modifier, color = SaleRed, shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp)) {
        Text(
            "-" + NumberFormat.getIntegerInstance().format(percent) + "%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ProductCard(
    title: String,
    thumbnailUrl: String,
    price: Money,
    originalPrice: Money?,
    rating: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    discountPercent: Int = 0,
    onAddToCart: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
            ProductImage(
                url = thumbnailUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFF3F1F8))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            )
            DiscountBadge(discountPercent, Modifier.align(Alignment.TopStart))
            if (onToggleFavorite != null) {
                FavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            if (onAddToCart != null) {
                FilledIconButton(
                    onClick = onAddToCart,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(Icons.Filled.AddShoppingCart, stringResource(R.string.action_add_to_cart), Modifier.size(20.dp))
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, minLines = 2)
            PriceText(price, originalPrice = originalPrice)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingBadge(rating)
                Text(
                    stringResource(R.string.label_free_shipping),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ProvideCurrency(override: String?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCurrencyOverride provides override, content = content)
}

/** Pulsing grey placeholder used while content loads (feels faster than a spinner). */
@Composable
fun Modifier.shimmerBackground(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    return this.clip(shape).background(base.copy(alpha = alpha.value))
}

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).shimmerBackground(RoundedCornerShape(16.dp)))
        Box(Modifier.fillMaxWidth(0.9f).height(14.dp).shimmerBackground())
        Box(Modifier.fillMaxWidth(0.5f).height(14.dp).shimmerBackground())
        Box(Modifier.fillMaxWidth(0.4f).height(18.dp).shimmerBackground())
    }
}

@Composable
fun CircleIcon(modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.primaryContainer, content: @Composable () -> Unit) {
    Box(modifier.size(56.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) { content() }
}

/** Heart toggle used on product cards and the product page. */
@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(34.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.92f)) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(if (isFavorite) R.string.fav_remove else R.string.fav_add),
                tint = if (isFavorite) SaleRed else Color(0xFF666666),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
