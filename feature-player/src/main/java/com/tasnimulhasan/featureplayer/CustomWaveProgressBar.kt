package com.tasnimulhasan.featureplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun CustomWaveProgressBar(
    amplitudes: List<Float>,           // List of values from 0f to 1f
    currentProgress: Float,            // 0f to 1f
    onSeek: (Float) -> Unit,           // Called when user seeks
    barColor: Color = Color(0xFFF89A90),
    playedColor: Color = Color(0xFFF3422D),
    barWidth: Dp = 2.5.dp,
    spaceBetween: Dp = 2.dp,
    cornerRadius: Dp = 1.dp
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val spacePx = with(density) { spaceBetween.toPx() }
    val cornerPx = with(density) { cornerRadius.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(currentProgress) }

    val displayProgress = if (isDragging) dragProgress else currentProgress

    Box(
        modifier = Modifier
            .fillMaxWidth()  // ✅ 85% of screen width
            .height(50.dp)
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newX = change.position.x
                    val newProgress = (newX / size.width).coerceIn(0f, 1f)
                    dragProgress = newProgress
                    onSeek(newProgress)
                    isDragging = true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    dragProgress = newProgress
                    onSeek(newProgress)
                }
            },
        contentAlignment = Alignment.Center // ✅ Ensures vertical centering
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalBars = amplitudes.size
            val totalBarWidth = totalBars * (barWidthPx + spacePx) - spacePx
            val startX = (size.width - totalBarWidth) / 2f // ✅ Horizontal centering
            val maxHeight = size.height
            val playedBars = (totalBars * displayProgress).toInt()

            amplitudes.forEachIndexed { index, amp ->
                val x = startX + index * (barWidthPx + spacePx)
                if (x > size.width) return@forEachIndexed

                val barHeight = amp.coerceIn(0f, 1f) * maxHeight
                val top = (size.height - barHeight) / 2
                drawRoundRect(
                    color = if (index <= playedBars) playedColor else barColor,
                    topLeft = Offset(x, top),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProgressBar() {
    val amplitudes = remember { List(60) { Random.nextFloat() } }
    var progress by remember { mutableFloatStateOf(0f) }
    CustomWaveProgressBar(
        amplitudes = amplitudes,
        currentProgress = progress,
        onSeek = { newProgress ->
            progress = newProgress
            // call your ViewModel or player service here to seek
        }
    )
}