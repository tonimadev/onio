package com.ipirangatech.onio.feature.rewards.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.model.Wheel
import com.ipirangatech.onio.core.model.WheelPrize
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.component.CoinBalance
import com.ipirangatech.onio.core.ui.component.CoinIcon
import com.ipirangatech.onio.core.ui.component.CoinRain
import com.ipirangatech.onio.core.ui.component.LocalCurrencyOverride
import com.ipirangatech.onio.core.ui.rememberMoneyFormatter
import com.ipirangatech.onio.core.ui.component.OnioButton
import com.ipirangatech.onio.core.ui.component.OnioTopBar
import com.ipirangatech.onio.core.ui.component.CouponTicket
import com.ipirangatech.onio.core.ui.component.TicketState
import com.ipirangatech.onio.core.ui.formatCountdown
import com.ipirangatech.onio.core.ui.motion.AnimatedCountText
import com.ipirangatech.onio.core.ui.motion.OnioMotion
import com.ipirangatech.onio.core.ui.motion.confetti
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WheelRoute(viewModel: WheelViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(WheelIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            WheelEffect.NavigateBack -> onBack()
        }
    }
    WheelScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

private val SegmentColors = listOf(Color(0xFFF23F5D), Color(0xFFFFB84D), Color(0xFF7A1F4B), Color(0xFF00A890))
private val SegmentTextColors = listOf(Color.White, Color(0xFF4A1226), Color.White, Color.White)
private val ConfettiColors = listOf(Color(0xFFF23F5D), Color(0xFFFFC83D), Color(0xFF00A890), Color.White, Color(0xFFFF7A59))

@Composable
internal fun WheelScreen(state: WheelState, onIntent: (WheelIntent) -> Unit, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val sweep = 360f / Wheel.segments.size

    // The prize is already decided; the animation only has to land on it.
    LaunchedEffect(state.pending) {
        val pending = state.pending ?: return@LaunchedEffect
        val jitter = (Random.nextFloat() - 0.5f) * sweep * 0.6f
        val target = Wheel.targetRotation(rotation.value, pending.segmentIndex, extraTurns = 6, jitter = jitter)
        rotation.animateTo(target, tween(5_600, easing = CubicBezierEasing(0.12f, 0.75f, 0.1f, 1f)))
        onIntent(WheelIntent.SpinFinished)
    }
    // One haptic "tick" every time a segment boundary passes the pointer.
    LaunchedEffect(Unit) {
        var last = floor(rotation.value / sweep).toInt()
        snapshotFlow { floor(rotation.value / sweep).toInt() }.collect { current ->
            if (current != last) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            last = current
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OnioTopBar(
                title = { Text(stringResource(R.string.wheel_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(WheelIntent.BackClicked) }, enabled = !state.isSpinning) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
                actions = { CoinBalance(state.wallet.coins, Modifier.padding(end = Dimens.Space12)) },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(Dimens.Space16),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.Space16, Alignment.CenterVertically),
            ) {
                Text(
                    pluralStringResource(R.plurals.rewards_spins, state.wallet.spins, state.wallet.spins),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnioTheme.colors.priceText,
                )
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val size = min(maxWidth.value, 360f).dp
                    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                        WheelDisc(rotation = rotation.value, modifier = Modifier.fillMaxSize())
                        SpinHub(
                            enabled = state.wallet.spins > 0 && !state.isSpinning,
                            onClick = { onIntent(WheelIntent.Spin) },
                        )
                        Pointer(Modifier.align(Alignment.TopCenter))
                    }
                }
                if (state.wallet.spins <= 0 && !state.isSpinning) NoSpins()
            }
            AnimatedVisibility(
                visible = state.result != null,
                enter = fadeIn(tween(OnioMotion.Medium)),
                exit = fadeOut(tween(OnioMotion.Short)),
            ) {
                state.result?.let { result -> ResultOverlay(result.prize, result.coupon, onCollect = { onIntent(WheelIntent.DismissResult) }) }
            }
        }
    }
}

