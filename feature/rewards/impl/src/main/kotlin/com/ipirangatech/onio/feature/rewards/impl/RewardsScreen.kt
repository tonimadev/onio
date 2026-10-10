package com.ipirangatech.onio.feature.rewards.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.model.RewardsRepository
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.component.CoinBalance
import com.ipirangatech.onio.core.ui.component.CoinIcon
import com.ipirangatech.onio.core.ui.component.CoinRain
import com.ipirangatech.onio.core.ui.component.CouponTicket
import com.ipirangatech.onio.core.ui.component.OnioButton
import com.ipirangatech.onio.core.ui.component.OnioTopBar
import com.ipirangatech.onio.core.ui.component.SectionCard
import com.ipirangatech.onio.core.ui.component.StatPill
import com.ipirangatech.onio.core.ui.component.TicketState
import com.ipirangatech.onio.core.ui.motion.staggeredEntrance
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat

@Composable
fun RewardsRoute(
    viewModel: RewardsViewModel,
    onBack: () -> Unit,
    onOpenWheel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(RewardsIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            RewardsEffect.NavigateToWheel -> onOpenWheel()
            RewardsEffect.NavigateBack -> onBack()
        }
    }
    RewardsScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun RewardsScreen(state: RewardsState, onIntent: (RewardsIntent) -> Unit, modifier: Modifier = Modifier) {
    // The check-in celebration dismisses itself.
    LaunchedEffect(state.lastCheckIn) {
        if (state.lastCheckIn != null) {
            delay(2_600)
            onIntent(RewardsIntent.DismissCheckIn)
        }
    }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OnioTopBar(
                title = { Text(stringResource(R.string.rewards_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(RewardsIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(Dimens.Space8),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            ) {
                item { BalanceHero(state, Modifier.staggeredEntrance(0)) }
                item { CheckInCard(state, onIntent, Modifier.staggeredEntrance(1)) }
                item { WheelCard(state, onIntent, Modifier.staggeredEntrance(2)) }
                item { CouponsHeader(state, onIntent, Modifier.staggeredEntrance(3)) }
                if (state.coupons.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.rewards_coupons_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(Dimens.Space16),
                        )
                    }
                }
                items(state.coupons, key = { it.id }) { coupon ->
                    CouponTicket(coupon, if (coupon.usedAtMillis != null) TicketState.USED else TicketState.AVAILABLE)
                }
            }
            if (state.lastCheckIn != null) CoinRain(trigger = state.lastCheckIn, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BalanceHero(state: RewardsState, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(OnioTheme.colors.headerStart, OnioTheme.colors.headerEnd)), MaterialTheme.shapes.small),
    ) {
        Row(Modifier.fillMaxWidth().padding(Dimens.Space16), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.rewards_balance_label), color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelLarge)
                CoinBalance(state.wallet.coins, style = MaterialTheme.typography.displaySmall, color = Color.White, iconSize = 34.dp)
            }
            StatPill(
                pluralStringResource(R.plurals.rewards_spins, state.wallet.spins, state.wallet.spins),
                leading = { Icon(Icons.Filled.Casino, null, tint = Color.White, modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

@Composable
private fun CheckInCard(state: RewardsState, onIntent: (RewardsIntent) -> Unit, modifier: Modifier) {
    SectionCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            Text(stringResource(R.string.rewards_checkin_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RewardsRepository.CHECK_IN_COINS.forEachIndexed { index, coins ->
                    DayTile(
                        day = index + 1,
                        coins = coins,
                        collected = index < state.daysCollected,
                        isNext = !state.wallet.checkedInToday && index == state.daysCollected,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OnioButton(
                onClick = { onIntent(RewardsIntent.CheckIn) },
                enabled = !state.wallet.checkedInToday,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val result = state.lastCheckIn
                Text(
                    when {
                        result != null -> pluralStringResource(R.plurals.rewards_coin_gain, result.coins, result.coins)
                        state.wallet.checkedInToday -> stringResource(R.string.rewards_checkin_done)
                        else -> stringResource(R.string.rewards_checkin_button)
                    },
                )
            }
        }
    }
}

@Composable
private fun DayTile(day: Int, coins: Int, collected: Boolean, isNext: Boolean, modifier: Modifier) {
    val border = when {
        isNext -> MaterialTheme.colorScheme.primary
        collected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> OnioTheme.colors.border
    }
    Column(
        modifier
            .height(78.dp)
            .background(if (collected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .border(if (isNext) 1.5.dp else 1.dp, border, MaterialTheme.shapes.small)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(NumberFormat.getIntegerInstance().format(day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (collected) {
            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        } else {
            CoinIcon(size = 22.dp)
        }
        Text("+" + NumberFormat.getIntegerInstance().format(coins), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WheelCard(state: RewardsState, onIntent: (RewardsIntent) -> Unit, modifier: Modifier) {
    SectionCard(modifier.fillMaxWidth(), onClick = { onIntent(RewardsIntent.WheelClicked) }) {
        Row(Modifier.padding(Dimens.Space12), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Casino, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.rewards_wheel_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.rewards_spins, state.wallet.spins, state.wallet.spins),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OnioButton(onClick = { onIntent(RewardsIntent.WheelClicked) }) { Text(stringResource(R.string.rewards_wheel_cta), maxLines = 1) }
        }
    }
}

@Composable
private fun CouponsHeader(state: RewardsState, onIntent: (RewardsIntent) -> Unit, modifier: Modifier) {
    SectionCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            Text(stringResource(R.string.rewards_coupons_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = state.code,
                    onValueChange = { onIntent(RewardsIntent.CodeChanged(it)) },
                    label = { Text(stringResource(R.string.rewards_code_hint)) },
                    singleLine = true,
                    isError = state.codeMessage == CodeMessage.UNKNOWN || state.codeMessage == CodeMessage.ALREADY_HAVE,
                    modifier = Modifier.weight(1f),
                )
                OnioButton(onClick = { onIntent(RewardsIntent.RedeemCode) }, enabled = state.code.isNotBlank(), modifier = Modifier.padding(top = 6.dp).height(52.dp)) {
                    Text(stringResource(R.string.rewards_code_add))
                }
            }
            state.codeMessage?.let { message ->
                Text(
                    stringResource(
                        when (message) {
                            CodeMessage.ADDED -> R.string.rewards_code_added
                            CodeMessage.UNKNOWN -> R.string.rewards_code_unknown
                            CodeMessage.ALREADY_HAVE -> R.string.rewards_code_have
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message == CodeMessage.ADDED) OnioTheme.colors.freeShipping else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
