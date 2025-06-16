package com.tasnimulhasan.featureplayer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun CustomWaveProgressBar(
    amplitudes: List<Float>,
    currentProgress: Float,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean,
    barColor: Color = Color(0xFFFFC5BF),
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

    LaunchedEffect(currentProgress) {
        if (!isDragging) {
            dragProgress = currentProgress
        }
    }

    val animatedAmplitudes = remember(amplitudes.size) {
        List(amplitudes.size) { Animatable(amplitudes[it]) }
    }

    LaunchedEffect(isPlaying, currentProgress) {
        if (!isPlaying) return@LaunchedEffect

        val playedBars = (animatedAmplitudes.size * (if (isDragging) dragProgress else currentProgress)).toInt()
        coroutineScope {
            animatedAmplitudes.forEachIndexed { index, animatable ->
                if (index <= playedBars) {
                    launch {
                        val target = Random.nextFloat()
                        animatable.animateTo(
                            target,
                            animationSpec = tween(durationMillis = 150)
                        )
                    }
                }
            }
        }
        delay(150)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragProgress)
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        val newX = change.position.x
                        val newProgress = (newX / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        onSeek(newProgress)
                        change.consume()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    dragProgress = newProgress
                    onSeek(newProgress)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalBars = animatedAmplitudes.size
            val totalBarWidth = totalBars * (barWidthPx + spacePx) - spacePx
            val startX = (size.width - totalBarWidth) / 2f
            val maxHeight = size.height
            val playedBars = (totalBars * (if (isDragging) dragProgress else currentProgress)).toInt()

            animatedAmplitudes.forEachIndexed { index, amp ->
                val x = startX + index * (barWidthPx + spacePx)
                if (x > size.width) return@forEachIndexed

                val amplitude = if (index <= playedBars) amp.value else amplitudes[index]
                val barHeight = amplitude.coerceIn(0f, 1f) * maxHeight
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
        isPlaying = false,
        onSeek = { newProgress ->
            progress = newProgress
        }
    )
}