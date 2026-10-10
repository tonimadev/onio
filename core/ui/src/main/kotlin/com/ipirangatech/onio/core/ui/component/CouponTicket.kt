package com.ipirangatech.onio.core.ui.component

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ipirangatech.onio.core.model.DiscountKind
import com.ipirangatech.onio.core.model.Money
import com.ipirangatech.onio.core.model.WalletCoupon
import com.ipirangatech.onio.core.ui.R
import com.ipirangatech.onio.core.ui.rememberMoneyFormatter
import com.ipirangatech.onio.core.ui.theme.OnioTheme

/** Rounded rectangle with a half-circle bite at the top and bottom of the perforation line. */
class TicketShape(private val stubWidth: Dp, private val notch: Dp = 7.dp, private val corner: Dp = 4.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val stub = with(density) { stubWidth.toPx() }
        val r = with(density) { notch.toPx() }
        val c = with(density) { corner.toPx() }
        val x = if (layoutDirection == LayoutDirection.Ltr) stub else size.width - stub
        val body = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), androidx.compose.ui.geometry.CornerRadius(c, c))) }
        val bites = Path().apply {
            addOval(Rect(Offset(x - r, -r), Size(r * 2, r * 2)))
            addOval(Rect(Offset(x - r, size.height - r), Size(r * 2, r * 2)))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, bites))
    }
}

enum class TicketState { AVAILABLE, SELECTED, DISABLED, USED }

@Composable
fun couponHeadline(coupon: WalletCoupon): String = when (coupon.kind) {
    DiscountKind.PERCENT -> stringResource(R.string.coupon_percent_off, coupon.value)
    DiscountKind.AMOUNT -> stringResource(R.string.coupon_amount_off, rememberMoneyFormatter(LocalCurrencyOverride.current).format(Money.usd(coupon.value.toLong())))
}

@Composable
fun couponMinSpend(coupon: WalletCoupon): String? =
    if (coupon.minSubtotalCents > 0) {
        stringResource(R.string.coupon_min_spend, rememberMoneyFormatter(LocalCurrencyOverride.current).format(Money.usd(coupon.minSubtotalCents)))
    } else {
        null
    }

@Composable
fun couponExpiry(coupon: WalletCoupon): String {
    val context = LocalContext.current
    val date = DateUtils.formatDateTime(context, coupon.expiresAtMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    return stringResource(R.string.coupon_expires, date)
}

/** Voucher-style coupon: coloured stub with the discount on the left, code and conditions on the right. */
@Composable
fun CouponTicket(
    coupon: WalletCoupon,
    state: TicketState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val stub = 96.dp
    val shape = TicketShape(stub)
    val active = state == TicketState.AVAILABLE || state == TicketState.SELECTED
    val stubBrush = if (active) {
        Brush.linearGradient(listOf(OnioTheme.colors.headerStart, OnioTheme.colors.headerEnd))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
    }
    val borderColor = if (state == TicketState.SELECTED) MaterialTheme.colorScheme.primary else OnioTheme.colors.border
    val content: @Composable () -> Unit = {
        Row(Modifier.height(IntrinsicSize.Min).fillMaxWidth()) {
            Box(Modifier.width(stub).fillMaxHeight().background(stubBrush).padding(8.dp), contentAlignment = Alignment.Center) {
                Text(
                    couponHeadline(coupon),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(coupon.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                couponMinSpend(coupon)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (state == TicketState.USED) stringResource(R.string.coupon_used) else couponExpiry(coupon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onClick != null && state != TicketState.USED) {
                Box(Modifier.padding(end = 12.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (state == TicketState.SELECTED) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (state == TicketState.SELECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
    val base = modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface, shape).border(1.dp, borderColor, shape)
    if (onClick != null && state != TicketState.USED && state != TicketState.DISABLED) {
        Surface(onClick = onClick, modifier = base, shape = shape, color = MaterialTheme.colorScheme.surface) { content() }
    } else {
        Box(base.then(if (state == TicketState.DISABLED || state == TicketState.USED) Modifier else Modifier)) {
            Surface(shape = shape, color = Color.Transparent) { content() }
        }
    }
}

/** Small pill showing how many spins/coins etc. Used as a compact stat. */
@Composable
fun StatPill(label: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Surface(modifier, shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            leading?.invoke()
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
