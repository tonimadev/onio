package com.ipirangatech.onio.feature.settings.impl

import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import com.ipirangatech.onio.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeSettingsRepository()

    @Test fun startsFromTheStoredSettings() {
        val state = SettingsViewModel(repo).uiState.value
        assertFalse(state.isLoading)
        assertEquals(AppSettings.DEFAULT_CANCEL_MINUTES, state.settings.cancelAfterMinutes)
        assertNull(state.settings.currencyOverride)
    }

    @Test fun choosingADelayPersistsItAndTheStateFollows() {
        val vm = SettingsViewModel(repo)
        vm.onIntent(SettingsIntent.CancelMinutesSelected(15))
        assertEquals(15, repo.state.value.cancelAfterMinutes)
        assertEquals(15, vm.uiState.value.settings.cancelAfterMinutes)
    }

    @Test fun currencyCanBePickedAndResetToAutomatic() {
        val vm = SettingsViewModel(repo)
        vm.onIntent(SettingsIntent.CurrencySelected("BRL"))
        assertEquals("BRL", vm.uiState.value.settings.currencyOverride)
        vm.onIntent(SettingsIntent.CurrencySelected(null))
        assertNull(vm.uiState.value.settings.currencyOverride)
    }

    @Test fun languageAndBackAreEmittedAsEffects() {
        val vm = SettingsViewModel(repo)
        vm.onIntent(SettingsIntent.LanguageSelected("pt-BR"))
        assertEquals(SettingsEffect.ApplyLanguage("pt-BR"), vm.uiState.value.effect)
        vm.onIntent(SettingsIntent.ConsumeEffect)
        vm.onIntent(SettingsIntent.BackClicked)
        assertEquals(SettingsEffect.NavigateBack, vm.uiState.value.effect)
    }
}
