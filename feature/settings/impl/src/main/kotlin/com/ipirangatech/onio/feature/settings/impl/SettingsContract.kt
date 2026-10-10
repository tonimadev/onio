package com.ipirangatech.onio.feature.settings.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.AppSettings

sealed interface SettingsEffect {
    data class ApplyLanguage(val tag: String) : SettingsEffect
    data object NavigateBack : SettingsEffect
}

sealed interface SettingsIntent {
    data class CancelMinutesSelected(val minutes: Int) : SettingsIntent
    /** Null = automatic (derived from language). */
    data class CurrencySelected(val code: String?) : SettingsIntent
    data class LanguageSelected(val tag: String) : SettingsIntent
    data object BackClicked : SettingsIntent
    data object ConsumeEffect : SettingsIntent
}

@Immutable
data class SettingsState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val effect: SettingsEffect? = null,
)
