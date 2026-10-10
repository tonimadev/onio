package com.ipirangatech.onio.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

/**
 * Handles a one-shot UI effect from the state.
 *
 * The effect is consumed *first* and handled in a separate coroutine. Doing it the other way round loses effects:
 * a handler that suspends (a snackbar waiting for its action) and then emits a new effect would have that new effect
 * wiped by the trailing `onConsumed()` before it could be handled. Handling outside the `LaunchedEffect` also means
 * that clearing the effect does not cancel a handler that is still suspended.
 */
@Composable
fun <E : Any> LaunchedUiEffectHandler(
    effect: E?,
    onConsumed: () -> Unit,
    onEffect: suspend (E) -> Unit,
) {
    val consumed by rememberUpdatedState(onConsumed)
    val handler by rememberUpdatedState(onEffect)
    val scope = rememberCoroutineScope()
    LaunchedEffect(effect) {
        if (effect != null) {
            consumed()
            scope.launch { handler(effect) }
        }
    }
}
