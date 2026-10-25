package com.tasnimulhasan.radio.ui

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasnimulhasan.radio.domain.FmBand
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private val TickSpacing = 14.dp
private val DialHeight = 88.dp

/**
 * A horizontally scrollable tuning dial for the FM band, 87.5 - 108.0 MHz in 0.1 MHz steps.
 *
 * Drag the scale left or right under the fixed needle (it keeps coasting after a fling and ticks
 * with haptics on every channel). [onFrequencyChange] is called with kilohertz on every channel the
 * needle crosses, so the readout follows the finger; [onFrequencyCommitted] is called once the dial
 * settles, which is when the tuner chip is actually retuned (retuning on every tick would hammer it).
 *
 * @param fadeColor the colour of the surface behind the dial, used to fade the scale at both edges.
 */
@Composable
internal fun FmDial(
    frequencyKhz: Int,
    onFrequencyChange: (Int) -> Unit,
    onFrequencyCommitted: () -> Unit,
    enabled: Boolean,
    fadeColor: Color,
    tickColor: Color,
    needleColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val tickPx = with(LocalDensity.current) { TickSpacing.toPx() }
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val latestChange by rememberUpdatedState(onFrequencyChange)
    val latestCommit by rememberUpdatedState(onFrequencyCommitted)

    // Fractional channel index: 0 = 87.5 MHz ... STEP_COUNT = 108.0 MHz.
    var position by remember { mutableFloatStateOf(FmBand.indexOf(frequencyKhz).toFloat()) }
    var busy by remember { mutableStateOf(false) }
    var lastEmitted by remember { mutableStateOf(FmBand.indexOf(frequencyKhz)) }

    // Follow changes made from outside the dial (step buttons, the tuner confirming a frequency).
    LaunchedEffect(frequencyKhz) {
        val target = FmBand.indexOf(frequencyKhz)
        lastEmitted = target
        if (!busy && abs(position - target) >= 0.5f) {
            busy = true
            animate(position, target.toFloat()) { value, _ -> position = value }
            busy = false
        }
    }

    fun moveTo(newPosition: Float) {
        position = newPosition.coerceIn(0f, FmBand.STEP_COUNT.toFloat())
        val index = position.roundToInt()
        if (index != lastEmitted) {
            lastEmitted = index
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            latestChange(FmBand.khzAt(index))
        }
    }

    val dragState = rememberDraggableState { delta -> moveTo(position - delta / tickPx) }

    val majorLabels = remember(textMeasurer, labelColor) {
        (88..108).associateWith {
            textMeasurer.measure(it.toString(), TextStyle(color = labelColor, fontSize = 11.sp))
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(DialHeight)
            .clipToBounds()
            .semantics {
                contentDescription = "FM tuning dial"
                stateDescription = "${FmBand.mhzLabel(frequencyKhz)} megahertz"
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                onDragStarted = { busy = true },
                onDragStopped = { velocity ->
                    // Coast to a stop, then settle exactly on a channel and retune the chip once.
                    animateDecay(
                        initialValue = position,
                        initialVelocity = -velocity / tickPx,
                        animationSpec = FloatExponentialDecaySpec(frictionMultiplier = 2f),
                    ) { value, _ -> moveTo(value) }
                    animate(position, position.roundToInt().toFloat()) { value, _ -> moveTo(value) }
                    busy = false
                    latestCommit()
                },
            )
            .drawWithContent {
                drawContent()
                // Fade both ends of the scale into the surface behind it.
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to fadeColor,
                        0.18f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1f to fadeColor,
                    )
                )
            },
    ) {
        val centerX = size.width / 2f
        val visible = ceil(centerX / tickPx).toInt() + 2
        val first = (position.roundToInt() - visible).coerceAtLeast(0)
        val last = (position.roundToInt() + visible).coerceAtMost(FmBand.STEP_COUNT)
        val baseY = size.height - 12.dp.toPx()

        for (index in first..last) {
            val x = centerX + (index - position) * tickPx
            val khz = FmBand.khzAt(index)
            val (height, width) = when {
                khz % 1000 == 0 -> 30.dp.toPx() to 2.dp.toPx()
                khz % 500 == 0 -> 20.dp.toPx() to 1.5f.dp.toPx()
                else -> 11.dp.toPx() to 1.dp.toPx()
            }
            drawLine(
                color = tickColor,
                start = Offset(x, baseY),
                end = Offset(x, baseY - height),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
            if (khz % 1000 == 0) {
                majorLabels[khz / 1000]?.let { layout ->
                    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, baseY - height - layout.size.height - 2.dp.toPx()))
                }
            }
        }

        // The needle and its little pointer, fixed at the centre.
        drawLine(
            color = needleColor,
            start = Offset(centerX, 10.dp.toPx()),
            end = Offset(centerX, size.height),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        val pointer = Path().apply {
            moveTo(centerX - 6.dp.toPx(), 0f)
            lineTo(centerX + 6.dp.toPx(), 0f)
            lineTo(centerX, 10.dp.toPx())
            close()
        }
        drawPath(pointer, needleColor)
    }
}
