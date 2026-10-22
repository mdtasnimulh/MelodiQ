package com.tasnimulhasan.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import com.tasnimulhasan.designsystem.theme.glassEdge
import com.tasnimulhasan.designsystem.theme.isGlassUi
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
 * A solid, floating bottom bar: a dark pill of icon tabs where the selected tab becomes a white pill
 * that also shows its name ([showLabels]), and a separate round action button beside it.
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

    val hasSelection = selectedIndex in 0 until n

    val barHeight by animateDpAsState(
        if (minimized) 52.dp else 64.dp,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "barHeight",
    )

    // The selected tab is a white pill holding its icon and (optionally) its name; every other tab is a
    // bare icon. The pill is each tab's own background, and tab widths animate, so it glides between tabs.
    val pillBg = if (glass) Color.White.copy(alpha = 0.92f) else Color.White
    val pillContent = Color(0xFF111111)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── The pill ───────────────────────────────────────────────────────────────────
        Row(
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
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = hasSelection && index == selectedIndex
                val weight by animateFloatAsState(
                    targetValue = if (active && showLabels) 2.3f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    label = "tabWeight",
                )
                val pillAlpha by animateFloatAsState(if (active) 1f else 0f, tween(200), label = "pillAlpha")
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .padding(vertical = 5.dp, horizontal = 2.dp)
                        .clip(CircleShape)
                        .background(pillBg.copy(alpha = pillBg.alpha * pillAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!active) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            // Always report it: tapping the current tab means "take me to its root".
                            onSelect(index)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (active) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = if (active) pillContent else inactiveTint,
                            modifier = Modifier.size(if (minimized) 22.dp else 24.dp),
                        )
                        if (showLabels) {
                            AnimatedVisibility(
                                visible = active,
                                enter = fadeIn(tween(220)) + expandHorizontally(tween(260)),
                                exit = fadeOut(tween(120)) + shrinkHorizontally(tween(200)),
                            ) {
                                Text(
                                    text = item.label,
                                    color = pillContent,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(start = 6.dp),
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
