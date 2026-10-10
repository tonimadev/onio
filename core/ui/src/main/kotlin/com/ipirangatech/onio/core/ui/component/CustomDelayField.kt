package com.ipirangatech.onio.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ipirangatech.onio.core.model.AppSettings
import com.ipirangatech.onio.core.ui.R

private enum class DelayUnit(val factor: Int, val label: Int) {
    MINUTES(1, R.string.delay_unit_minutes),
    HOURS(60, R.string.delay_unit_hours),
    DAYS(24 * 60, R.string.delay_unit_days),
}

private fun parse(text: String, unit: DelayUnit): Long? = text.toLongOrNull()?.times(unit.factor)

private fun unitFor(minutes: Int): DelayUnit = when {
    minutes in AppSettings.CANCEL_DELAY_OPTIONS_MINUTES -> DelayUnit.MINUTES
    minutes % DelayUnit.DAYS.factor == 0 -> DelayUnit.DAYS
    minutes % DelayUnit.HOURS.factor == 0 -> DelayUnit.HOURS
    else -> DelayUnit.MINUTES
}

/** Empty for preset delays (their chip is selected instead); the number in [unit] otherwise. */
private fun textFor(minutes: Int, unit: DelayUnit): String =
    if (minutes in AppSettings.CANCEL_DELAY_OPTIONS_MINUTES) "" else (minutes / unit.factor).toString()

private fun isValid(minutes: Long?): Boolean = minutes != null && minutes in 1..AppSettings.MAX_CANCEL_MINUTES

/**
 * Free-form cancel delay: a number plus a unit. Valid values are reported as minutes; the field
 * clears itself when the delay is changed from outside (e.g. a preset chip), so only one wins.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomDelayField(minutes: Int, onMinutesChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var unit by rememberSaveable { mutableStateOf(unitFor(minutes)) }
    var text by rememberSaveable { mutableStateOf(textFor(minutes, unit)) }
    // Values we reported ourselves; they come back through the settings flow and must not rewrite the field.
    val sent = remember { mutableSetOf<Int>() }

    // The delay can change from outside (settings loading after first composition, a preset chip):
    // show custom values in the field and clear it for presets.
    LaunchedEffect(minutes) {
        if (minutes !in sent && parse(text, unit) != minutes.toLong()) {
            unit = unitFor(minutes)
            text = textFor(minutes, unit)
            sent.clear()
        }
    }

    fun report(newText: String, newUnit: DelayUnit) {
        val value = parse(newText, newUnit)
        if (isValid(value)) {
            sent += value!!.toInt()
            onMinutesChange(value.toInt())
        }
    }

    val invalid = text.isNotEmpty() && !isValid(parse(text, unit))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input.filter(Char::isDigit).take(5)
                report(text, unit)
            },
            label = { Text(stringResource(R.string.delay_custom_label)) },
            supportingText = { Text(stringResource(R.string.delay_custom_hint)) },
            isError = invalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DelayUnit.entries.forEach { option ->
                FilterChip(
                    selected = unit == option,
                    onClick = {
                        unit = option
                        report(text, option)
                    },
                    label = { Text(stringResource(option.label)) },
                )
            }
        }
    }
}
