package com.ipirangatech.onio.feature.rewards.impl

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.component.CoinIcon
import com.ipirangatech.onio.core.ui.component.CoinRain
import com.ipirangatech.onio.core.ui.component.OnioButton
import com.ipirangatech.onio.core.ui.component.OnioOutlinedButton
import com.ipirangatech.onio.core.ui.component.PriceText
import com.ipirangatech.onio.core.ui.component.SectionCard
import com.ipirangatech.onio.core.ui.motion.AnimatedCheckmark
import com.ipirangatech.onio.core.ui.motion.OnioMotion
import com.ipirangatech.onio.core.ui.motion.confetti
import com.ipirangatech.onio.core.ui.motion.staggeredEntrance
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat

@Composable
fun OrderPlacedRoute(
    viewModel: OrderPlacedViewModel,
    onSpin: () -> Unit,
    onViewOrder: (Long) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(OrderPlacedIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            OrderPlacedEffect.NavigateToWheel -> onSpin()
            is OrderPlacedEffect.NavigateToOrder -> onViewOrder(effect.orderId)
            OrderPlacedEffect.ContinueShopping -> onContinue()
        }
    }
    OrderPlacedScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

private val ConfettiColors = listOf(Color(0xFFF23F5D), Color(0xFFFFC83D), Color(0xFF00A890), Color(0xFFFF7A59), Color(0xFF7A1F4B))

/** The "you did it" moment: confetti, a check that draws itself, a coin shower and the reward breakdown. */
@Composable
internal fun OrderPlacedScreen(state: OrderPlacedState, onIntent: (OrderPlacedIntent) -> Unit, modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(450)
        started = true
    }
    val coinsShown by animateIntAsState(if (started) state.coins else 0, tween(1_300), label = "coins")
    val badgeScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { badgeScale.animateTo(1f, OnioMotion.bouncy()) }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .confetti(Unit, ConfettiColors, particles = 64),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.Space16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(124.dp)
                    .graphicsLayer { scaleX = badgeScale.value; scaleY = badgeScale.value }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(OnioTheme.colors.headerStart, OnioTheme.colors.headerEnd))),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedCheckmark(Color.White, Modifier.size(78.dp), strokeWidth = 12f)
            }
            Text(
                stringResource(R.string.placed_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.staggeredEntrance(1),
            )
            state.orderNumber?.let { number ->
                Row(
                    Modifier.staggeredEntrance(2),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.placed_order, number), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.total?.let { PriceText(it, style = MaterialTheme.typography.titleMedium) }
                }
            }
            SectionCard(Modifier.fillMaxWidth().staggeredEntrance(3)) {
                Column(
                    Modifier.fillMaxWidth().padding(Dimens.Space16),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space8),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                        CoinIcon(size = 40.dp)
                        Text(
                            pluralStringResource(R.plurals.rewards_coin_gain, state.coins, coinsShown),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnioTheme.colors.priceText,
                        )
                    }
                    if (state.spinsEarned > 0) {
                        Text(
                            stringResource(R.string.placed_earned_spin),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        OnioButton(onClick = { onIntent(OrderPlacedIntent.SpinNow) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.placed_spin_now))
                        }
                    }
                }
            }
            OnioOutlinedButton(
                onClick = { onIntent(OrderPlacedIntent.ViewOrder) },
                modifier = Modifier.fillMaxWidth().staggeredEntrance(4),
            ) { Text(stringResource(R.string.placed_view_order)) }
            TextButton(onClick = { onIntent(OrderPlacedIntent.Continue) }, modifier = Modifier.navigationBarsPadding().staggeredEntrance(5)) {
                Text(stringResource(R.string.placed_continue))
            }
        }
        CoinRain(trigger = Unit, modifier = Modifier.fillMaxSize(), coins = 40, durationMillis = 3_000)
    }
}
