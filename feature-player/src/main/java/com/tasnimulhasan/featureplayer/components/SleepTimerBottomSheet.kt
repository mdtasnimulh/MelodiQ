package com.tasnimulhasan.featureplayer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Preset durations offered by the sleep timer. [END_OF_SONG] is resolved by the caller
 * against the currently playing track's remaining time.
 */
enum class SleepTimerOption(val label: String) {
    END_OF_SONG("End of song"),
    MIN_5("5 min"),
    MIN_10("10 min"),
    MIN_15("15 min"),
    MIN_30("30 min"),
    MIN_45("45 min"),
    HOUR_1("1 h"),
    HOUR_2("2 h"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    onDismiss: () -> Unit,
    isTimerRunning: Boolean,
    remainingTimeMillis: Long,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onOptionSelected: (SleepTimerOption) -> Unit,
    onCancelTimer: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cross-fades between "pick a duration" and "timer is running" states so
            // re-opening the sheet while a timer is active shows the live countdown
            // instead of the picker again.
            AnimatedContent(
                targetState = isTimerRunning,
                transitionSpec = {
                    fadeIn(tween(220)).togetherWith(fadeOut(tween(120)))
                },
                label = "SleepSheetContent"
            ) { running ->
                if (running) {
                    RunningTimerContent(
                        remainingTimeMillis = remainingTimeMillis,
                        accentColor = accentColor,
                        onCancelTimer = {
                            onCancelTimer()
                            onDismiss()
                        }
                    )
                } else {
                    TimerOptionsContent(
                        accentColor = accentColor,
                        onOptionSelected = {
                            onOptionSelected(it)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerOptionsContent(
    accentColor: Color,
    onOptionSelected: (SleepTimerOption) -> Unit,
) {
    Icon(
        imageVector = Icons.Default.Bedtime,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(32.dp)
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Sleep Timer",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Stop playback automatically",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
    )
    Spacer(Modifier.height(20.dp))

    val rows = SleepTimerOption.entries.chunked(3)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowOptions.forEach { option ->
                    Box(modifier = Modifier.weight(1f)) {
                        SleepOptionChip(
                            option = option,
                            accentColor = accentColor,
                            onClick = { onOptionSelected(option) }
                        )
                    }
                }
                repeat(3 - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SleepOptionChip(
    option: SleepTimerOption,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (option == SleepTimerOption.END_OF_SONG) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "End of\nsong",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
        } else {
            Text(
                text = option.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RunningTimerContent(
    remainingTimeMillis: Long,
    accentColor: Color,
    onCancelTimer: () -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bedtime,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(36.dp)
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Playback stops in",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = formatRemaining(remainingTimeMillis),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text = "Cancel timer",
        color = accentColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCancelTimer)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    )
}

private fun formatRemaining(millis: Long): String {
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
