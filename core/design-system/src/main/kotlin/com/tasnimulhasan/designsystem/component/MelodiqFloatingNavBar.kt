package com.tasnimulhasan.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * How much room the floating bar takes at the bottom of the screen. Lists add this to their
 * bottom content padding so their last item can scroll clear of the bar (the bar floats over
 * the content instead of reserving its own strip). 0.dp on screens that have no bar.
 */
val LocalFloatingBarPadding = compositionLocalOf { 0.dp }

data class FloatingNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val LensSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 420f)

/**
 * A floating, rounded "glass" tab bar in the style of the iOS tab bar:
 *  - a translucent capsule with a light edge highlight that floats above the content;
 *  - a selection "lens" that slides between tabs with a springy overshoot;
 *  - press and drag anywhere on the bar: the lens grows, follows your finger, tabs change as
 *    it passes over them (with a tick), and it settles on release;
 *  - [minimized] (set while the content scrolls down) shrinks the bar to icons only, and it
 *    expands again when scrolling back up.
 */
@Composable
fun MelodiqFloatingNavBar(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimized: Boolean = false,
) {
    val n = items.size
    if (n == 0) return
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val selected = selectedIndex.coerceIn(0, n - 1)
    val lensPos = remember { Animatable(selected.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(0) }

    // Follow external selection changes (drawer shortcuts, back navigation) unless the user
    // is currently dragging the lens.
    LaunchedEffect(selected) {
        if (!dragging) lensPos.animateTo(selected.toFloat(), LensSpring)
    }

    val barHeight by animateDpAsState(
        if (minimized) 54.dp else 68.dp,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "barHeight",
    )
    val sidePadding by animateDpAsState(
        if (minimized) 52.dp else 20.dp,
        spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "barSide",
    )
    val lensScale by animateFloatAsState(
        if (dragging) 1.12f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "lensScale",
    )

    val activeIndex by remember { derivedStateOf { lensPos.value.roundToInt().coerceIn(0, n - 1) } }
    val itemWidthPx = if (widthPx > 0) widthPx.toFloat() / n else 0f
    val itemWidthDp = with(density) { itemWidthPx.toDp() }

    val edge = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.32f))
    )
    val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = sidePadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.28f),
                )
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh.copy(alpha = 0.74f))
                .background(sheen)
                .border(0.8.dp, edge, CircleShape)
                .onSizeChanged { widthPx = it.width }
                .pointerInput(n, widthPx) {
                    if (widthPx == 0) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val itemW = size.width.toFloat() / n
                        fun indexAt(x: Float) = (x / itemW - 0.5f).coerceIn(0f, (n - 1).toFloat())

                        dragging = true
                        var lastTick = indexAt(down.position.x).roundToInt()
                        scope.launch { lensPos.animateTo(indexAt(down.position.x), LensSpring) }
                        var cancelled = false
                        var lastX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            lastX = change.position.x
                            if (change.positionChanged()) {
                                val pos = indexAt(change.position.x)
                                scope.launch { lensPos.snapTo(pos) }
                                val nearest = pos.roundToInt()
                                if (nearest != lastTick) {
                                    lastTick = nearest
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                            }
                            if (change.changedToUp()) break
                            if (change.isConsumed) { cancelled = true; break }
                        }

                        dragging = false
                        val target = if (cancelled) selected else indexAt(lastX).roundToInt()
                        scope.launch { lensPos.animateTo(target.toFloat(), LensSpring) }
                        if (target != selected) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        // Always report it: tapping the current tab means "take me to its root".
                        onSelect(target)
                    }
                },
        ) {
            // The lens: a tinted glass capsule behind the active tab.
            if (itemWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((lensPos.value * itemWidthPx).roundToInt(), 0) }
                        .width(itemWidthDp)
                        .fillMaxHeight()
                        .padding(5.dp)
                        .scale(lensScale)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = if (dragging) 0.26f else 0.18f))
                        .border(
                            0.8.dp,
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f))),
                            CircleShape,
                        ),
                )
            }

            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { index, item ->
                    val active = index == activeIndex
                    val tint = if (active) scheme.primary else scheme.onSurfaceVariant
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (active) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(if (minimized) 22.dp else 24.dp),
                        )
                        AnimatedVisibility(
                            visible = !minimized,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Text(
                                text = item.label,
                                color = tint,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
