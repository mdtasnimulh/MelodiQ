package com.tasnimulhasan.featureplayer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Preset durations offered by the sleep timer. [END_OF_SONG] is resolved by the caller
 * against the currently playing track's remaining time.
 */
enum class SleepTimerOption(val label: String) {
    END_OF_SONG("Song end"),
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
    onCustomTimeSet: (hours: Int, minutes: Int, seconds: Int) -> Unit,
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
            // IMPORTANT: AnimatedContent renders its content slot inside its own internal
            // Box-based layout (needed to cross-fade the outgoing/incoming content). Any
            // composables emitted directly here as bare siblings get STACKED on top of each
            // other instead of flowing vertically - that was the actual cause of the
            // timer/grid/button overlap. Fix: TimerOptionsContent and RunningTimerContent
            // each wrap their own children in a real Column below, so AnimatedContent only
            // ever sees a single child per state.
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
                        },
                        onCustomTimeSet = { h, m, s ->
                            onCustomTimeSet(h, m, s)
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
    onCustomTimeSet: (hours: Int, minutes: Int, seconds: Int) -> Unit,
) {
    // Everything below MUST live inside this single Column - see the comment on
    // AnimatedContent above for why a bare sequence of composables here would overlap.
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
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

        // Quick presets, wrapped into fixed-height pill rows (no aspect-ratio squares) so
        // sizing never fights the sheet's own width/height measurement.
        val rows = SleepTimerOption.entries.chunked(4)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowOptions.forEach { option ->
                        SleepOptionPill(
                            modifier = Modifier.weight(1f),
                            option = option,
                            accentColor = accentColor,
                            onClick = { onOptionSelected(option) }
                        )
                    }
                    repeat(4 - rowOptions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        var showCustomPicker by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showCustomPicker = !showCustomPicker }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Set a custom time",
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (showCustomPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        if (showCustomPicker) {
            CustomTimePicker(
                accentColor = accentColor,
                onTimeSet = onCustomTimeSet
            )
        }
    }
}

@Composable
private fun SleepOptionPill(
    modifier: Modifier = Modifier,
    option: SleepTimerOption,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (option == SleepTimerOption.END_OF_SONG) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            text = option.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CustomTimePicker(
    accentColor: Color,
    onTimeSet: (hours: Int, minutes: Int, seconds: Int) -> Unit,
) {
    val hours = remember { mutableIntStateOf(0) }
    val minutes = remember { mutableIntStateOf(0) }
    val seconds = remember { mutableIntStateOf(0) }

    // Own Column too - this composable is called conditionally alongside sibling
    // composables above, so it needs to lay out its own children vertically regardless
    // of what container eventually hosts it.
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePickerColumn("Hours", hours, 0..12)
            TimePickerColumn("Minutes", minutes, 0..59)
            TimePickerColumn("Seconds", seconds, 0..59)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onTimeSet(hours.intValue, minutes.intValue, seconds.intValue) },
            enabled = hours.intValue > 0 || minutes.intValue > 0 || seconds.intValue > 0,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Timer")
        }
    }
}

@Composable
private fun TimePickerColumn(label: String, state: MutableIntState, range: IntRange) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 36.dp
    val visibleItemCount = 3

    // Snap to the nearest item in the middle
    val selectedIndex = remember {
        derivedStateOf {
            val visibleItemInfo = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val itemHeightPx = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
            val adjustedIndex = (visibleItemInfo + (offset.toFloat() / itemHeightPx).roundToInt())
                .coerceIn(range.first, range.last)
            adjustedIndex
        }
    }

    // Update state when snapping
    LaunchedEffect(selectedIndex.value) {
        if (state.intValue != selectedIndex.value) {
            state.intValue = selectedIndex.value
        }
    }

    // Scroll to initial value
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val initialIndex = state.intValue
            listState.scrollToItem(
                index = initialIndex,
                scrollOffset = -((visibleItemCount - 1) / 2) * itemHeight.value.toInt()
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .height(itemHeight * visibleItemCount)
                .width(64.dp),
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true,
        ) {
            itemsIndexed(range.toList()) { _, value ->
                Text(
                    text = value.toString().padStart(2, '0'),
                    color = if (value == state.intValue) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = if (value == state.intValue) 22.sp else 15.sp,
                    fontWeight = if (value == state.intValue) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                state.intValue = value
                                listState.animateScrollToItem(
                                    index = value,
                                    scrollOffset = -((visibleItemCount - 1) / 2) * itemHeight.value.toInt()
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun RunningTimerContent(
    remainingTimeMillis: Long,
    accentColor: Color,
    onCancelTimer: () -> Unit,
) {
    // Same fix as TimerOptionsContent - own Column so this doesn't stack inside
    // AnimatedContent's internal Box when the timer view is showing.
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
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