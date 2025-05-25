package com.tasnimulhasan.songs

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

    var angle by remember { mutableFloatStateOf(0f) }
    val sweepPercent = ((angle + 180f) / 360f).coerceIn(0f, 0f)
    val radius = 100f
    val strokeWidth = 20f

    Box(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset((size.width / 2).toFloat(), (size.height / 2).toFloat())
                    val touchPoint = change.position
                    val deltaX = touchPoint.x - center.x
                    val deltaY = touchPoint.y - center.y
                    val newAngle = atan2(deltaY, deltaX) * (180f / PI).toFloat()
                    angle = newAngle
                    onValueChange(angle)
                }
            }
    ) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusArc = size.minDimension / 4f

            // Arc Progress Inactive
            drawArc(
                color = Color.DarkGray,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radiusArc, center.y - radiusArc),
                size = Size(radiusArc * 2, radiusArc * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Arc Progress Active
            drawArc(
                color = Color.Green,
                startAngle = 135f,
                sweepAngle = 270f * sweepPercent,
                useCenter = false,
                topLeft = Offset(center.x - radiusArc, center.y - radiusArc),
                size = Size(radiusArc * 2, radiusArc * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawCircle(
                color = Color.Gray,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            rotate(degrees = angle, pivot = center) {
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