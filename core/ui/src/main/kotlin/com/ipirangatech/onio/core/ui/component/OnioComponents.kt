package com.ipirangatech.onio.core.ui.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.ipirangatech.onio.core.ui.motion.AnimatedCountText
import com.ipirangatech.onio.core.ui.motion.pressScale
import com.ipirangatech.onio.core.ui.theme.Dimens
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import java.text.NumberFormat
import java.util.Locale

private val ButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

/** Solid brand button with small corners (the Material `Button` is a fixed pill). */
@Composable
fun OnioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        interactionSource = source,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = ButtonPadding,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) { content() }
    }
}

/** Secondary action: brand outline on a pale brand tint ("Add to cart" next to a solid "Buy now"). */
@Composable
fun OnioOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val enabledColor = MaterialTheme.colorScheme.primary
    val source = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        interactionSource = source,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (enabled) enabledColor else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = OnioTheme.colors.priceText,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = ButtonPadding,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) { content() }
    }
}

/** Filter/sort chip: white with a hairline border; selected = brand border, text and tint. */
@Composable
fun OnioChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        leadingIcon = leadingIcon,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = OnioTheme.colors.priceText,
            selectedLeadingIconColor = OnioTheme.colors.priceText,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = OnioTheme.colors.border,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp,
        ),
    )
}

/** Flat white top bar with a hairline divider. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnioTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        HorizontalDivider(color = OnioTheme.colors.border, thickness = Dimens.Hairline)
    }
}

/** White block with a hairline border and almost square corners; sections are separated by page background. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraSmall
    val border = BorderStroke(Dimens.Hairline, OnioTheme.colors.border)
    if (onClick == null) {
        Surface(modifier, shape = shape, color = MaterialTheme.colorScheme.surface, border = border) { Column(content = content) }
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = MaterialTheme.colorScheme.surface, border = border) {
            Column(content = content)
        }
    }
}

/** `HH : MM : SS` on dark boxes, the signature element of flash-sale sections. */
@Composable
fun FlashCountdown(millis: Long, modifier: Modifier = Modifier, locale: Locale = Locale.getDefault()) {
    val total = (millis / 1000).coerceAtLeast(0)
    val two = NumberFormat.getIntegerInstance(locale).also {
        it.isGroupingUsed = false
        it.minimumIntegerDigits = 2
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(total / 3600, (total % 3600) / 60, total % 60).forEachIndexed { index, value ->
            if (index > 0) Text(":", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Box(
                Modifier.background(OnioTheme.colors.countdownBox, MaterialTheme.shapes.extraSmall).padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedCountText(
                    two.format(value),
                    color = OnioTheme.colors.onCountdownBox,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Makes status-bar icons light (for a dark/brand header) while this composable is in composition. */
@Composable
fun StatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, light) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = !light
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
}

/** Small square tile with a tinted background, used for category icons. */
@Composable
fun IconTile(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) { content() }
}
