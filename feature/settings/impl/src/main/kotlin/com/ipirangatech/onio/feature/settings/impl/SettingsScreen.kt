package com.ipirangatech.onio.feature.settings.impl

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.model.ExchangeRates
import com.ipirangatech.onio.core.ui.LaunchedUiEffectHandler
import com.ipirangatech.onio.core.ui.component.CustomDelayField
import com.ipirangatech.onio.core.ui.R as UiR

/** Native names, so a user can always find their language. */
private val Languages = listOf(
    "en" to "English", "zh" to "中文", "hi" to "हिन्दी", "es" to "Español", "fr" to "Français",
    "ar" to "العربية", "bn" to "বাংলা", "pt-BR" to "Português", "ru" to "Русский", "ur" to "اردو",
)

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedUiEffectHandler(
        effect = state.effect,
        onConsumed = { viewModel.onIntent(SettingsIntent.ConsumeEffect) },
    ) { effect ->
        when (effect) {
            is SettingsEffect.ApplyLanguage ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(effect.tag))
            SettingsEffect.NavigateBack -> onBack()
        }
    }
    SettingsScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(state: SettingsState, onIntent: (SettingsIntent) -> Unit, modifier: Modifier = Modifier) {
    var currentLanguage by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { null })
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SettingsIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_cancel_after), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_cancel_after_hint), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSettings.CANCEL_DELAY_OPTIONS_MINUTES.forEach { minutes ->
                    FilterChip(
                        selected = state.settings.cancelAfterMinutes == minutes,
                        onClick = { onIntent(SettingsIntent.CancelMinutesSelected(minutes)) },
                        label = {
                            Text(
                                if (minutes >= 60) pluralStringResource(R.plurals.settings_hours, minutes / 60, minutes / 60)
                                else pluralStringResource(R.plurals.settings_minutes, minutes, minutes),
                            )
                        },
                    )
                }
            }
            CustomDelayField(
                minutes = state.settings.cancelAfterMinutes,
                onMinutesChange = { onIntent(SettingsIntent.CancelMinutesSelected(it)) },
            )
            HorizontalDivider()
            Text(stringResource(R.string.settings_currency), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.settings.currencyOverride == null,
                    onClick = { onIntent(SettingsIntent.CurrencySelected(null)) },
                    label = { Text(stringResource(R.string.settings_automatic)) },
                )
                ExchangeRates.supportedCurrencies.forEach { code ->
                    FilterChip(
                        selected = state.settings.currencyOverride == code,
                        onClick = { onIntent(SettingsIntent.CurrencySelected(code)) },
                        label = { Text(code) },
                    )
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Languages.forEach { (tag, name) ->
                    FilterChip(
                        selected = currentLanguage?.startsWith(tag.substringBefore('-')) == true,
                        onClick = {
                            currentLanguage = tag
                            onIntent(SettingsIntent.LanguageSelected(tag))
                        },
                        label = { Text(name) },
                    )
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_about_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
