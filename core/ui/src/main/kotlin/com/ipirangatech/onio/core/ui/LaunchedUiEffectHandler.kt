package com.ipirangatech.onio.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/** Runs [onEffect] once per non-null [effect], then calls [onConsumed] so the state clears it. */
@Composable
fun <E : Any> LaunchedUiEffectHandler(
    effect: E?,
    onConsumed: () -> Unit,
    onEffect: suspend (E) -> Unit,
) {
    val consumed by rememberUpdatedState(onConsumed)
    val handler by rememberUpdatedState(onEffect)
    LaunchedEffect(effect) {
        if (effect != null) {
            handler(effect)
            consumed()
        }
    }
}
