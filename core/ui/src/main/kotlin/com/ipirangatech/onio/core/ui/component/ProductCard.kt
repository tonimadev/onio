package com.ipirangatech.onio.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.ui.R
import com.ipirangatech.onio.core.ui.motion.pressScale
import com.ipirangatech.onio.core.ui.rememberMoneyFormatter
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import java.text.NumberFormat

/** User-chosen currency (null = derive from locale). Provided once by the app's composition root. */
val LocalCurrencyOverride = compositionLocalOf<String?> { null }

private val HeartRed = Color(0xFFE53935)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceText(
    price: Money,
    modifier: Modifier = Modifier,
    originalPrice: Money? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = OnioTheme.colors.priceText,
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
        Icon(Icons.Filled.Star, contentDescription = null, tint = OnioTheme.colors.star, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 2.dp))
    }
}

/** Yellow "-10%" tag in the corner of a product image. */
@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    if (percent <= 0) return
    Surface(modifier = modifier, color = OnioTheme.colors.saleTagBackground, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            // LRI..PDI keeps "-19%" in this order inside right-to-left text (otherwise it renders as "19%-").
            "\u2066-" + NumberFormat.getIntegerInstance().format(percent) + "%\u2069",
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = OnioTheme.colors.saleTagText,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Small outlined "Free shipping" tag in teal. */
@Composable
fun FreeShippingTag(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        border = BorderStroke(Dimens.Hairline, OnioTheme.colors.freeShipping),
    ) {
        Text(
            stringResource(R.string.label_free_shipping),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = OnioTheme.colors.freeShipping,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().pressScale(source, pressedScale = 0.97f),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.Hairline, OnioTheme.colors.border),
        interactionSource = source,
    ) {
        Column {
            Box {
                ProductImage(url = thumbnailUrl, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                DiscountBadge(discountPercent, Modifier.align(Alignment.TopEnd))
                if (onToggleFavorite != null) {
                    FavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopStart).padding(6.dp))
                }
            }
            Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize.times(1.08f)),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    PriceText(price, originalPrice = originalPrice, modifier = Modifier.weight(1f))
                    if (onAddToCart != null) QuickAddButton(onAddToCart)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RatingBadge(rating)
                    FreeShippingTag()
                }
            }
        }
    }
}

/** Small square cart button on product cards (outlined in the brand color). */
@Composable
private fun QuickAddButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier.size(30.dp).pressScale(source, pressedScale = 0.88f),
        interactionSource = source,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.AddShoppingCart,
                contentDescription = stringResource(R.string.action_add_to_cart),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
fun ProvideCurrency(override: String?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCurrencyOverride provides override, content = content)
}

/** Heart toggle used on product cards and the product page. */
@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(30.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.9f)) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(if (isFavorite) R.string.fav_remove else R.string.fav_add),
                tint = if (isFavorite) HeartRed else Color(0xFF666666),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Pulsing grey placeholder used while content loads (feels faster than a spinner). */
@Composable
fun Modifier.shimmerBackground(shape: Shape = MaterialTheme.shapes.small): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val base = MaterialTheme.colorScheme.outlineVariant
    return this.clip(shape).background(base.copy(alpha = alpha.value))
}

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).shimmerBackground(MaterialTheme.shapes.extraSmall))
        Box(Modifier.fillMaxWidth(0.9f).height(12.dp).shimmerBackground())
        Box(Modifier.fillMaxWidth(0.5f).height(12.dp).shimmerBackground())
        Box(Modifier.width(80.dp).height(16.dp).shimmerBackground())
    }
}
