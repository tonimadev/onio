package com.ipirangatech.onio.feature.onboarding.impl

import com.ipirangatech.onio.core.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private lateinit var settings: FakeSettingsRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        settings = FakeSettingsRepository()
        viewModel = OnboardingViewModel(settings)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun nextAdvancesAndSkipJumpsToTheNameForm() {
        viewModel.onIntent(OnboardingIntent.NextClicked)
        assertEquals(1, viewModel.uiState.value.page)
        viewModel.onIntent(OnboardingIntent.SkipClicked)
        assertTrue(viewModel.uiState.value.isLastPage)
        assertEquals(LAST_PAGE, viewModel.uiState.value.page)
        viewModel.onIntent(OnboardingIntent.NextClicked)
        assertEquals(LAST_PAGE, viewModel.uiState.value.page)
    }

    @Test fun submitNeedsANameAndStoresItTrimmed() {
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onIntent(OnboardingIntent.NameChanged("  Ana  "))
        assertTrue(viewModel.uiState.value.canSubmit)
        viewModel.onIntent(OnboardingIntent.SubmitClicked)
        assertEquals(OnboardingEffect.Finished, viewModel.uiState.value.effect)
        assertEquals("Ana", settings.state.value.userName)
        assertTrue(settings.state.value.onboardingCompleted)
        viewModel.onIntent(OnboardingIntent.ConsumeEffect)
        assertNull(viewModel.uiState.value.effect)
    }

    @Test fun regretTimeDefaultsToOneHourAndCanBeChosen() {
        assertEquals(60, viewModel.uiState.value.cancelAfterMinutes)
        viewModel.onIntent(OnboardingIntent.PreferNotToSayClicked)
        assertEquals(60, settings.state.value.cancelAfterMinutes)
    }

    @Test fun chosenRegretTimeIsStoredAsCancelDelay() {
        viewModel.onIntent(OnboardingIntent.CancelMinutesSelected(15))
        viewModel.onIntent(OnboardingIntent.NameChanged("Ana"))
        viewModel.onIntent(OnboardingIntent.SubmitClicked)
        assertEquals(15, settings.state.value.cancelAfterMinutes)
    }

    @Test fun preferNotToSayCompletesWithoutAName() {
        viewModel.onIntent(OnboardingIntent.PreferNotToSayClicked)
        assertEquals(OnboardingEffect.Finished, viewModel.uiState.value.effect)
        assertNull(settings.state.value.userName)
        assertTrue(settings.state.value.onboardingCompleted)
    }
}
