package com.tasnimulhasan.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * One motion language for the whole app, so every screen enters, leaves and reacts to touch
 * the same way instead of each navigation file picking its own slide/fade.
 */
object MelodiqMotion {
    private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** Moving deeper (list -> details): new screen slides in a short way and fades up. */
    fun pushEnter(): EnterTransition =
        slideInHorizontally(tween(380, easing = Emphasized)) { it / 5 } + fadeIn(tween(300))

    /** The screen being covered drifts away a little and fades. */
    fun pushExit(): ExitTransition =
        slideOutHorizontally(tween(300, easing = EmphasizedAccelerate)) { -it / 6 } + fadeOut(tween(180))

    fun popEnter(): EnterTransition =
        slideInHorizontally(tween(380, easing = Emphasized)) { -it / 6 } + fadeIn(tween(300))

    fun popExit(): ExitTransition =
        slideOutHorizontally(tween(300, easing = EmphasizedAccelerate)) { it / 5 } + fadeOut(tween(180))

    /** Same-level switches (search, tabs): fade with a slight scale-up. */
    fun fadeEnter(): EnterTransition =
        fadeIn(tween(260)) + scaleIn(tween(260, easing = Emphasized), initialScale = 0.96f)

    fun fadeExit(): ExitTransition = fadeOut(tween(160))
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
