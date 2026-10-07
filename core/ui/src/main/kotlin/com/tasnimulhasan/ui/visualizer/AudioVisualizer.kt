package com.tasnimulhasan.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tasnimulhasan.entity.enums.VisualizerStyle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders [bars] (0f..1f normalized amplitude - see VisualizerController for how they're
 * derived) in the given [style]. [style] == OFF renders nothing, so callers don't need to
 * gate on visibility themselves.
 *
 * Deliberately draws straight from the latest [bars] snapshot rather than adding a separate
 * per-bar smoothing/animation layer on top: VisualizerController already paces capture at
 * roughly 20fps, which reads as smooth motion on its own, and a correct cross-fade between
 * arrays of amplitude values (handling size/identity changes safely every frame) is exactly
 * the kind of thing that's easy to get subtly wrong without being able to see it run. This
 * can be revisited once it's been seen working on a device.
 */
@Composable
fun AudioVisualizer(
    bars: FloatArray,
    style: VisualizerStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (style == VisualizerStyle.OFF) return

    when (style) {
        VisualizerStyle.BARS -> BarsVisualizer(bars, color, modifier)
        VisualizerStyle.WAVEFORM -> WaveformVisualizer(bars, color, modifier)
        VisualizerStyle.CIRCULAR -> CircularVisualizer(bars, color, modifier)
        VisualizerStyle.OFF -> Unit
    }
}

@Composable
private fun BarsVisualizer(values: FloatArray, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val barWidth = size.width / (values.size * 1.5f)
        val gap = barWidth * 0.5f
        values.forEachIndexed { i, amplitude ->
            val barHeight = (amplitude * size.height).coerceAtLeast(barWidth * 0.5f)
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}

@Composable
private fun WaveformVisualizer(values: FloatArray, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val midY = size.height / 2f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, amplitude ->
            val x = i * stepX
            // Alternate up/down so a flat (silent) waveform still reads as a line, not a
            // single dot, and loud passages fill more of the available height.
            val sign = if (i % 2 == 0) 1f else -1f
            val y = midY - sign * amplitude * midY
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CircularVisualizer(values: FloatArray, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val radius = min(size.width, size.height) / 2.4f
        val center = Offset(size.width / 2f, size.height / 2f)
        val angleStep = 360f / values.size
        values.forEachIndexed { i, amplitude ->
            val angleRad = Math.toRadians((i * angleStep).toDouble())
            val lineLength = radius * 0.4f * amplitude.coerceAtLeast(0.08f)
            val startX = center.x + radius * cos(angleRad).toFloat()
            val startY = center.y + radius * sin(angleRad).toFloat()
            val endX = center.x + (radius + lineLength) * cos(angleRad).toFloat()
            val endY = center.y + (radius + lineLength) * sin(angleRad).toFloat()
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}
