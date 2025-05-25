package com.tasnimulhasan.songs

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
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun CustomKnob(
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    var angle by remember { mutableFloatStateOf(-90f) }
    val strokeWidth = 20f
    var lastTouchAngle by remember { mutableFloatStateOf(-90f) }

    val clampedAngle = angle.coerceIn(-90f, 270f)
    val sweepPercent = ((clampedAngle + 90f) / 360f).coerceIn(0f, 1f)

    val context = LocalContext.current
    var lastStep by remember { mutableFloatStateOf(-1f) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    Box(
        modifier = modifier
            .size(120.dp)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset((size.width / 2).toFloat(), (size.height / 2).toFloat())
                    val touchPoint = change.position
                    val deltaX = touchPoint.x - center.x
                    val deltaY = touchPoint.y - center.y

                    // Raw angle in range (-180, 180]
                    val rawAngle = atan2(deltaY, deltaX) * (180f / PI).toFloat()

                    // Normalize to range (-90 to 270) for your use case
                    var adjustedAngle = rawAngle
                    if (adjustedAngle < -90f) adjustedAngle += 360f

                    // Handle small rotation jitter
                    val delta = adjustedAngle - lastTouchAngle
                    if (kotlin.math.abs(delta) < 40f) {
                        // Only allow movement within -90 to 270
                        val newAngle = (angle + delta).coerceIn(-90f, 270f)
                        angle = newAngle

                        // Haptic Feedback Every 5%
                        val progress = ((newAngle + 90f) / 360f).coerceIn(0f, 1f)
                        val step = ((progress * 100f) / 5f).toInt()
                        if (step.toFloat() != lastStep) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            lastStep = step.toFloat()
                        }

                        onValueChange(angle)
                        lastTouchAngle = adjustedAngle
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusArc = size.minDimension / 2f
            val radius = size.minDimension / 2.6f

            // Inactive arc (full background)
            drawArc(
                color = Color.DarkGray,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radiusArc, center.y - radiusArc),
                size = Size(radiusArc * 2, radiusArc * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active arc (progress)
            drawArc(
                color = Color.Green,
                startAngle = 135f,
                sweepAngle = 270f * sweepPercent,
                useCenter = false,
                topLeft = Offset(center.x - radiusArc, center.y - radiusArc),
                size = Size(radiusArc * 2, radiusArc * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Inner circle ring
            drawCircle(
                color = Color.Gray,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Rotating pointer
            rotate(degrees = clampedAngle, pivot = center) {
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = strokeWidth / 2
                )
            }
        }
    }
}

@Composable
fun CustomKnobProgressBar(volumeLevel: Float) {
    val barCount = 30
    val filledBars = (barCount * volumeLevel).roundToInt()

    Row(
        modifier = Modifier
            .height(25.dp)
            .padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(barCount) { i ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .weight(1f)
                    .background(
                        color = if (i < filledBars) Color.Green else Color.DarkGray
                    )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewCustomKnob() {
    CustomKnob(
        onValueChange = { _ -> }
    )
}