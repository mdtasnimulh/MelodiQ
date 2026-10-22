package com.tasnimulhasan.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import com.tasnimulhasan.designsystem.theme.glassEdge
import com.tasnimulhasan.designsystem.theme.isGlassUi
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.math.floor
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

/** The round button next to the bar (it opens a screen of its own instead of being a tab). */
data class FloatingNavAction(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

private val CapsuleSpring = spring<Float>(dampingRatio = 0.58f, stiffness = 400f)

/**
 * A solid, floating bottom bar: a dark pill of icon tabs with a lighter round highlight that springs
 * between them (tap, or press and drag across the pill); with [showLabels] the highlight widens into a
 * pill holding the selected tab's icon and name. A separate round action button sits beside it.
 *  - [selectedIndex] = -1 means no tab is current (the action screen is open);
 *  - [minimized] (set while the content scrolls down) shrinks the bar, and it grows back on scroll up.
 */
@Composable
fun MelodiqFloatingNavBar(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimized: Boolean = false,
    showLabels: Boolean = true,
    action: FloatingNavAction? = null,
) {
    val n = items.size
    if (n == 0) return
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val glass = isGlassUi()
    val pillColor = when {
        glass -> scheme.surfaceContainerHighest
        isDark -> Color(0xFF2A2A2E)
        else -> Color(0xFF1C1C1E)
    }
    val inactiveTint = if (glass) Color.White.copy(alpha = 0.66f) else Color(0xFF8E8E93)

    val activeCircle = Color.White.copy(alpha = if (glass) 0.26f else 0.16f)
    val scope = rememberCoroutineScope()

    val hasSelection = selectedIndex in 0 until n
    val selected = selectedIndex.coerceIn(0, n - 1)
    val capsulePos = remember { Animatable(selected.toFloat()) }
    var pressing by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(0) }

    LaunchedEffect(selected) {
        if (!pressing) capsulePos.animateTo(selected.toFloat(), CapsuleSpring)
    }

    val barHeight by animateDpAsState(
        if (minimized) 52.dp else 64.dp,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "barHeight",
    )
    val capsuleScale by animateFloatAsState(
        if (pressing) 1.1f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "capsuleScale",
    )
    val capsuleAlpha by animateFloatAsState(if (hasSelection) 1f else 0f, label = "capsuleAlpha")

    // With labels on, the tab under the capsule is wider (it holds icon + name). Widths are in "weight
    // units": every tab is 1, and the tab(s) the capsule is over share [extra] more, so the total never changes.
    val extra = if (showLabels) 1.5f else 0f
    val totalUnits = n + extra
    val unitPx = if (widthPx > 0) widthPx / totalUnits else 0f
    fun tent(pos: Float, i: Int) = (1f - abs(pos - i)).coerceIn(0f, 1f)
    fun unitsOf(pos: Float, i: Int) = 1f + extra * tent(pos, i)
    /** Left edge of the capsule (weight units) when it is at fractional tab position [pos]. */
    fun capsuleLeft(pos: Float): Float {
        val p = pos.coerceIn(0f, (n - 1).toFloat())
        val k = floor(p).toInt().coerceAtMost(n - 1)
        var left = 0f
        for (j in 0 until k) left += unitsOf(p, j)
        return left + (p - k) * unitsOf(p, k)
    }
    /** Which tab sits under horizontal position [x] (px) with the capsule at [pos]. */
    fun tabAt(x: Float, pos: Float): Int {
        var acc = 0f
        val u = x / unitPx.coerceAtLeast(0.0001f)
        for (j in 0 until n) {
            acc += unitsOf(pos, j)
            if (u < acc) return j
        }
        return n - 1
    }
    /** Fractional tab position whose capsule is centred under [x] (px); used while dragging. */
    fun posAt(x: Float): Float {
        val target = x / unitPx.coerceAtLeast(0.0001f)
        var lo = 0f
        var hi = (n - 1).toFloat()
        repeat(14) {
            val mid = (lo + hi) / 2f
            if (capsuleLeft(mid) + (1f + extra) / 2f < target) lo = mid else hi = mid
        }
        return (lo + hi) / 2f
    }

    val capsuleWidthDp = with(density) { (unitPx * (1f + extra)).toDp() }
    val pos = capsulePos.value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── The pill ───────────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .height(barHeight)
                .shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.35f))
                .clip(CircleShape)
                .background(pillColor)
                .then(
                    if (glass) Modifier.border(1.dp, glassEdge(), CircleShape)
                    else Modifier.border(0.8.dp, Color.White.copy(alpha = if (isDark) 0.12f else 0.08f), CircleShape)
                )
                .onSizeChanged { widthPx = it.width }
                .pointerInput(n, widthPx, showLabels, selected) {
                    if (widthPx == 0) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slop = 8.dp.toPx()
                        val startPos = capsulePos.value
                        val hit = tabAt(down.position.x, startPos)

                        pressing = true
                        var lastTick = hit
                        scope.launch { capsulePos.animateTo(hit.toFloat(), CapsuleSpring) }
                        var cancelled = false
                        var dragging = false
                        var lastX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            lastX = change.position.x
                            if (!dragging && abs(lastX - down.position.x) > slop) dragging = true
                            if (dragging && change.positionChanged()) {
                                val p = posAt(change.position.x)
                                scope.launch { capsulePos.snapTo(p) }
                                val nearest = p.roundToInt()
                                if (nearest != lastTick) {
                                    lastTick = nearest
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                            }
                            if (change.changedToUp()) break
                            if (change.isConsumed) { cancelled = true; break }
                        }

                        pressing = false
                        val target = when {
                            cancelled -> selected
                            dragging -> posAt(lastX).roundToInt().coerceIn(0, n - 1)
                            else -> hit
                        }
                        scope.launch { capsulePos.animateTo(target.toFloat(), CapsuleSpring) }
                        if (target != selected || !hasSelection) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        // Always report it: tapping the current tab means "take me to its root".
                        onSelect(target)
                    }
                },
        ) {
            if (unitPx > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((capsuleLeft(capsulePos.value) * unitPx).roundToInt(), 0) }
                        .width(capsuleWidthDp)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .scale(capsuleScale)
                        .clip(CircleShape)
                        .background(activeCircle.copy(alpha = activeCircle.alpha * capsuleAlpha))
                )
            }
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { index, item ->
                    val t = if (hasSelection) tent(pos, index) else 0f
                    val active = hasSelection && t > 0.5f
                    Box(Modifier.weight(unitsOf(pos, index)).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (active) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = if (active) Color.White else inactiveTint,
                                modifier = Modifier.size(if (minimized) 22.dp else 26.dp),
                            )
                            if (showLabels && t > 0.01f) {
                                // The name grows out of the icon as the capsule arrives and folds back as it leaves.
                                Text(
                                    text = item.label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .layout { measurable, constraints ->
                                            val p = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                                            val w = ((p.width + 6.dp.roundToPx()) * t).roundToInt()
                                            layout(w, p.height) { p.placeRelative(6.dp.roundToPx(), 0) }
                                        }
                                        .clipToBounds()
                                        .graphicsLayer { alpha = ((t - 0.2f) / 0.8f).coerceIn(0f, 1f) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Round action button ────────────────────────────────────────────────────────
        if (action != null) {
            val size by animateDpAsState(
                if (minimized) 52.dp else 64.dp,
                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
                label = "actionSize",
            )
            val pressScale by animateFloatAsState(
                if (action.selected) 1.06f else 1f,
                spring(dampingRatio = 0.5f, stiffness = 400f),
                label = "actionScale",
            )
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(size)
                    .scale(pressScale)
                    .shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .then(
                        if (action.selected) Modifier.border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape) else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        action.onClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(if (minimized) 24.dp else 28.dp),
                )
            }
        }
    }
}
