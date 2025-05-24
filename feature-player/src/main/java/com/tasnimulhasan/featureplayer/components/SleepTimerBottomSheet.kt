package com.tasnimulhasan.featureplayer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.os.Process

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    onDismiss: () -> Unit,
    onTimeSet: (hours: Int, minutes: Int, seconds: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val hours = remember { mutableIntStateOf(0) }
    val minutes = remember { mutableIntStateOf(0) }
    val seconds = remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onDismiss.invoke() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set Sleep Timer", color = Color.Black, fontSize = 18.sp)

            Spacer(Modifier.height(16.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onTimeSet(hours.intValue, minutes.intValue, seconds.intValue)
                        onDismiss()
                    },
                    enabled = hours.intValue > 0 || minutes.intValue > 0 || seconds.intValue > 0
                ) {
                    Text("Set Timer")
                }

                Button(
                    onClick = { onDismiss() }
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun TimePickerColumn(label: String, state: MutableIntState, range: IntRange) {
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
        Text(label, color = Color.Gray, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .height(itemHeight * visibleItemCount)
                .width(60.dp),
            state = listState,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true,
        ) {
            itemsIndexed(range.toList()) { index, value ->
                Text(
                    text = value.toString().padStart(2, '0'),
                    color = if (value == state.intValue) Color.Black else Color.Gray.copy(alpha = 0.6f),
                    fontSize = if (value == state.intValue) 24.sp else 16.sp,
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