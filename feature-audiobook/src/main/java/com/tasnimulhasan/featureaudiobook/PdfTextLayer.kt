package com.tasnimulhasan.featureaudiobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.magnifier
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
 *  - selection: long-press, drag to extend letter by letter, with a magnifier above the finger, like in any PDF app
 *    ([selStart]..[selEnd] are character offsets of the page text, both included, either order);
 *  - narration: shades the sentence being read ([sentenceStart]..[sentenceEnd], characters of the page text)
 *    and, darker, the word being spoken ([wordStart]..[wordEnd]). Use -1 for "none".
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    /** Search matches on this page as character ranges of the page text, and which one is the current one (-1 none). */
    matches: List<IntRange> = emptyList(),
    currentMatch: Int = -1,
    /** True only while a finger is dragging a selection (so the viewer can ignore pinches then). */
    onSelecting: (Boolean) -> Unit = {},
) {
    val selecting by rememberUpdatedState(onSelecting)
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { selecting(false) } }
    val scheme = MaterialTheme.colorScheme
    val sentenceColor = scheme.primary.copy(alpha = 0.22f)
    val wordColor = Color(0xFFFF9F0A).copy(alpha = 0.62f) // the spoken word: orange, distinct from the sentence tint
    val selectColor = Color(0xFF2F7DF6).copy(alpha = 0.32f)
    val matchColor = Color(0xFFFFEB3B).copy(alpha = 0.50f)
    val currentMatchColor = Color(0xFFFF9800).copy(alpha = 0.65f)

    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    // Where the finger is while a selection is being dragged (Unspecified = no magnifier).
    var magnifierAt by remember { mutableStateOf(Offset.Unspecified) }
    val currentWords by rememberUpdatedState(words)
    val select by rememberUpdatedState(onSelect)
    val clear by rememberUpdatedState(onClear)

    Canvas(
        modifier = modifier
            .magnifier(sourceCenter = { magnifierAt }, zoom = 1.7f)
            .onSizeChanged { layerSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress != null) {
                        val anchor = nearestChar(currentWords, longPress.position, layerSize, 56f)
                        if (anchor >= 0) {
                            select(anchor, anchor)
                            magnifierAt = longPress.position
                            selecting(true)
                            drag(longPress.id) { change ->
                                val i = nearestChar(currentWords, change.position, layerSize, 400f)
                                if (i >= 0) select(anchor, i)
                                magnifierAt = change.position
                                change.consume()
                            }
                            magnifierAt = Offset.Unspecified
                            selecting(false)
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
        matches.forEachIndexed { mi, m ->
            drawRuns(words, if (mi == currentMatch) currentMatchColor else matchColor) { _, k -> k.end > m.first && k.start <= m.last }
        }
        if (sentenceStart >= 0 && sentenceEnd > sentenceStart) {
            drawRuns(words, sentenceColor) { _, k -> k.end > sentenceStart && k.start < sentenceEnd }
        }
        if (selStart >= 0) {
            val a = minOf(selStart, selEnd)
            val b = maxOf(selStart, selEnd)
            // Letter by letter: a word at the edge of the selection is only covered as far as the chosen letter.
            drawSegments(words, selectColor) { _, k, out ->
                if (k.end > a && k.start <= b) {
                    out[0] = k.edgeX(maxOf(a, k.start) - k.start)
                    out[1] = k.edgeX(minOf(b, k.end - 1) - k.start + 1)
                    true
                } else false
            }
        }
        if (wordStart >= 0 && wordEnd > wordStart) {
            drawRuns(words, wordColor) { _, k -> k.end > wordStart && k.start < wordEnd }
        }
    }
}

/** The character (offset into the page text) under or nearest to [p], or -1 when nothing is within [maxDistance] pixels. */
private fun nearestChar(words: List<PageWord>, p: Offset, size: IntSize, maxDistance: Float): Int {
    val wi = nearestWord(words, p, size, maxDistance)
    if (wi < 0) return -1
    val k = words[wi]
    val n = k.end - k.start
    if (n <= 0) return k.start
    val fx = p.x / size.width.toFloat()
    var j = 0
    while (j < n - 1 && fx >= k.edgeX(j + 1)) j++
    return k.start + j
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
private fun DrawScope.drawRuns(words: List<PageWord>, color: Color, include: (Int, PageWord) -> Boolean) =
    drawSegments(words, color) { i, k, out ->
        if (include(i, k)) {
            out[0] = k.l
            out[1] = k.r
            true
        } else false
    }

/**
 * Fills part of each word: [segment] says whether a word is drawn and writes the left and right
 * edge (page fractions) into `out`. Neighbours on one line merge into one rounded bar.
 */
private fun DrawScope.drawSegments(words: List<PageWord>, color: Color, segment: (Int, PageWord, FloatArray) -> Boolean) {
    val out = FloatArray(2)
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
        if (!segment(i, k, out)) {
            flush()
            continue
        }
        val kl = out[0] * w
        val kt = k.t * h
        val kr = out[1] * w
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
