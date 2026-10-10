package com.ipirangatech.onio.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipirangatech.onio.core.ui.motion.AnimatedCountText
import com.ipirangatech.onio.core.ui.theme.OnioTheme
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random

/** A gold "Onio coin": shaded face, rim and an embossed ring. Drawn, so it is crisp at any size. */
@Composable
fun CoinIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val face = OnioTheme.colors.coinFace
    val rim = OnioTheme.colors.coinRim
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(rim, radius = r)
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), face, face), center = Offset(r * 0.7f, r * 0.6f), radius = r * 1.5f),
            radius = r * 0.88f,
        )
        drawCircle(rim.copy(alpha = 0.7f), radius = r * 0.62f, style = Stroke(width = r * 0.12f))
        // A small highlight that reads as a shine.
        drawCircle(Color.White.copy(alpha = 0.7f), radius = r * 0.09f, center = Offset(r * 0.62f, r * 0.55f))
    }
}

/** Coin + number that rolls when the balance changes. */
@Composable
fun CoinBalance(
    coins: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
    iconSize: Dp = 20.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CoinIcon(size = iconSize)
        AnimatedCountText(NumberFormat.getIntegerInstance().format(coins), style = style, color = color, fontWeight = FontWeight.Bold)
    }
}

private class Drop(val x: Float, val delay: Float, val speed: Float, val size: Float, val spin: Float)

/**
 * One-shot rain of spinning coins over [modifier]'s bounds; replays whenever [trigger] changes.
 * Spinning is faked by squeezing the coin horizontally with |cos|, which is cheap and reads well.
 */
@Composable
fun CoinRain(trigger: Any?, modifier: Modifier = Modifier, coins: Int = 26, durationMillis: Int = 2600) {
    val progress = remember { Animatable(1f) }
    val drops = remember(trigger) {
        List(coins) { Drop(Random.nextFloat(), Random.nextFloat() * 0.45f, 0.75f + Random.nextFloat() * 0.5f, 14f + Random.nextFloat() * 12f, 4f + Random.nextFloat() * 6f) }
    }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
    }
    val face = OnioTheme.colors.coinFace
    val rim = OnioTheme.colors.coinRim
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        drops.forEach { d ->
            val local = ((t - d.delay) / (1f - d.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -d.size + local * d.speed * (size.height + d.size * 2)
            val squeeze = abs(cos(local * d.spin * PI.toFloat())).coerceAtLeast(0.12f)
            val w = d.size * squeeze
            val alpha = (1f - ((local - 0.8f) / 0.2f).coerceIn(0f, 1f))
            val topLeft = Offset(d.x * size.width - w / 2, y)
            drawOval(rim.copy(alpha = alpha), topLeft, Size(w, d.size))
            drawOval(face.copy(alpha = alpha), topLeft + Offset(w * 0.12f, d.size * 0.1f), Size(w * 0.76f, d.size * 0.8f))
        }
    }
}
