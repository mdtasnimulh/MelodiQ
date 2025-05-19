package com.tasnimulhasan.songs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    thumbDisplay: (Float) -> String = { it.roundToInt().toString() }
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy
        ), label = "animatedValue"
    )

    val interaction = remember { MutableInteractionSource() }
    val isDragging by interaction.collectIsDraggedAsState()
    val density = LocalDensity.current

    // Animate thumb offset when dragging
    val offsetHeight by animateFloatAsState(
        targetValue = with(density) { if (isDragging) 36.dp.toPx() else 0.dp.toPx() },
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ), label = "offsetAnimation"
    )

    val thumbSize = 48.dp
    var width by remember { mutableIntStateOf(0) }

    Slider(
        value = animatedValue,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interaction,
        thumb = {
            Box(
                Modifier
                    .zIndex(10f)
                    .size(thumbSize)
                    .padding(10.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .background(
                        color = Color(0xFFECB91F), // Yellow thumb color
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thumbDisplay(animatedValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black
                )
            }
        },
        track = { sliderState ->
            val fraction by remember {
                derivedStateOf {
                    (animatedValue - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                }
            }

            val strokeColor = MaterialTheme.colorScheme.onSurface
            val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

            Box(
                Modifier
                    .clearAndSetSemantics { }
                    .height(thumbSize)
                    .fillMaxWidth()
                    .onSizeChanged { width = it.width }
            ) {
                // Custom track with path and graduations
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .drawWithCache {
                            onDrawBehind {
                                scale(
                                    scaleY = 1f,
                                    scaleX = if (isLtr) 1f else -1f
                                ) {
                                    drawSliderPath(
                                        fraction = fraction,
                                        offsetHeight = offsetHeight,
                                        color = strokeColor,
                                        steps = sliderState.steps
                                    )
                                }
                            }
                        }
                )
                // Thumb positioning
                Box(
                    Modifier
                        .zIndex(10f)
                        .align(Alignment.CenterStart)
                        .offset {
                            val thumbOffset = lerp(
                                start = -(thumbSize.toPx() / 2),
                                end = width - (thumbSize.toPx() / 2),
                                t = fraction
                            ).roundToInt()
                            IntOffset(
                                x = thumbOffset,
                                y = -offsetHeight.roundToInt()
                            )
                        }
                        .size(thumbSize)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Transparent)
                )
            }
        }
    )
}

private fun DrawScope.drawSliderPath(
    fraction: Float,
    offsetHeight: Float,
    color: Color,
    steps: Int
) {
    val path = Path()
    val activeWidth = size.width * fraction
    val midPointHeight = size.height / 2
    val curveHeight = midPointHeight - offsetHeight
    val beyondBounds = size.width * 2
    val ramp = 72.dp.toPx()

    // Path for the track
    path.moveTo(
        x = beyondBounds,
        y = midPointHeight
    )
    path.lineTo(
        x = activeWidth + ramp,
        y = midPointHeight
    )
    path.cubicTo(
        x1 = activeWidth + (ramp / 2),
        y1 = midPointHeight,
        x2 = activeWidth + (ramp / 2),
        y2 = curveHeight,
        x3 = activeWidth,
        y3 = curveHeight
    )
    path.cubicTo(
        x1 = activeWidth - (ramp / 2),
        y1 = curveHeight,
        x2 = activeWidth - (ramp / 2),
        y2 = midPointHeight,
        x3 = activeWidth - ramp,
        y3 = midPointHeight
    )
    path.lineTo(
        x = -beyondBounds,
        y = midPointHeight
    )
    val variation = 0.1f
    path.lineTo(
        x = -beyondBounds,
        y = midPointHeight + variation
    )
    path.lineTo(
        x = activeWidth - ramp,
        y = midPointHeight + variation
    )
    path.cubicTo(
        x1 = activeWidth - (ramp / 2),
        y1 = midPointHeight + variation,
        x2 = activeWidth - (ramp / 2),
        y2 = curveHeight + variation,
        x3 = activeWidth,
        y3 = curveHeight + variation
    )
    path.cubicTo(
        x1 = activeWidth + (ramp / 2),
        y1 = curveHeight + variation,
        x2 = activeWidth + (ramp / 2),
        y2 = midPointHeight + variation,
        x3 = activeWidth + ramp,
        y3 = midPointHeight + variation
    )
    path.lineTo(
        x = beyondBounds,
        y = midPointHeight + variation
    )

    // Exclude areas outside the slider bounds
    val exclude = Path().apply {
        addRect(Rect(-beyondBounds, -beyondBounds, 0f, beyondBounds))
        addRect(Rect(size.width, -beyondBounds, beyondBounds, beyondBounds))
    }

    val trimmedPath = Path()
    trimmedPath.op(path, exclude, PathOperation.Difference)

    // Draw active and inactive parts of the track
    clipRect(
        left = -beyondBounds,
        top = -beyondBounds,
        bottom = beyondBounds,
        right = activeWidth
    ) {
        drawTrimmedPath(trimmedPath, color)
    }
    clipRect(
        left = activeWidth,
        top = -beyondBounds,
        bottom = beyondBounds,
        right = beyondBounds
    ) {
        drawTrimmedPath(trimmedPath, color.copy(alpha = 0.2f))
    }

    // Draw graduation marks
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(trimmedPath, false)
    val graduations = steps + 1
    for (i in 0..graduations) {
        val pos = pathMeasure.getPosition(
            (i / graduations.toFloat()) * pathMeasure.length / 2
        )
        val height = 10f
        when (i) {
            0, graduations -> drawCircle(
                color = color,
                radius = 10f,
                center = pos
            )
            else -> drawLine(
                strokeWidth = if (pos.x < activeWidth) 4f else 2f,
                color = color,
                start = pos + Offset(0f, height),
                end = pos + Offset(0f, -height)
            )
        }
    }
}

private fun DrawScope.drawTrimmedPath(path: Path, color: Color) {
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 10f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

// Linear interpolation helper function
private fun lerp(start: Float, end: Float, t: Float): Float {
    return start + (end - start) * t
}