package com.tasnimulhasan.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tasnimulhasan.entity.enums.VisualizerStyle
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders [bars] (0f..1f normalized amplitude - see VisualizerController) in the given [style],
 * with the same look as the previews in Settings: rounded bars that brighten with level, a
 * flowing two-layer waveform, and a ring of spokes.
 *
 * With [ring] = true every style is drawn AROUND a round cover (the caller leaves the middle
 * free for the art): BARS as thick rounded blocks, WAVEFORM as a wobbling closed wave, and
 * CIRCULAR as fine spokes. Without it BARS and WAVEFORM are drawn as a strip. [style] == OFF
 * renders nothing.
 *
 * The incoming data arrives at ~20fps; it is eased every frame (fast rise, slower fall) so the
 * motion looks fluid instead of stepping.
 */
@Composable
fun AudioVisualizer(
    bars: FloatArray,
    style: VisualizerStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    ring: Boolean = false,
) {
    if (style == VisualizerStyle.OFF) return
    val smooth = rememberSmoothedBars(bars)

    Canvas(modifier = modifier) {
        // Reading the tick here makes only this draw pass re-run each frame.
        val now = smooth.tick.longValue
        val v = smooth.values
        if (v.isEmpty()) return@Canvas
        val phase = (now / 1_000_000L).toFloat() / 2400f
        when {
            style == VisualizerStyle.CIRCULAR -> drawRingSpokes(v, color)
            ring && style == VisualizerStyle.BARS -> drawRingBlocks(v, color)
            ring && style == VisualizerStyle.WAVEFORM -> drawRingWave(v, color, secondaryColor, phase)
            style == VisualizerStyle.BARS -> drawStripBars(v, color)
            style == VisualizerStyle.WAVEFORM -> drawStripWave(v, color, secondaryColor, phase)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Smoothing
// ─────────────────────────────────────────────────────────────────────────────────────────

private class SmoothedBars {
    var values = FloatArray(0)
    val tick = mutableLongStateOf(0L)
}

@Composable
private fun rememberSmoothedBars(bars: FloatArray): SmoothedBars {
    val holder = remember { SmoothedBars() }
    val latest = rememberUpdatedState(bars)
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val target = latest.value
                if (holder.values.size != target.size) holder.values = FloatArray(target.size)
                val cur = holder.values
                for (i in target.indices) {
                    val t = target[i].coerceIn(0f, 1f)
                    val k = if (t > cur[i]) 0.55f else 0.16f
                    cur[i] += (t - cur[i]) * k
                }
                holder.tick.longValue = now
            }
        }
    }
    return holder
}

/** Value at fractional position [x] (0..1) across the array, linearly interpolated. */
private fun sample(v: FloatArray, x: Float): Float {
    if (v.size == 1) return v[0]
    val p = x.coerceIn(0f, 1f) * (v.size - 1)
    val i = p.toInt().coerceAtMost(v.size - 2)
    val f = p - i
    return v[i] * (1f - f) + v[i + 1] * f
}

/** Mirrored around the circle: low frequencies at the top on both sides, like a classic ring equaliser. */
private fun sampleMirrored(v: FloatArray, turn: Float): Float {
    val t = turn - kotlin.math.floor(turn)
    val m = if (t < 0.5f) t * 2f else (1f - t) * 2f
    return sample(v, m)
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Strip (BARS, WAVEFORM)
// ─────────────────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawStripBars(v: FloatArray, color: Color) {
    val n = v.size
    val slot = size.width / n
    for (i in 0 until n) {
        val level = 0.12f + 0.88f * v[i]
        val h = size.height * level
        drawRoundRect(
            color = color.copy(alpha = 0.55f + 0.45f * level),
            topLeft = Offset(i * slot + slot * 0.2f, size.height - h),
            size = Size(slot * 0.6f, h),
            cornerRadius = CornerRadius(slot * 0.3f),
        )
    }
}

private fun DrawScope.drawStripWave(v: FloatArray, color: Color, second: Color, phase: Float) {
    fun wave(ph: Float, ampScale: Float): Path {
        val path = Path()
        val steps = 96
        for (i in 0..steps) {
            val u = i / steps.toFloat()
            val x = size.width * u
            val envelope = sin(PI.toFloat() * u)
            val amp = (0.16f + 0.84f * sample(v, u)) * ampScale
            val y = size.height / 2f + sin(2f * PI.toFloat() * (u * 2.4f + ph)) * amp * size.height * envelope
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }
    val w = 3.dp.toPx()
    drawPath(wave(-phase * 0.6f, 0.34f), second.copy(alpha = 0.55f), style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(wave(phase, 0.46f), color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Ring (around round cover art). The canvas is square; the art takes the middle ~78%.
// ─────────────────────────────────────────────────────────────────────────────────────────

private fun DrawScope.ringGeometry(): Triple<Offset, Float, Float> {
    val half = min(size.width, size.height) / 2f
    val inner = half * 0.83f
    val maxLen = half - inner - 2.dp.toPx()
    return Triple(Offset(size.width / 2f, size.height / 2f), inner, maxLen)
}

private fun DrawScope.drawRingSpokes(v: FloatArray, color: Color) {
    val (c, inner, maxLen) = ringGeometry()
    val n = 72
    val stroke = 3.dp.toPx()
    for (i in 0 until n) {
        val turn = i / n.toFloat()
        val a = (2f * PI.toFloat() * turn) - PI.toFloat() / 2f
        val level = sampleMirrored(v, turn)
        val len = maxLen * (0.14f + 0.86f * level)
        drawLine(
            color = color.copy(alpha = 0.5f + 0.5f * level),
            start = Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
            end = Offset(c.x + cos(a) * (inner + len), c.y + sin(a) * (inner + len)),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawRingBlocks(v: FloatArray, color: Color) {
    val (c, inner, maxLen) = ringGeometry()
    val n = 36
    val stroke = 6.dp.toPx()
    for (i in 0 until n) {
        val turn = i / n.toFloat()
        val a = (2f * PI.toFloat() * turn) - PI.toFloat() / 2f
        val level = sampleMirrored(v, turn)
        val len = maxLen * (0.18f + 0.82f * level)
        drawLine(
            color = color.copy(alpha = 0.55f + 0.45f * level),
            start = Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
            end = Offset(c.x + cos(a) * (inner + len), c.y + sin(a) * (inner + len)),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawRingWave(v: FloatArray, color: Color, second: Color, phase: Float) {
    val (c, inner, maxLen) = ringGeometry()
    fun loop(ph: Float, ampScale: Float, wobbles: Float): Path {
        val path = Path()
        val steps = 160
        for (i in 0..steps) {
            val turn = i / steps.toFloat()
            val a = (2f * PI.toFloat() * turn) - PI.toFloat() / 2f
            val level = 0.18f + 0.82f * sampleMirrored(v, turn)
            val wob = 0.5f + 0.5f * sin(2f * PI.toFloat() * (turn * wobbles + ph))
            val r = inner + maxLen * 0.5f + maxLen * 0.5f * ampScale * level * (wob * 2f - 1f)
            val x = c.x + cos(a) * r
            val y = c.y + sin(a) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }
    val w = 3.dp.toPx()
    drawPath(loop(-phase * 0.6f, 0.9f, 7f), second.copy(alpha = 0.55f), style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(loop(phase, 1.0f, 5f), color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
