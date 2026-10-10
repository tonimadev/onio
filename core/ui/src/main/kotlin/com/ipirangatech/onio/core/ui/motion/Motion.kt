package com.ipirangatech.onio.core.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipirangatech.onio.core.model.Money
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shared motion vocabulary. Every animation here is driven by Compose's animation clock, which follows the
 * system "remove animations" setting, so users who disable animations get instant transitions.
 */
object OnioMotion {
    const val Short = 150
    const val Medium = 300
    const val Long = 500
    val Easing = FastOutSlowInEasing

    /** Bouncy but controlled: used for taps, pops and entrances. */
    fun <T> bouncy() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
}

/** Shrinks slightly while pressed, springing back on release. Pass the same source given to the clickable. */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, OnioMotion.bouncy(), label = "pressScale")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Fades and slides an item up when it first appears; items further down start slightly later. */
@Composable
fun Modifier.staggeredEntrance(index: Int, maxStaggered: Int = 8): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(index, maxStaggered) * 45L)
        progress.animateTo(1f, tween(OnioMotion.Long, easing = OnioMotion.Easing))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 28.dp.toPx()
    }
}

/** Quick "pop" (grow then settle) whenever [key] changes, e.g. a cart badge when the count changes. */
@Composable
fun Modifier.popOn(key: Any?, peak: Float = 1.35f): Modifier {
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(key) {
        if (first) {
            first = false
        } else {
            scale.snapTo(peak)
            scale.animateTo(1f, OnioMotion.bouncy())
        }
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** Text whose value rolls vertically when it changes (quantities, countdown digits). */
@Composable
fun AnimatedCountText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            (slideInVertically(tween(OnioMotion.Medium)) { it } + fadeIn(tween(OnioMotion.Medium))) togetherWith
                (slideOutVertically(tween(OnioMotion.Medium)) { -it } + fadeOut(tween(OnioMotion.Short)))
        },
        label = "countText",
    ) { value -> Text(value, style = style, color = color, fontWeight = fontWeight) }
}

/** Animates [target] from its previous value, so totals "count" instead of jumping. */
@Composable
fun animateMoneyAsState(target: Money): Money {
    val animated by animateFloatAsState(
        targetValue = target.minorUnits.toFloat(),
        animationSpec = tween(700, easing = OnioMotion.Easing),
        label = "money",
    )
    return Money(animated.toLong(), target.currencyCode)
}

fun twoDigits(value: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).also {
        it.isGroupingUsed = false
        it.minimumIntegerDigits = 2
    }.format(value)

/** A checkmark that draws itself; used to confirm a payment. */
@Composable
fun AnimatedCheckmark(color: Color, modifier: Modifier = Modifier, strokeWidth: Float = 10f, durationMillis: Int = 550) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis, easing = OnioMotion.Easing)) }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.54f)
            lineTo(w * 0.43f, h * 0.74f)
            lineTo(w * 0.80f, h * 0.30f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress.value, partial, true)
        drawPath(partial, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private class Piece(val fromLeft: Boolean, val angle: Float, val speed: Float, val spin: Float, val w: Float, val h: Float)

/**
 * Party-popper confetti: two "cannons" in the bottom corners shoot paper pieces up and inwards; gravity pulls them
 * back down while they spin and fade. Drawn over the content, replays whenever [trigger] changes.
 */
@Composable
fun Modifier.confetti(trigger: Any?, colors: List<Color>, particles: Int = 36, durationMillis: Int = 2800): Modifier {
    val progress = remember { Animatable(0f) }
    val pieces = remember(trigger) {
        List(particles) { i ->
            val left = i % 2 == 0
            // Angles are measured from the horizontal; the left cannon shoots up-right, the right one up-left.
            val up = Math.toRadians((52 + Random.nextFloat() * 36).toDouble()).toFloat()
            Piece(left, up, 0.95f + Random.nextFloat() * 0.85f, (Random.nextFloat() - 0.5f) * 5f, 5f + Random.nextFloat() * 5f, 8f + Random.nextFloat() * 7f)
        }
    }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
    }
    val seconds = durationMillis / 1000f
    return drawWithContent {
        drawContent()
        val t = progress.value
        if (t <= 0f || t >= 1f) return@drawWithContent
        val time = t * seconds
        val gravity = size.height * 1.5f
        pieces.forEachIndexed { index, piece ->
            val dir = if (piece.fromLeft) 1f else -1f
            val vx = cos(piece.angle) * piece.speed * size.height * 1.1f * dir
            val vy = -sin(piece.angle) * piece.speed * size.height * 1.5f
            val x = (if (piece.fromLeft) 0f else size.width) + vx * time * 0.55f
            val y = size.height * 0.96f + vy * time + 0.5f * gravity * time * time
            val alpha = 1f - ((t - 0.7f) / 0.3f).coerceIn(0f, 1f)
            val w = piece.w.dp.toPx()
            val h = piece.h.dp.toPx()
            withTransform({
                translate(x, y)
                rotate(piece.spin * t * 360f, Offset.Zero)
            }) {
                drawRect(colors[index % colors.size].copy(alpha = alpha), topLeft = Offset(-w / 2, -h / 2), size = Size(w, h))
            }
        }
    }
}

/** Two-digit time group (HH, MM or SS) whose digits roll when they change. */
@Composable
fun AnimatedDigits(value: Long, modifier: Modifier = Modifier, style: TextStyle, color: Color, locale: Locale = Locale.getDefault()) {
    Row(modifier) {
        AnimatedCountText(twoDigits(value, locale), style = style, color = color, fontWeight = FontWeight.Bold)
    }
}
