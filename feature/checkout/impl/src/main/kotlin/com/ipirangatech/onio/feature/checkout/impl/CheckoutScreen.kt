package com.ipirangatech.onio.feature.checkout.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import com.ipirangatech.onio.core.ui.component.OnioButton
import com.ipirangatech.onio.core.ui.component.OnioTopBar
import com.ipirangatech.onio.core.ui.component.SectionCard
import com.ipirangatech.onio.core.ui.motion.animateMoneyAsState
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.input.VisualTransformation
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

@Composable
internal fun CheckoutScreen(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit, modifier: Modifier = Modifier) {
    var cvvFocused by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OnioTopBar(
                title = { Text(stringResource(R.string.checkout_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CheckoutIntent.BackClicked) }, enabled = !state.isProcessing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(start = Dimens.Space12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            pluralStringResource(R.plurals.checkout_total_items, state.itemCount, state.itemCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PriceText(animateMoneyAsState(state.total), style = MaterialTheme.typography.titleLarge)
                    }
                    OnioButton(
                        onClick = { onIntent(CheckoutIntent.Submit) },
                        enabled = !state.isProcessing && state.itemCount > 0,
                        modifier = Modifier.padding(Dimens.Space8).height(48.dp),
                    ) { Text(stringResource(R.string.checkout_pay), maxLines = 1) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(Dimens.Space8),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            ) {
                SummaryRow(state)
                SectionCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                        Text(stringResource(R.string.checkout_shipping), style = MaterialTheme.typography.titleMedium)
                        Field(state, CheckoutField.NAME, state.name, R.string.checkout_name, onIntent, KeyboardType.Text)
                        Field(state, CheckoutField.ADDRESS, state.address, R.string.checkout_address, onIntent, KeyboardType.Text)
                    }
                }
                SectionCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                        Text(stringResource(R.string.checkout_payment), style = MaterialTheme.typography.titleMedium)
                        CardPreview(state, showBack = cvvFocused)
                        Field(state, CheckoutField.CARD_NUMBER, state.cardNumber, R.string.checkout_card_number, onIntent, KeyboardType.Number, ltr = true, transformation = CardNumberTransformation)
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                            Box(Modifier.weight(1f)) {
                                Field(state, CheckoutField.EXPIRY, state.expiry, R.string.checkout_expiry, onIntent, KeyboardType.Number, ltr = true, transformation = ExpiryTransformation)
                            }
                            Box(Modifier.weight(1f)) {
                                Field(state, CheckoutField.CVV, state.cvv, R.string.checkout_cvv, onIntent, KeyboardType.NumberPassword, ltr = true, onFocusChanged = { cvvFocused = it })
                            }
                        }
                        if (state.hasError) {
                            Text(stringResource(UiR.string.error_generic), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = OnioTheme.colors.freeShipping)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.checkout_secure), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            AnimatedVisibility(state.isProcessing, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.checkout_processing), style = MaterialTheme.typography.titleMedium)
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
    transformation: VisualTransformation = VisualTransformation.None,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onIntent(CheckoutIntent.FieldChanged(field, it)) },
        label = { Text(stringResource(label)) },
        isError = field in state.errors,
        supportingText = if (field in state.errors) ({ Text(stringResource(R.string.checkout_invalid)) }) else null,
        singleLine = true,
        visualTransformation = transformation,
        enabled = !state.isProcessing,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = if (ltr) TextStyle(textDirection = TextDirection.Ltr) else TextStyle.Default,
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
    )
}

@Composable
private fun SummaryRow(state: CheckoutState) {
    SectionCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.Space12), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
            Text(stringResource(R.string.checkout_summary), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                state.thumbnails.take(5).forEach {
                    ProductImage(it, Modifier.size(52.dp).clip(MaterialTheme.shapes.extraSmall).border(Dimens.Hairline, OnioTheme.colors.border, MaterialTheme.shapes.extraSmall))
                }
            }
        }
    }
}

/** Live credit-card mock-up; flips to its back while the CVV field is focused. */
@Composable
private fun CardPreview(state: CheckoutState, showBack: Boolean) {
    val rotation by animateFloatAsState(if (showBack) 180f else 0f, tween(450), label = "cardFlip")
    val digits = CardValidation.digitsOnly(state.cardNumber)
    val brand = CardValidation.brandOf(digits)
    val shown = (digits + "•".repeat(16)).take(16).chunked(4).joinToString("  ")
    val expiry = if (state.expiry.length > 2) state.expiry.take(2) + "/" + state.expiry.drop(2) else state.expiry
    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(OnioTheme.colors.cardStart, OnioTheme.colors.cardEnd))),
    ) {
        if (rotation <= 90f) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
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
                        expiry.ifBlank { stringResource(R.string.checkout_expiry) },
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr),
                    )
                }
            }
        } else {
            // Back face: counter-rotated so the text is not mirrored.
            Column(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.padding(top = 20.dp).fillMaxWidth().height(36.dp).background(Color.Black))
                Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(32.dp).background(Color.White.copy(alpha = 0.9f)))
                    Text(
                        "•".repeat(state.cvv.length.coerceAtLeast(3)),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Ltr),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
