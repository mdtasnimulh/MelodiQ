package com.tasnimulhasan.featureaudiobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

/**
 * A transparent layer laid over a rendered PDF page. It does two jobs:
 *  - selection: long-press a word, drag to extend, like in any PDF app ([selStart]..[selEnd] are word indexes);
 *  - narration: shades the sentence being read ([sentenceStart]..[sentenceEnd], characters of the page text)
 *    and, darker, the word being spoken ([wordStart]..[wordEnd]). Use -1 for "none".
 */
@Composable
internal fun PdfTextLayer(
    words: List<PageWord>,
    sentenceStart: Int,
    sentenceEnd: Int,
    wordStart: Int,
    wordEnd: Int,
    selStart: Int,
    selEnd: Int,
    onSelect: (anchor: Int, focus: Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val sentenceColor = scheme.primary.copy(alpha = 0.22f)
    val wordColor = Color(0xFFFF9F0A).copy(alpha = 0.62f) // the spoken word: orange, distinct from the sentence tint
    val selectColor = Color(0xFF2F7DF6).copy(alpha = 0.32f)

    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val currentWords by rememberUpdatedState(words)
    val select by rememberUpdatedState(onSelect)
    val clear by rememberUpdatedState(onClear)

    Canvas(
        modifier = modifier
            .onSizeChanged { layerSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress != null) {
                        val anchor = nearestWord(currentWords, longPress.position, layerSize, 56f)
                        if (anchor >= 0) {
                            select(anchor, anchor)
                            drag(longPress.id) { change ->
                                val i = nearestWord(currentWords, change.position, layerSize, 400f)
                                if (i >= 0) select(anchor, i)
                                change.consume()
                            }
                        }
                    } else {
                        // Lifted without moving or waiting: a tap, which clears the selection.
                        val change = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (change != null && !change.pressed && !change.isConsumed) clear()
                    }
                }
            },
    ) {
        if (words.isEmpty()) return@Canvas
        if (sentenceStart >= 0 && sentenceEnd > sentenceStart) {
            drawRuns(words, sentenceColor) { _, k -> k.end > sentenceStart && k.start < sentenceEnd }
        }
        if (selStart >= 0) {
            val a = minOf(selStart, selEnd)
            val b = maxOf(selStart, selEnd)
            drawRuns(words, selectColor) { i, _ -> i in a..b }
        }
        if (wordStart >= 0 && wordEnd > wordStart) {
            drawRuns(words, wordColor) { _, k -> k.end > wordStart && k.start < wordEnd }
        }
    }
}

/** The word closest to [p] (pixels), or -1 when nothing is within [maxDistance] pixels. */
private fun nearestWord(words: List<PageWord>, p: Offset, size: IntSize, maxDistance: Float): Int {
    if (words.isEmpty() || size.width <= 0 || size.height <= 0) return -1
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    var best = -1
    var bestD = Float.MAX_VALUE
    for (i in words.indices) {
        val k = words[i]
        val l = k.l * w
        val r = k.r * w
        val t = k.t * h
        val b = k.b * h
        val dx = if (p.x < l) l - p.x else if (p.x > r) p.x - r else 0f
        val dy = if (p.y < t) t - p.y else if (p.y > b) p.y - b else 0f
        val d = dx * dx + dy * dy
        if (d < bestD) {
            bestD = d
            best = i
        }
    }
    return if (bestD <= maxDistance * maxDistance) best else -1
}

/** Fills the words picked by [include]; neighbours on one line are drawn as one rounded bar. */
private fun DrawScope.drawRuns(words: List<PageWord>, color: Color, include: (Int, PageWord) -> Boolean) {
    val w = size.width
    val h = size.height
    var l = 0f
    var t = 0f
    var r = 0f
    var b = 0f
    var open = false
    fun flush() {
        if (open) {
            drawRoundRect(
                color = color,
                topLeft = Offset(l - 2f, t - 1f),
                size = Size(r - l + 4f, b - t + 2f),
                cornerRadius = CornerRadius(5f, 5f),
            )
            open = false
        }
    }
    for (i in words.indices) {
        val k = words[i]
        if (!include(i, k)) {
            flush()
            continue
        }
        val kl = k.l * w
        val kt = k.t * h
        val kr = k.r * w
        val kb = k.b * h
        if (open && abs(kt - t) < (b - t) * 0.6f && kl >= l) {
            r = maxOf(r, kr)
            b = maxOf(b, kb)
            t = minOf(t, kt)
        } else {
            flush()
            l = kl
            t = kt
            r = kr
            b = kb
            open = true
        }
    }
    flush()
}
