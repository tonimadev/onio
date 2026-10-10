package com.ipirangatech.onio.feature.savings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ipirangatech.onio.core.ui.component.OnioOutlinedButton
import com.ipirangatech.onio.core.ui.component.OnioTopBar
import com.ipirangatech.onio.core.ui.motion.animateMoneyAsState
import com.ipirangatech.onio.core.ui.theme.OnioTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.component.LoadingBox
import com.ipirangatech.onio.core.ui.component.MessageState
import com.ipirangatech.onio.core.ui.component.PriceText
import com.ipirangatech.onio.core.ui.R as UiR

@Composable
fun SavingsRoute(
    viewModel: SavingsViewModel,
    onOpenSettings: () -> Unit,
    onOpenOrders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(SavingsIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            SavingsEffect.NavigateToSettings -> onOpenSettings()
            SavingsEffect.NavigateToOrders -> onOpenOrders()
        }
    }
    SavingsScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavingsScreen(state: SavingsState, onIntent: (SavingsIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OnioTopBar(
                title = { Text(stringResource(R.string.savings_title)) },
                actions = {
                    IconButton(onClick = { onIntent(SavingsIntent.SettingsClicked) }) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.savings_settings))
                    }
                },
            )
        },
    ) { padding ->
        val savings = state.savings
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            savings == null -> MessageState(stringResource(UiR.string.error_generic), Modifier.padding(padding))
            else -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(Brush.linearGradient(listOf(OnioTheme.colors.headerStart, OnioTheme.colors.headerEnd))),
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (state.userName.isNullOrBlank()) stringResource(R.string.savings_headline)
                            else stringResource(R.string.savings_headline_named, state.userName),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        PriceText(animateMoneyAsState(savings.total), style = MaterialTheme.typography.displaySmall, color = Color.White)
                        Text(
                            if (savings.cancelledOrders == 0) stringResource(R.string.savings_none_yet)
                            else pluralStringResource(R.plurals.savings_summary, savings.cancelledOrders, savings.cancelledOrders),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.95f),
                        )
                    }
                }
                if (savings.itemsNotBought > 0) {
                    Text(
                        pluralStringResource(R.plurals.savings_items_not_bought, savings.itemsNotBought, savings.itemsNotBought),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(stringResource(R.string.savings_motivation), style = MaterialTheme.typography.bodyMedium)
                OnioOutlinedButton(onClick = { onIntent(SavingsIntent.SeeOrdersClicked) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.savings_see_orders))
                }
            }
        }
    }
}