@Composable
private fun NoSpins() {
    val left by rememberTimeLeftToday()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.wheel_no_spins), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.wheel_no_spins_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.wheel_next_spin_in, formatCountdown(left, Locale.getDefault())),
            style = MaterialTheme.typography.labelLarge,
            color = OnioTheme.colors.priceText,
        )
    }
}

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
private fun WheelDisc(rotation: Float, modifier: Modifier) {
    val segments = Wheel.segments
    val sweep = 360f / segments.size
    val bulbs = rememberInfiniteTransition(label = "bulbs")
    val bulbPhase by bulbs.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bulbPhase")
    Box(modifier.aspectRatio(1f)) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                segments.indices.forEach { i ->
                    drawArc(
                        color = SegmentColors[i % SegmentColors.size],
                        startAngle = -90f + sweep * i,
                        sweepAngle = sweep,
                        useCenter = true,
                    )
                    // Thin separators between segments.
                    val a = Math.toRadians((-90f + sweep * i).toDouble())
                    val r = size.minDimension / 2
                    drawLine(
                        Color.White.copy(alpha = 0.55f),
                        Offset(center.x, center.y),
                        Offset(center.x + (cos(a) * r).toFloat(), center.y + (sin(a) * r).toFloat()),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
            segments.forEachIndexed { i, segment ->
                Box(Modifier.fillMaxSize().padding(10.dp).rotate(sweep * i + sweep / 2)) {
                    SegmentLabel(segment.prize, SegmentTextColors[i % SegmentTextColors.size], Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
                }
            }
        }
        // Static rim with blinking bulbs (this is what makes it feel like a game show).
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            drawCircle(Color(0xFF4A1226), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
            val count = 20
            repeat(count) { i ->
                val angle = Math.toRadians((360.0 / count) * i - 90)
                val r = size.minDimension / 2 - stroke / 2
                val on = (i % 2 == 0) == (bulbPhase > 0.5f)
                drawCircle(
                    if (on) Color(0xFFFFE08A) else Color(0xFFFFC83D).copy(alpha = 0.45f),
                    radius = 2.6.dp.toPx(),
                    center = Offset(center.x + (cos(angle) * r).toFloat(), center.y + (sin(angle) * r).toFloat()),
                )
            }
        }
    }
}

@Composable
private fun SegmentLabel(prize: WheelPrize, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (prize) {
            is WheelPrize.Coins -> {
                Text("+" + NumberFormat.getIntegerInstance().format(prize.amount), color = color, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                CoinIcon(size = 18.dp)
            }
            is WheelPrize.Discount -> {
                val t = prize.template
                val text = if (t.kind == com.ipirangatech.onio.core.model.DiscountKind.PERCENT) {
                    stringResource(UiR.string.coupon_percent_off, t.value)
                } else {
                    stringResource(UiR.string.coupon_amount_off, rememberMoneyFormatter(LocalCurrencyOverride.current).format(com.ipirangatech.onio.core.model.Money.usd(t.value.toLong())))
                }
                Text(text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
            }
        }
    }
}

@Composable
private fun SpinHub(enabled: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "hub")
    val scale by pulse.animateFloat(1f, 1.07f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "hubScale")
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        shadowElevation = 6.dp,
        modifier = Modifier.size(78.dp).graphicsLayer { if (enabled) { scaleX = scale; scaleY = scale } },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.wheel_spin), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun Pointer(modifier: Modifier) {
    Canvas(modifier.size(width = 30.dp, height = 36.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2, size.height)
            close()
        }
        drawPath(path, Color(0xFF222222))
        drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(size.width / 2, 7.dp.toPx()))
    }
}

@Composable
private fun ResultOverlay(prize: WheelPrize, coupon: com.ipirangatech.onio.core.model.WalletCoupon?, onCollect: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {}
            .confetti(prize, ConfettiColors, particles = 56),
        contentAlignment = Alignment.Center,
    ) {
        val scale = remember { Animatable(0.6f) }
        LaunchedEffect(Unit) { scale.animateTo(1f, OnioMotion.bouncy()) }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(Dimens.Space24).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        ) {
            Column(
                Modifier.padding(Dimens.Space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
            ) {
                Text(stringResource(R.string.wheel_win_title), style = MaterialTheme.typography.headlineMedium, color = OnioTheme.colors.priceText)
                when (prize) {
                    is WheelPrize.Coins -> {
                        CoinIcon(size = 72.dp)
                        val shown = androidx.compose.animation.core.animateIntAsState(prize.amount, tween(1_000), label = "won").value
                        AnimatedCountText(
                            "+" + NumberFormat.getIntegerInstance().format(shown),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    is WheelPrize.Discount -> if (coupon != null) {
                        Text(stringResource(R.string.wheel_win_coupon), style = MaterialTheme.typography.titleMedium)
                        CouponTicket(coupon, TicketState.AVAILABLE, Modifier.width(280.dp))
                    }
                }
                OnioButton(onClick = onCollect, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wheel_collect)) }
            }
        }
        if (prize is WheelPrize.Coins) CoinRain(trigger = prize, modifier = Modifier.fillMaxSize(), coins = 34)
    }
}
