package com.tasnimulhasan.featureplayer.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasnimulhasan.designsystem.theme.LightOrange
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun CustomButtonGroups(
    buttonColor: Color,
    repeatModeOne: Boolean,
    repeatModeAll: Boolean,
    onRepeatButtonClicked: () -> Unit,
    onEQButtonClicked: () -> Unit,
    onSleepButtonClicked: () -> Unit,
    onShareButtonClicked: () -> Unit,
    onVolumeBoostClicked: () -> Unit,
    sleepTimerActive: Boolean = false,
    sleepTimerRemainingMillis: Long = 0L,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillActionButton(
                modifier = Modifier.weight(1f),
                buttonColor = buttonColor,
                onClick = onRepeatButtonClicked,
            ) { tint ->
                Icon(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    imageVector = if (repeatModeAll) Icons.Default.AllInclusive
                    else if (repeatModeOne) Icons.Default.RepeatOne
                    else Icons.Default.Repeat,
                    tint = tint,
                    contentDescription = "Repeat Button"
                )
            }

            PillActionButton(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                buttonColor = buttonColor,
                onClick = onEQButtonClicked,
            ) { tint ->
                Icon(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    imageVector = Icons.Default.GraphicEq,
                    tint = tint,
                    contentDescription = "Equalizer Button"
                )
            }

            PillActionButton(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                buttonColor = buttonColor,
                onClick = onSleepButtonClicked,
                isActive = sleepTimerActive,
            ) { tint ->
                /*if (sleepTimerActive) {
                    // Timer running: show the live countdown right on the button itself,
                    // instead of the icon, so it's visible without opening the sheet.
                    Text(
                        text = formatSleepPillTime(sleepTimerRemainingMillis),
                        color = tint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .width(24.dp)
                            .height(24.dp),
                        imageVector = Icons.Default.Timer,
                        tint = tint,
                        contentDescription = "Sleep Button"
                    )
                }*/
                Icon(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    imageVector = Icons.Default.Timer,
                    tint = tint,
                    contentDescription = "Sleep Button"
                )
            }

            PillActionButton(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                buttonColor = buttonColor,
                onClick = onShareButtonClicked,
            ) { tint ->
                Icon(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    imageVector = Icons.Default.Share,
                    tint = tint,
                    contentDescription = "Share Button"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillActionButton(
                modifier = Modifier.weight(1f),
                buttonColor = buttonColor,
                onClick = onVolumeBoostClicked,
            ) { tint ->
                Icon(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    tint = tint,
                    contentDescription = "Volume Boos Button"
                )
            }
        }
    }
}

/**
 * Shared pill-shaped action button. Adds a gentle press-scale for tactile feedback and,
 * when [isActive] is true (used for the sleep timer while it's running), a slow breathing
 * pulse so the active state reads clearly without being distracting.
 */
@Composable
private fun PillActionButton(
    modifier: Modifier = Modifier,
    buttonColor: Color,
    onClick: () -> Unit,
    isActive: Boolean = false,
    content: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "PillPressScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "PillActivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PillActivePulseAlpha"
    )

    val backgroundAlphaMultiplier = if (isActive) pulseAlpha else 1f
    val tint = buttonColor.copy(alpha = if (isActive) 1f else 0.75f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(35.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(25.dp))
            .background(
                color = buttonColor.copy(alpha = buttonColor.alpha * backgroundAlphaMultiplier),
                shape = RoundedCornerShape(25.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(tint)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewCustomButtonGroups() {
    CustomButtonGroups(
        buttonColor = LightOrange.copy(alpha = 0.05f),
        repeatModeOne = true,
        repeatModeAll = false,
        onRepeatButtonClicked = {},
        onEQButtonClicked = {},
        onSleepButtonClicked = {},
        onShareButtonClicked = {},
        onVolumeBoostClicked = {},
    )
}

private fun formatSleepPillTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}