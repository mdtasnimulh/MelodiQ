package com.tasnimulhasan.ui.reorder

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Long-press-then-drag reordering for a [androidx.compose.foundation.lazy.LazyColumn].
 *
 * While a row is being dragged the caller's list is rearranged live through [onSwap] (so the
 * other rows slide out of the way); when the finger lifts, [onDrop] reports where the row
 * started and where it ended, which is the moment to save the new order.
 *
 * Usage:
 *  - `val reorder = rememberDragReorderState(listState, onSwap = {...}, onDrop = {...})`
 *  - `LazyColumn(state = listState, userScrollEnabled = !reorder.isDragging, modifier = Modifier.dragReorder(reorder))`
 *  - every row: `Modifier.dragItem(reorder, key)` (and skip `animateItem()` for the dragged row).
 * Rows MUST have stable keys, and must not consume long-presses themselves.
 */
@Stable
class DragReorderState internal constructor(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val swap: () -> (Int, Int) -> Unit,
    private val drop: () -> (Int, Int) -> Unit,
    private val haptic: () -> Unit,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    private var draggedDistance by mutableFloatStateOf(0f)
    private var initialOffset = 0
    private var startIndex = -1
    private var currentIndex = -1

    val isDragging: Boolean get() = draggingKey != null

    /** Extra vertical shift for the dragged row, so it follows the finger however the list moves under it. */
    fun translationFor(key: Any): Float {
        if (key != draggingKey) return 0f
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return draggedDistance
        return initialOffset + draggedDistance - info.offset
    }

    internal fun start(y: Float) {
        val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { y.toInt() >= it.offset && y.toInt() < it.offset + it.size } ?: return
        draggingKey = hit.key
        startIndex = hit.index
        currentIndex = hit.index
        initialOffset = hit.offset
        draggedDistance = 0f
        haptic()
    }

    internal fun drag(dy: Float) {
        val key = draggingKey ?: return
        draggedDistance += dy
        val info = listState.layoutInfo
        val dragged = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        val top = initialOffset + draggedDistance
        val center = top + dragged.size / 2f
        val target = info.visibleItemsInfo.firstOrNull {
            it.key != key && center >= it.offset && center <= it.offset + it.size
        }
        if (target != null && target.index != currentIndex) {
            swap()(currentIndex, target.index)
            currentIndex = target.index
        }
        // Keep scrolling when the row is held near the top or bottom edge.
        val edge = 48f
        val up = info.viewportStartOffset + edge - top
        val down = top + dragged.size - (info.viewportEndOffset - edge)
        val scrollDelta = when {
            up > 0f -> -up.coerceAtMost(28f)
            down > 0f -> down.coerceAtMost(28f)
            else -> 0f
        }
        if (scrollDelta != 0f) scope.launch { listState.scrollBy(scrollDelta) }
    }

    internal fun end() {
        if (draggingKey == null) return
        val from = startIndex
        val to = currentIndex
        draggingKey = null
        draggedDistance = 0f
        startIndex = -1
        currentIndex = -1
        if (from >= 0 && to >= 0 && from != to) drop()(from, to)
    }
}

@Composable
fun rememberDragReorderState(
    listState: LazyListState,
    onSwap: (from: Int, to: Int) -> Unit,
    onDrop: (from: Int, to: Int) -> Unit,
): DragReorderState {
    val scope = rememberCoroutineScope()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val swap = rememberUpdatedState(onSwap)
    val dropped = rememberUpdatedState(onDrop)
    return remember(listState) {
        DragReorderState(
            listState = listState,
            scope = scope,
            swap = { swap.value },
            drop = { dropped.value },
            haptic = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
        )
    }
}

/** On the LazyColumn: long-press a row, then drag it up or down. */
fun Modifier.dragReorder(state: DragReorderState): Modifier = pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset -> state.start(offset.y) },
        onDrag = { change, amount ->
            change.consume()
            state.drag(amount.y)
        },
        onDragEnd = { state.end() },
        onDragCancel = { state.end() },
    )
}

/** On each row: follows the finger, lifts with a shadow and sits above its neighbours while dragged. */
fun Modifier.dragItem(state: DragReorderState, key: Any): Modifier = this
    .zIndex(if (state.draggingKey == key) 1f else 0f)
    .graphicsLayer {
        val dragging = state.draggingKey == key
        translationY = state.translationFor(key)
        val s = if (dragging) 1.03f else 1f
        scaleX = s
        scaleY = s
        shadowElevation = if (dragging) 18.dp.toPx() else 0f
    }
