package com.ipirangatech.onio.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.component.CustomDelayField
import com.ipirangatech.onio.core.ui.R as UiR

private class Slide(val icon: ImageVector, val title: Int, val body: Int, val colors: List<Color>)

private val Slides = listOf(
    Slide(Icons.Filled.Storefront, R.string.onb_p1_title, R.string.onb_p1_body, listOf(Color(0xFF8A5CFF), Color(0xFF4A20C4))),
    Slide(Icons.Filled.EventBusy, R.string.onb_p2_title, R.string.onb_p2_body, listOf(Color(0xFFFF9A3D), Color(0xFFE5532D))),
    Slide(Icons.Filled.Savings, R.string.onb_p3_title, R.string.onb_p3_body, listOf(Color(0xFF2BC48A), Color(0xFF0E8F6A))),
)

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(OnboardingIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            OnboardingEffect.Finished -> onFinished()
        }
    }
    OnboardingScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun OnboardingScreen(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { LAST_PAGE + 1 }
    // State is the source of truth: buttons move the pager, swipes report back.
    LaunchedEffect(state.page) {
        if (pager.currentPage != state.page) pager.animateScrollToPage(state.page)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { onIntent(OnboardingIntent.PageChanged(it)) }
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!state.isLastPage) {
                    TextButton(onClick = { onIntent(OnboardingIntent.SkipClicked) }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.onb_skip))
                    }
                }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                when {
                    page < Slides.size -> SlidePage(Slides[page])
                    page == REGRET_PAGE -> RegretPage(state, onIntent)
                    else -> NamePage(state, onIntent)
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    repeat(LAST_PAGE + 1) { index ->
                        val selected = state.page == index
                        Box(
                            Modifier.padding(3.dp).size(if (selected) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                if (state.isLastPage) {
                    Button(onClick = { onIntent(OnboardingIntent.SubmitClicked) }, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.onb_start))
                    }
                    TextButton(onClick = { onIntent(OnboardingIntent.PreferNotToSayClicked) }, enabled = !state.isSaving) {
                        Text(stringResource(R.string.onb_prefer_not))
                    }
                } else {
                    Button(onClick = { onIntent(OnboardingIntent.NextClicked) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.onb_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun Illustration(icon: ImageVector, colors: List<Color>) {
    Box(
        Modifier.size(180.dp).clip(CircleShape).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(88.dp), tint = Color.White) }
}

@Composable
private fun SlidePage(slide: Slide) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Illustration(slide.icon, slide.colors)
        Text(stringResource(slide.title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(slide.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NamePage(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Illustration(Icons.Filled.Person, listOf(Color(0xFFE040A0), Color(0xFF6C3CE9)))
        Text(stringResource(R.string.onb_p4_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        OutlinedTextField(
            value = state.name,
            onValueChange = { onIntent(OnboardingIntent.NameChanged(it)) },
            label = { Text(stringResource(R.string.onb_name_label)) },
            supportingText = {
                Text(if (state.hasError) stringResource(UiR.string.error_generic) else stringResource(R.string.onb_p4_body))
            },
            isError = state.hasError,
            singleLine = true,
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onIntent(OnboardingIntent.SubmitClicked) }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegretPage(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Illustration(Icons.Filled.Schedule, listOf(Color(0xFFFFB400), Color(0xFFFF7A1A)))
        Text(stringResource(R.string.onb_regret_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppSettings.CANCEL_DELAY_OPTIONS_MINUTES.forEach { minutes ->
                FilterChip(
                    selected = state.cancelAfterMinutes == minutes,
                    onClick = { onIntent(OnboardingIntent.CancelMinutesSelected(minutes)) },
                    label = {
                        Text(
                            if (minutes >= 60) pluralStringResource(R.plurals.onb_hours, minutes / 60, minutes / 60)
                            else pluralStringResource(R.plurals.onb_minutes, minutes, minutes),
                        )
                    },
                )
            }
        }
        CustomDelayField(
            minutes = state.cancelAfterMinutes,
            onMinutesChange = { onIntent(OnboardingIntent.CancelMinutesSelected(it)) },
        )
        Text(stringResource(R.string.onb_regret_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
