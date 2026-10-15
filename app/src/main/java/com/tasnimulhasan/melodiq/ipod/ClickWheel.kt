package com.tasnimulhasan.melodiq.ipod

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

private val LabelGrey = Color(0xFF9B9BA0)

/**
 * The iPod click wheel.
 *  - drag a finger around the ring: [onRotate] fires once per ~17 degrees, +1 clockwise, -1 anticlockwise;
 *  - tap MENU / the left / right / bottom labels: [onMenu] / [onPrevious] / [onNext] / [onPlayPause];
 *  - press and hold the left or right label: [onHoldSeek] repeats (false = backwards, true = forwards);
 *  - press and hold MENU: [onMenuHold];
 *  - tap the centre button: [onCenter].
 * The whole thing is one pointer-input surface so a finger can start on a label and turn into a
 * rotation without lifting, exactly like the hardware.
 */
@Composable
internal fun ClickWheel(
    onRotate: (Int) -> Unit,
    onCenter: () -> Unit,
    onMenu: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onHoldSeek: (forward: Boolean) -> Unit,
    onMenuHold: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotate by rememberUpdatedState(onRotate)
    val center by rememberUpdatedState(onCenter)
    val menu by rememberUpdatedState(onMenu)
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    val playPause by rememberUpdatedState(onPlayPause)
    val holdSeek by rememberUpdatedState(onHoldSeek)
    val menuHold by rememberUpdatedState(onMenuHold)
    val feedback by rememberUpdatedState(onFeedback)

    val scope = rememberCoroutineScope()
    var centerPressed by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(6.dp, CircleShape, ambientColor = Color(0x33000000), spotColor = Color(0x44000000))
            .background(
                Brush.radialGradient(
                    0.0f to Color.White,
                    0.7f to Color(0xFFF6F6F6),
                    1.0f to Color(0xFFE3E3E3),
                ),
                CircleShape,
            )
            .border(1.dp, Color(0xFFD2D2D4), CircleShape)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val radius = this.size.width / 2f
                    val rel0 = down.position - c
                    val dist0 = rel0.getDistance()
                    if (dist0 > radius) return@awaitEachGesture

                    // ── Centre button: a plain tap ───────────────────────────────────────
                    if (dist0 < radius * 0.40f) {
                        down.consume()
                        centerPressed = true
                        val up = waitForUpOrCancellation()
                        centerPressed = false
                        if (up != null) {
                            up.consume()
                            feedback()
                            center()
                        }
                        return@awaitEachGesture
                    }

                    // ── Ring: tap a label, hold prev/next, or rotate ─────────────────────
                    down.consume()
                    val zone = zoneOf(atan2(rel0.y, rel0.x))
                    var lastAngle = atan2(rel0.y, rel0.x)
                    var accumulated = 0f
                    var total = 0f
                    var rotated = false
                    var holdFired = false
                    var lastTickAt = 0L

                    // Turning fast moves several rows per tick (like the real wheel), so a
                    // library of thousands of songs can be crossed in a couple of spins.
                    fun tick(direction: Int) {
                        val now = SystemClock.uptimeMillis()
                        val gap = now - lastTickAt
                        lastTickAt = now
                        val multiplier = when {
                            gap < 35 -> 4
                            gap < 70 -> 2
                            else -> 1
                        }
                        feedback()
                        rotate(direction * multiplier)
                    }

                    var holdJob: Job? = null
                    if (zone == Zone.MENU) {
                        holdJob = scope.launch {
                            delay(500)
                            holdFired = true
                            feedback()
                            menuHold()
                        }
                    } else if (zone == Zone.NEXT || zone == Zone.PREVIOUS) {
                        holdJob = scope.launch {
                            delay(450)
                            holdFired = true
                            while (isActive) {
                                holdSeek(zone == Zone.NEXT)
                                delay(250)
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUp()) { change.consume(); break }
                        if (!change.pressed) break

                        val rel = change.position - c
                        if (rel.getDistance() < 12f) continue
                        val angle = atan2(rel.y, rel.x)
                        var delta = angle - lastAngle
                        if (delta > PI.toFloat()) delta -= TWO_PI
                        if (delta < -PI.toFloat()) delta += TWO_PI
                        lastAngle = angle
                        accumulated += delta
                        total += delta

                        if (!rotated && abs(total) > ROTATE_SLOP) {
                            rotated = true
                            holdJob?.cancel()
                        }
                        if (rotated) {
                            while (accumulated >= STEP) { accumulated -= STEP; tick(1) }
                            while (accumulated <= -STEP) { accumulated += STEP; tick(-1) }
                        }
                        change.consume()
                    }
                    holdJob?.cancel()

                    if (!rotated && !holdFired) {
                        feedback()
                        when (zone) {
                            Zone.MENU -> menu()
                            Zone.NEXT -> next()
                            Zone.PLAY_PAUSE -> playPause()
                            Zone.PREVIOUS -> previous()
                        }
                    }
                }
            },
    ) {
        val labelOffset = with(androidx.compose.ui.platform.LocalDensity.current) { (size.width * 0.37f).toDp() }

        Text(
            text = "MENU",
            color = LabelGrey,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center).offset(y = -labelOffset),
        )
        Icon(
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = "Previous",
            tint = LabelGrey,
            modifier = Modifier.align(Alignment.Center).offset(x = -labelOffset).size(30.dp),
        )
        Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Next",
            tint = LabelGrey,
            modifier = Modifier.align(Alignment.Center).offset(x = labelOffset).size(30.dp),
        )
        Row(
            modifier = Modifier.align(Alignment.Center).offset(y = labelOffset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play / pause", tint = LabelGrey, modifier = Modifier.size(24.dp))
            Icon(Icons.Filled.Pause, contentDescription = null, tint = LabelGrey, modifier = Modifier.size(22.dp).padding(start = 0.dp))
        }

        // Centre button.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.40f)
                .aspectRatio(1f)
                .background(
                    Brush.radialGradient(
                        0.0f to if (centerPressed) Color(0xFFD8D8D8) else Color(0xFFEFEFEF),
                        0.75f to if (centerPressed) Color(0xFFCDCDCD) else Color(0xFFE4E4E4),
                        1.0f to Color(0xFFD4D4D4),
                    ),
                    CircleShape,
                )
                .border(1.dp, Color(0xFFCFCFD1), CircleShape),
        )
    }
}

private enum class Zone { MENU, NEXT, PLAY_PAUSE, PREVIOUS }

/** Screen coordinates: 0 = right, +90 degrees = down. */
private fun zoneOf(angle: Float): Zone {
    val q = PI.toFloat() / 4f
    return when {
        angle >= -3 * q && angle < -q -> Zone.MENU
        angle >= -q && angle < q -> Zone.NEXT
        angle >= q && angle < 3 * q -> Zone.PLAY_PAUSE
        else -> Zone.PREVIOUS
    }
}

private const val TWO_PI = (2 * PI).toFloat()
private const val STEP = 0.30f          // ~17 degrees of travel per tick
private const val ROTATE_SLOP = 0.22f   // ~12 degrees before a touch counts as a rotation
