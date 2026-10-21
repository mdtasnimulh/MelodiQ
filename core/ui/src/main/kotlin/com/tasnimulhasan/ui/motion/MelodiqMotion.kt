package com.tasnimulhasan.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * One motion language for the whole app, so every screen enters, leaves and reacts to touch
 * the same way instead of each navigation file picking its own slide/fade.
 */
object MelodiqMotion {
    private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * Page turn, like frosted glass: going forward, the screen you leave drifts half a screen to
     * the left while it fades out, and the new one fades in while drifting in from the right.
     * Going back plays the same thing mirrored. The two fades overlap, so one page dissolves
     * into the other instead of one covering the other.
     */
    fun pushEnter(): EnterTransition =
        slideInHorizontally(tween(440, easing = Emphasized)) { it / 2 } +
            fadeIn(tween(360, delayMillis = 70, easing = LinearOutSlowInEasing))

    fun pushExit(): ExitTransition =
        slideOutHorizontally(tween(440, easing = Emphasized)) { -it / 2 } +
            fadeOut(tween(260, easing = FastOutLinearInEasing))

    fun popEnter(): EnterTransition =
        slideInHorizontally(tween(440, easing = Emphasized)) { -it / 2 } +
            fadeIn(tween(360, delayMillis = 70, easing = LinearOutSlowInEasing))

    fun popExit(): ExitTransition =
        slideOutHorizontally(tween(440, easing = Emphasized)) { it / 2 } +
            fadeOut(tween(260, easing = FastOutLinearInEasing))

    /** Kept for screens that still ask for a "same level" switch: they use the same page turn. */
    fun fadeEnter(): EnterTransition = pushEnter()

    fun fadeExit(): ExitTransition = pushExit()
}

/**
 * Springy "press in" feedback: the element shrinks slightly while a finger is down and
 * bounces back on release. Observes touches without consuming them, so clicks, long-presses
 * and scrolling keep working exactly as before.
 */
fun Modifier.pressFeedback(pressedScale: Float = 0.97f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressFeedback",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                pressed = false
            }
        }
}


private val EntranceEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * List / section entrance: the element fades in while rising a little, each one a beat after
 * the one before it ([index] x [stepMs]), so a screen "builds" itself instead of popping in.
 * Only the first [maxIndex] items animate; anything further down simply appears, which keeps
 * scrolling light and never replays the effect for rows that scroll back into view.
 */
fun Modifier.entrance(index: Int = 0, stepMs: Int = 45, maxIndex: Int = 10): Modifier = composed {
    val animate = index <= maxIndex
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index.coerceAtLeast(0) * stepMs.toLong())
            progress.animateTo(1f, tween(420, easing = EntranceEasing))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 28.dp.toPx()
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
    }
}
