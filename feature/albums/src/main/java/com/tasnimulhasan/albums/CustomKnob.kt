package com.tasnimulhasan.albums

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun CustomKnob(
    modifier: Modifier = Modifier,
    angle: Float, // incoming from parent
    onValueChange: (Float) -> Unit
) {
    val strokeWidth = 16f
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    // === NEW ===
    var currentAngle by remember { mutableFloatStateOf(angle) }
    var lastTouchAngle by remember { mutableFloatStateOf(-90f) }
    var lastStep by remember { mutableFloatStateOf(-1f) }

    // Sync external angle on recomposition (e.g. preset change)
    if (abs(currentAngle - angle) > 1f) {
        currentAngle = angle
    }

    val sweepPercent = ((currentAngle + 90f) / 360f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(100.dp)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset((size.width / 2).toFloat(), (size.height / 2).toFloat())
                    val touchPoint = change.position
                    val deltaX = touchPoint.x - center.x
                    val deltaY = touchPoint.y - center.y

                    var rawAngle = atan2(deltaY, deltaX) * (180f / PI).toFloat()
                    if (rawAngle < -90f) rawAngle += 360f

                    val delta = rawAngle - lastTouchAngle
                    if (abs(delta) < 40f) {
                        val newAngle = (currentAngle + delta).coerceIn(-90f, 270f)

                        val progress = ((newAngle + 90f) / 360f).coerceIn(0f, 1f)
                        val step = ((progress * 100f) / 5f).toInt()
                        if (step.toFloat() != lastStep) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    50,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                            lastStep = step.toFloat()
                        }

                        currentAngle = newAngle
                        onValueChange(newAngle)
                        lastTouchAngle = rawAngle
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusArc = size.minDimension / 2f
            val radius = size.minDimension / 2.6f

            drawArc(
                color = Color.DarkGray,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radiusArc, center.y - radiusArc),
                size = Size(radiusArc * 2, radiusArc * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val gain = ((currentAngle + 90f) / 360f * 3000f - 1500f) / 1000f // in dB
            val maxGain = 1.5f
            if (gain < 0f) {
                val leftSweep = 135f + (135f * (1f - abs(gain) / maxGain))
                drawArc(
                    color = Color.Green,
                    startAngle = leftSweep,
                    sweepAngle = 135f - (leftSweep - 135f),
                    useCenter = false,
                    topLeft = Offset(center.x - radiusArc, center.y - radiusArc),
                    size = Size(radiusArc * 2, radiusArc * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else if (gain > 0f) {
                val rightSweep = 135f + 135f
                val sweep = 135f * (gain / maxGain)
                drawArc(
                    color = Color.Green,
                    startAngle = rightSweep,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusArc, center.y - radiusArc),
                    size = Size(radiusArc * 2, radiusArc * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            drawCircle(
                color = Color.Gray,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            rotate(degrees = currentAngle, pivot = center) {
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = strokeWidth / 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun CustomKnobProgressBar(volumeLevel: Float) {
    val barCount = 31
    val centerIndex = barCount / 2
    val gainRange = 1.5f // +/-1.5dB range
    val gain = volumeLevel * 3f - 1.5f // Convert progress (0–1) to gain (-1.5f to 1.5f)
    val activeBars = ((abs(gain) / gainRange) * centerIndex).roundToInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(25.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until barCount) {
            val isLeft = i < centerIndex
            val isRight = i >= centerIndex + 1
            val isCenter = i == centerIndex

            val isActive = when {
                isCenter -> gain == 0f
                gain < 0 -> isLeft && i >= centerIndex - activeBars
                gain > 0 -> isRight && i <= centerIndex + activeBars
                else -> false
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .weight(1f)
                    .background(if (isActive) Color.Green else Color.DarkGray)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewCustomKnob() {
    CustomKnob(
        onValueChange = { _ -> },
        modifier = Modifier,
        angle = 0f
    )
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewCustomKnobBar() {
    CustomKnobProgressBar(
        volumeLevel = 45f
    )
}