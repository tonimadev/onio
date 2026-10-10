package com.ipirangatech.onio.feature.onboarding.impl

import androidx.compose.runtime.Immutable
import com.ipirangatech.onio.core.model.AppSettings

/** Pages: 0-2 intro slides, 3 regret-time question, 4 name form. */
const val REGRET_PAGE = 3
const val LAST_PAGE = 4

sealed interface OnboardingEffect {
    data object Finished : OnboardingEffect
}

sealed interface OnboardingIntent {
    data object NextClicked : OnboardingIntent
    data object SkipClicked : OnboardingIntent
    data class PageChanged(val page: Int) : OnboardingIntent
    data class NameChanged(val name: String) : OnboardingIntent
    data class CancelMinutesSelected(val minutes: Int) : OnboardingIntent
    data object SubmitClicked : OnboardingIntent
    data object PreferNotToSayClicked : OnboardingIntent
    data object ConsumeEffect : OnboardingIntent
}

@Immutable
data class OnboardingState(
    val page: Int = 0,
    val name: String = "",
    /** Answer to "how soon do you regret your purchases?"; one hour unless the user picks another. */
    val cancelAfterMinutes: Int = AppSettings.DEFAULT_CANCEL_MINUTES,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val effect: OnboardingEffect? = null,
) {
    val isLastPage: Boolean get() = page == LAST_PAGE
    val canSubmit: Boolean get() = name.isNotBlank() && !isSaving
}
