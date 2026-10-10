package com.ipirangatech.onio.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipirangatech.onio.core.model.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()
    val effect = uiState.map { it.effect }.distinctUntilChanged()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.NextClicked -> _uiState.update { it.copy(page = (it.page + 1).coerceAtMost(LAST_PAGE)) }
            OnboardingIntent.SkipClicked -> _uiState.update { it.copy(page = LAST_PAGE) }
            is OnboardingIntent.PageChanged -> _uiState.update { it.copy(page = intent.page.coerceIn(0, LAST_PAGE)) }
            is OnboardingIntent.CancelMinutesSelected -> _uiState.update { it.copy(cancelAfterMinutes = intent.minutes) }
            is OnboardingIntent.NameChanged -> _uiState.update { it.copy(name = intent.name.take(MAX_NAME), hasError = false) }
            OnboardingIntent.SubmitClicked -> finish(_uiState.value.name.takeIf { it.isNotBlank() })
            OnboardingIntent.PreferNotToSayClicked -> finish(null)
            OnboardingIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun finish(name: String?) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            runCatching { settings.completeOnboarding(name, _uiState.value.cancelAfterMinutes) }
                .onSuccess { _uiState.update { it.copy(isSaving = false, effect = OnboardingEffect.Finished) } }
                .onFailure { _uiState.update { it.copy(isSaving = false, hasError = true) } }
        }
    }

    private companion object {
        const val MAX_NAME = 40
    }
}
