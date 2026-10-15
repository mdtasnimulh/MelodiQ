package com.tasnimulhasan.designsystem.component

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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.dp
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

/** The round button next to the bar (it opens a screen of its own instead of being a tab). */
data class FloatingNavAction(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

private val CapsuleSpring = spring<Float>(dampingRatio = 0.58f, stiffness = 400f)

/**
 * A solid, floating bottom bar: a dark pill with icon tabs and a lighter round highlight that
 * springs between them, and a separate round action button beside it.
 *  - tap a tab, or press and drag across the pill; the highlight follows and settles with a spring;
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
    action: FloatingNavAction? = null,
) {
    val n = items.size
    if (n == 0) return
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val pillColor = if (isDark) Color(0xFF2A2A2E) else Color(0xFF1C1C1E)
    val activeCircle = Color.White.copy(alpha = 0.16f)
    val inactiveTint = Color(0xFF8E8E93)

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

    val itemWidthPx = if (widthPx > 0) widthPx.toFloat() / n else 0f
    val itemWidthDp = with(density) { itemWidthPx.toDp() }
    val activeIndex by remember { derivedStateOf { capsulePos.value.roundToInt().coerceIn(0, n - 1) } }

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
                .border(0.8.dp, Color.White.copy(alpha = if (isDark) 0.12f else 0.08f), CircleShape)
                .onSizeChanged { widthPx = it.width }
                .pointerInput(n, widthPx) {
                    if (widthPx == 0) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val itemW = size.width.toFloat() / n
                        fun indexAt(x: Float) = (x / itemW - 0.5f).coerceIn(0f, (n - 1).toFloat())

                        pressing = true
                        var lastTick = indexAt(down.position.x).roundToInt()
                        scope.launch { capsulePos.animateTo(indexAt(down.position.x), CapsuleSpring) }
                        var cancelled = false
                        var lastX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            lastX = change.position.x
                            if (change.positionChanged()) {
                                val pos = indexAt(change.position.x)
                                scope.launch { capsulePos.snapTo(pos) }
                                val nearest = pos.roundToInt()
                                if (nearest != lastTick) {
                                    lastTick = nearest
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                            }
                            if (change.changedToUp()) break
                            if (change.isConsumed) { cancelled = true; break }
                        }

                        pressing = false
                        val target = if (cancelled) selected else indexAt(lastX).roundToInt()
                        scope.launch { capsulePos.animateTo(target.toFloat(), CapsuleSpring) }
                        if (target != selected || !hasSelection) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        // Always report it: tapping the current tab means "take me to its root".
                        onSelect(target)
                    }
                },
        ) {
            if (itemWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((capsulePos.value * itemWidthPx).roundToInt(), 0) }
                        .width(itemWidthDp)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .scale(capsuleScale)
                        .clip(CircleShape)
                        .background(activeCircle.copy(alpha = activeCircle.alpha * capsuleAlpha))
                )
            }
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { index, item ->
                    val active = hasSelection && index == activeIndex
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (active) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = if (active) Color.White else inactiveTint,
                            modifier = Modifier.size(if (minimized) 22.dp else 26.dp),
                        )
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
