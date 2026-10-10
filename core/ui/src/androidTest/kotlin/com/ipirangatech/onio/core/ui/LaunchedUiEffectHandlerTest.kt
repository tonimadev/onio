package com.ipirangatech.onio.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LaunchedUiEffectHandlerTest {
    @get:Rule val compose = createComposeRule()

    private sealed interface Effect {
        data object ShowSnackbar : Effect
        data object Navigate : Effect
    }

    /** Mirrors a ViewModel: a single effect slot in the state, cleared by "ConsumeEffect". */
    private class Holder {
        var effect by mutableStateOf<Effect?>(null)
        val handled = mutableListOf<Effect>()
    }

    @Test fun anEffectEmittedWhileAnotherHandlerIsSuspendedIsNotLost() {
        val holder = Holder()
        compose.setContent {
            LaunchedUiEffectHandler(effect = holder.effect, onConsumed = { holder.effect = null }) { effect ->
                holder.handled += effect
                if (effect == Effect.ShowSnackbar) {
                    delay(300) // the snackbar is on screen...
                    holder.effect = Effect.Navigate // ...and the user taps "View cart"
                }
            }
        }
        compose.runOnIdle { holder.effect = Effect.ShowSnackbar }
        compose.waitUntil(5_000) { holder.handled.size == 2 }
        assertEquals(listOf(Effect.ShowSnackbar, Effect.Navigate), holder.handled)
        compose.runOnIdle { assertNull(holder.effect) }
    }

    @Test fun eachEffectIsHandledExactlyOnce() {
        val holder = Holder()
        compose.setContent {
            LaunchedUiEffectHandler(effect = holder.effect, onConsumed = { holder.effect = null }) { holder.handled += it }
        }
        compose.runOnIdle { holder.effect = Effect.Navigate }
        compose.waitForIdle()
        compose.runOnIdle { holder.effect = Effect.Navigate }
        compose.waitForIdle()
        assertEquals(listOf(Effect.Navigate, Effect.Navigate), holder.handled)
    }
}
