package com.ipirangatech.onio.feature.checkout.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ipirangatech.onio.core.model.CardBrand
import com.ipirangatech.onio.core.model.CardValidation
import com.ipirangatech.onio.core.ui.component.ProductImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.R as UiR
import com.ipirangatech.onio.core.ui.component.PriceText

@Composable
fun CheckoutRoute(
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
    onOrderPlaced: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(CheckoutIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            is CheckoutEffect.OrderPlaced -> onOrderPlaced(effect.orderId)
            CheckoutEffect.NavigateBack -> onBack()
        }
    }
    CheckoutScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CheckoutScreen(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkout_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CheckoutIntent.BackClicked) }, enabled = !state.isProcessing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryRow(state)
                Text(stringResource(R.string.checkout_shipping), style = MaterialTheme.typography.titleMedium)
                Field(state, CheckoutField.NAME, state.name, R.string.checkout_name, onIntent, KeyboardType.Text)
                Field(state, CheckoutField.ADDRESS, state.address, R.string.checkout_address, onIntent, KeyboardType.Text)
                Text(stringResource(R.string.checkout_payment), style = MaterialTheme.typography.titleMedium)
                CardPreview(state)
                Field(state, CheckoutField.CARD_NUMBER, state.cardNumber, R.string.checkout_card_number, onIntent, KeyboardType.Number, ltr = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        Field(state, CheckoutField.EXPIRY, state.expiry, R.string.checkout_expiry, onIntent, KeyboardType.Number, ltr = true)
                    }
                    Box(Modifier.weight(1f)) {
                        Field(state, CheckoutField.CVV, state.cvv, R.string.checkout_cvv, onIntent, KeyboardType.NumberPassword, ltr = true)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(pluralStringResource(R.plurals.checkout_total_items, state.itemCount, state.itemCount))
                    PriceText(state.total, style = MaterialTheme.typography.titleLarge)
                }
                if (state.hasError) {
                    Text(stringResource(UiR.string.error_generic), color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { onIntent(CheckoutIntent.Submit) },
                    enabled = !state.isProcessing && state.itemCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.checkout_pay)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.checkout_secure), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (state.isProcessing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.checkout_processing))
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(
    state: CheckoutState,
    field: CheckoutField,
    value: String,
    label: Int,
    onIntent: (CheckoutIntent) -> Unit,
    keyboard: KeyboardType,
    ltr: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onIntent(CheckoutIntent.FieldChanged(field, it)) },
        label = { Text(stringResource(label)) },
        isError = field in state.errors,
        supportingText = if (field in state.errors) ({ Text(stringResource(R.string.checkout_invalid)) }) else null,
        singleLine = true,
        enabled = !state.isProcessing,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = if (ltr) TextStyle(textDirection = TextDirection.Ltr) else TextStyle.Default,
        modifier = Modifier.fillMaxWidth(),
    )
}


@Composable
private fun SummaryRow(state: CheckoutState) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.checkout_summary), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.thumbnails.take(5).forEach { ProductImage(it, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))) }
            }
        }
    }
}

/** Live credit-card mock-up that fills in while the user types. */
@Composable
private fun CardPreview(state: CheckoutState) {
    val digits = CardValidation.digitsOnly(state.cardNumber)
    val brand = CardValidation.brandOf(digits)
    val shown = (digits + "•".repeat(16)).take(16).chunked(4).joinToString("  ")
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2B1B6B), Color(0xFF6C3CE9), Color(0xFFE040A0)))),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CreditCard, null, tint = Color.White)
                Text(
                    if (brand == CardBrand.OTHER) "" else brand.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                shown,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Ltr),
                fontWeight = FontWeight.Medium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    state.name.ifBlank { stringResource(R.string.checkout_name) }.uppercase(),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    state.expiry.ifBlank { stringResource(R.string.checkout_expiry) },
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr),
                )
            }
        }
    }
}
