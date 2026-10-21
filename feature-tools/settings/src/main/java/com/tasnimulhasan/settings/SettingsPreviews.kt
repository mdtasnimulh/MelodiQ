package com.tasnimulhasan.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.designsystem.component.Icon
import com.tasnimulhasan.entity.enums.CoverArtStyle
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.entity.enums.MiniPlayerPosition
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.enums.VisualizerStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────────────────
// Shared pieces. Every preview is drawn with Compose (no images), so it always uses the app's
// real colours and stays sharp. Nothing here reads or changes a setting.
// ─────────────────────────────────────────────────────────────────────────────────────────

/** The soft card every preview sits in. */
@Composable
internal fun PreviewPanel(
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center, content = content)
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** A phone-shaped frame. The caller sets the width; the height follows. */
@Composable
private fun Phone(
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .aspectRatio(0.52f)
            .clip(shape)
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), shape)
            .background(background),
        content = content,
    )
}

@Composable
private fun SampleArt(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(0.dp)) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.fillMaxSize(0.38f))
    }
}

@Composable
private fun Line(widthFraction: Float, color: Color, height: Dp = 5.dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(widthFraction).height(height).clip(CircleShape).background(color))
}

@Composable
private fun SeekLine(progress: Float, active: Color, track: Color) {
    Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(active))
    }
}

@Composable
private fun TransportRow(tint: Color, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.SkipPrevious, null, tint = tint, modifier = Modifier.size(15.dp))
        Box(Modifier.size(24.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Icon(Icons.Filled.SkipNext, null, tint = tint, modifier = Modifier.size(15.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Player cover art
// ─────────────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun CoverArtPreview(style: CoverArtStyle) {
    PreviewPanel(
        caption = when (style) {
            CoverArtStyle.HALF -> "Half screen: the cover fills the top of the player."
            CoverArtStyle.CIRCLE -> "Circle: round artwork in the middle (also needed for the circular visualizer)."
            CoverArtStyle.FULL -> "Full screen: the cover fills the whole player with controls on top."
        },
    ) {
        Crossfade(targetState = style, animationSpec = tween(220), label = "coverPreview") { s ->
            PlayerMock(s, Modifier.width(128.dp))
        }
    }
}

@Composable
private fun PlayerMock(style: CoverArtStyle, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Phone(modifier) {
        when (style) {
            CoverArtStyle.HALF -> Column(Modifier.fillMaxSize()) {
                SampleArt(
                    Modifier.fillMaxWidth().weight(1.15f),
                    RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Line(0.7f, scheme.onBackground, 6.dp)
                        Line(0.42f, scheme.onSurfaceVariant.copy(alpha = 0.6f), 4.dp)
                    }
                    SeekLine(0.4f, scheme.primary, scheme.outlineVariant)
                    TransportRow(scheme.onSurfaceVariant, scheme.primary)
                }
            }

            CoverArtStyle.CIRCLE -> Column(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                SampleArt(
                    Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .border(3.dp, scheme.primary.copy(alpha = 0.35f), CircleShape)
                        .padding(5.dp),
                    CircleShape,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Line(0.6f, scheme.onBackground, 6.dp)
                    Line(0.36f, scheme.onSurfaceVariant.copy(alpha = 0.6f), 4.dp)
                }
                SeekLine(0.4f, scheme.primary, scheme.outlineVariant)
                TransportRow(scheme.onSurfaceVariant, scheme.primary)
            }

            CoverArtStyle.FULL -> Box(Modifier.fillMaxSize()) {
                SampleArt(Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)))
                )
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Line(0.7f, Color.White, 6.dp)
                        Line(0.42f, Color.White.copy(alpha = 0.65f), 4.dp)
                    }
                    SeekLine(0.4f, Color.White, Color.White.copy(alpha = 0.35f))
                    TransportRow(Color.White, scheme.primary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Theme and accent colour
// ─────────────────────────────────────────────────────────────────────────────────────────

private class MockPalette(val bg: Color, val surface: Color, val text: Color, val dim: Color)

private val LightMock = MockPalette(Color(0xFFFFFBFE), Color(0xFFECE6F0), Color(0xFF1C1B1F), Color(0xFFCAC4D0))
private val DarkMock = MockPalette(Color(0xFF1C1B1F), Color(0xFF2B2930), Color(0xFFE6E1E5), Color(0xFF49454F))
private val AmoledMock = MockPalette(Color.Black, Color(0xFF141414), Color(0xFFE6E1E5), Color(0xFF3A3A3A))

/** A tiny home screen: title, songs, mini player and bottom bar in the given palette and accent. */
@Composable
private fun AppMock(palette: MockPalette, accent: Color, modifier: Modifier = Modifier, rows: Int = 3) {
    Column(
        modifier = modifier.fillMaxSize().background(palette.bg).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Line(0.45f, palette.text, 7.dp)
        repeat(rows) { i ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (i == 0) accent else palette.dim)
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Line(if (i == 0) 0.9f else 0.7f, if (i == 0) accent else palette.text.copy(alpha = 0.8f), 4.dp, Modifier.width(34.dp))
                    Line(0.6f, palette.dim, 3.dp, Modifier.width(34.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(8.dp)).background(palette.surface).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Line(0.8f, palette.text.copy(alpha = 0.8f), 3.dp)
                Line(0.5f, palette.dim, 2.dp)
            }
            Box(Modifier.size(11.dp).clip(CircleShape).background(accent))
        }
        Row(
            Modifier.fillMaxWidth().height(16.dp).clip(CircleShape).background(Color(0xFF1C1C1E)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF8E8E93)))
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF8E8E93)))
        }
    }
}

/** Four selectable thumbnails: Follow system (half light / half dark), Light, Dark and AMOLED. */
@Composable
internal fun ThemePicker(
    selected: DarkThemeConfig,
    label: (DarkThemeConfig) -> String,
    onSelected: (DarkThemeConfig) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DarkThemeConfig.entries.forEach { option ->
            val isSelected = option == selected
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .selectable(selected = isSelected, onClick = { onSelected(option) })
                    .padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.58f)
                        .clip(shape)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
                            shape = shape,
                        )
                ) {
                    when (option) {
                        DarkThemeConfig.LIGHT -> AppMock(LightMock, accent, rows = 2)
                        DarkThemeConfig.DARK -> AppMock(DarkMock, accent, rows = 2)
                        DarkThemeConfig.AMOLED -> AppMock(AmoledMock, accent, rows = 2)
                        DarkThemeConfig.FOLLOW_SYSTEM -> Row(Modifier.fillMaxSize()) {
                            BoxWithHalf(alignEnd = false) { w -> AppMock(LightMock, accent, Modifier.requiredWidth(w), rows = 2) }
                            BoxWithHalf(alignEnd = true) { w -> AppMock(DarkMock, accent, Modifier.requiredWidth(w), rows = 2) }
                        }
                    }
                }
                Text(
                    text = label(option).substringBefore(" ("),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Shows only the left or right half of content that is laid out at the full width. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.BoxWithHalf(alignEnd: Boolean, content: @Composable (Dp) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
        val full = maxWidth * 2
        Box(Modifier.align(if (alignEnd) Alignment.TopEnd else Alignment.TopStart).requiredWidth(full).fillMaxHeight()) {
            content(full)
        }
    }
}

/** How the accent colour looks across the app: a home screen and a player, in the live colours. */
@Composable
internal fun AccentPreview() {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val palette = if (scheme.background == Color.Black) AmoledMock else if (dark) DarkMock else LightMock
    PreviewPanel(caption = "Buttons, the playing song, sliders and the navigation bar use this colour.") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Phone(Modifier.width(104.dp), background = palette.bg) { AppMock(palette, scheme.primary) }
            PlayerMock(CoverArtStyle.HALF, Modifier.width(104.dp))
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

// ─────────────────────────────────────────────────────────────────────────────────────────
// Mini player position
// ─────────────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun MiniPlayerPreview(position: MiniPlayerPosition) {
    val scheme = MaterialTheme.colorScheme
    val x by animateFloatAsState(
        if (position == MiniPlayerPosition.BOTTOM_END || position == MiniPlayerPosition.TOP_END) 1f else -1f,
        spring(dampingRatio = 0.7f, stiffness = 260f), label = "miniX",
    )
    val y by animateFloatAsState(
        if (position == MiniPlayerPosition.BOTTOM_END || position == MiniPlayerPosition.BOTTOM_START) 1f else -1f,
        spring(dampingRatio = 0.7f, stiffness = 260f), label = "miniY",
    )
    PreviewPanel(caption = "The mini player floats here while you browse. You can also drag it to any corner.") {
        Phone(Modifier.width(132.dp)) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Line(0.5f, scheme.onBackground, 7.dp)
                repeat(6) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(scheme.surfaceContainerHigh))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Line(0.9f, scheme.onSurface.copy(alpha = 0.7f), 4.dp, Modifier.width(48.dp))
                            Line(0.55f, scheme.outlineVariant, 3.dp, Modifier.width(48.dp))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(BiasAlignment(x, y))
                    .padding(8.dp)
                    .width(84.dp)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                SampleArt(Modifier.size(20.dp), CircleShape)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Line(0.9f, scheme.onPrimaryContainer, 3.dp)
                    Line(0.55f, scheme.onPrimaryContainer.copy(alpha = 0.5f), 2.dp)
                }
                Box(Modifier.size(16.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, null, tint = scheme.onPrimary, modifier = Modifier.size(11.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Audio visualizer (animated)
// ─────────────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun VisualizerPreview(style: VisualizerStyle) {
    val scheme = MaterialTheme.colorScheme
    val t by rememberInfiniteTransition(label = "viz").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "vizT",
    )
    PreviewPanel(
        caption = when (style) {
            VisualizerStyle.OFF -> "Off: nothing is drawn and no extra permission is used."
            VisualizerStyle.BARS -> "Bars move with the music on the player. With Circle cover art they wrap around the cover."
            VisualizerStyle.WAVEFORM -> "A flowing waveform moves with the music. With Circle cover art it flows around the cover."
            VisualizerStyle.CIRCULAR -> "A ring of bars pulses around a round cover (needs Circle cover art)."
        },
    ) {
        Crossfade(targetState = style, animationSpec = tween(220), label = "vizPreview") { s ->
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                when (s) {
                    VisualizerStyle.OFF -> Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                        val n = 24
                        val slot = size.width / n
                        for (i in 0 until n) {
                            val h = 4.dp.toPx()
                            drawRoundRect(
                                color = scheme.outlineVariant,
                                topLeft = Offset(i * slot + slot * 0.25f, (size.height - h) / 2f),
                                size = androidx.compose.ui.geometry.Size(slot * 0.5f, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                            )
                        }
                    }

                    VisualizerStyle.BARS -> Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                        val n = 28
                        val slot = size.width / n
                        for (i in 0 until n) {
                            val level = 0.18f + 0.82f * abs(sin(2f * PI.toFloat() * (t + i * 0.07f)))
                            val h = size.height * level
                            drawRoundRect(
                                color = scheme.primary.copy(alpha = 0.55f + 0.45f * level),
                                topLeft = Offset(i * slot + slot * 0.2f, size.height - h),
                                size = androidx.compose.ui.geometry.Size(slot * 0.6f, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(slot * 0.3f),
                            )
                        }
                    }

                    VisualizerStyle.WAVEFORM -> Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                        fun wave(phase: Float, amp: Float): Path {
                            val path = Path()
                            val steps = 80
                            for (i in 0..steps) {
                                val x = size.width * i / steps
                                val envelope = sin(PI.toFloat() * i / steps)
                                val y = size.height / 2f +
                                    sin(2f * PI.toFloat() * (i / steps.toFloat() * 2.4f + phase)) * amp * size.height * envelope
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            return path
                        }
                        drawPath(wave(-t * 0.6f, 0.28f), scheme.tertiary.copy(alpha = 0.55f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(wave(t, 0.38f), scheme.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    }

                    VisualizerStyle.CIRCULAR -> Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val n = 48
                            val inner = size.minDimension * 0.34f
                            val maxLen = size.minDimension * 0.15f
                            val c = Offset(size.width / 2f, size.height / 2f)
                            for (i in 0 until n) {
                                val a = 2f * PI.toFloat() * i / n
                                val len = maxLen * (0.2f + 0.8f * abs(sin(2f * PI.toFloat() * (t + i * 0.05f))))
                                drawLine(
                                    color = scheme.primary,
                                    start = Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                                    end = Offset(c.x + cos(a) * (inner + len), c.y + sin(a) * (inner + len)),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        }
                        SampleArt(Modifier.fillMaxSize(0.62f), CircleShape)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Sort order
// ─────────────────────────────────────────────────────────────────────────────────────────

private data class SampleSong(val title: String, val artist: String, val seconds: Int, val daysAgo: Int)

private val sampleSongs = listOf(
    SampleSong("Aurora", "Nova", 192, 12),
    SampleSong("Blue Hour", "Lumen", 245, 3),
    SampleSong("Cascade", "Echo", 168, 40),
    SampleSong("Daybreak", "Atlas", 301, 1),
)

@Composable
internal fun SortPreview(sortType: SortType) {
    val scheme = MaterialTheme.colorScheme
    val ordered = when (sortType) {
        SortType.DATE_MODIFIED_ASC -> sampleSongs.sortedByDescending { it.daysAgo }
        SortType.DATE_MODIFIED_DESC -> sampleSongs.sortedBy { it.daysAgo }
        SortType.NAME_ASC -> sampleSongs.sortedBy { it.title }
        SortType.NAME_DESC -> sampleSongs.sortedByDescending { it.title }
        SortType.ARTIST_ASC -> sampleSongs.sortedBy { it.artist }
        SortType.ARTIST_DESC -> sampleSongs.sortedByDescending { it.artist }
        SortType.DURATION_ASC -> sampleSongs.sortedBy { it.seconds }
        SortType.DURATION_DESC -> sampleSongs.sortedByDescending { it.seconds }
    }
    fun key(s: SampleSong) = when (sortType) {
        SortType.DATE_MODIFIED_ASC, SortType.DATE_MODIFIED_DESC -> if (s.daysAgo == 1) "1 day ago" else "${s.daysAgo} days ago"
        SortType.NAME_ASC, SortType.NAME_DESC -> s.title.first().toString()
        SortType.ARTIST_ASC, SortType.ARTIST_DESC -> s.artist
        SortType.DURATION_ASC, SortType.DURATION_DESC -> "${s.seconds / 60}:${(s.seconds % 60).toString().padStart(2, '0')}"
    }
    PreviewPanel(caption = "Example songs, in the order your Songs list will use.") {
        Crossfade(targetState = sortType, animationSpec = tween(180), label = "sortPreview") { _ ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ordered.forEachIndexed { index, song ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surface)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.width(14.dp))
                        SampleArt(Modifier.size(30.dp), RoundedCornerShape(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Text(
                            key(song),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.primary,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
// Volume normalization and crossfade
// ─────────────────────────────────────────────────────────────────────────────────────────

/** Three tracks at different loudness; with normalization on they level out. */
@Composable
internal fun NormalizationPreview(enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val flat by animateFloatAsState(if (enabled) 1f else 0f, tween(450), label = "normalize")
    val raw = listOf(0.95f, 0.45f, 0.72f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerLow)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        raw.forEachIndexed { i, level ->
            val shown = level + (0.7f - level) * flat
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(shown)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (enabled) scheme.primary else scheme.outline)
                    )
                }
                Text("Track ${i + 1}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text(
            if (enabled) "Even volume" else "Different volumes",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.weight(1.1f).padding(bottom = 18.dp),
        )
    }
}

/** Track A fading out while track B fades in; the overlap is as long as the chosen duration. */
@Composable
internal fun CrossfadePreview(durationMs: Long) {
    val scheme = MaterialTheme.colorScheme
    val overlap by animateFloatAsState((durationMs / 1000f / 12f).coerceIn(0.08f, 1f), tween(250), label = "overlap")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerLow)
            .padding(14.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val w = size.width
            val h = size.height
            val half = w / 2f
            val span = (w * 0.5f) * overlap          // width of the crossfade zone
            val start = half - span / 2f
            val end = half + span / 2f
            // Track A: full volume, then fades out across the zone.
            val a = Path().apply {
                moveTo(0f, h * 0.1f); lineTo(start, h * 0.1f); lineTo(end, h); lineTo(0f, h); close()
            }
            // Track B: fades in across the zone, then full volume.
            val b = Path().apply {
                moveTo(w, h * 0.1f); lineTo(end, h * 0.1f); lineTo(start, h); lineTo(w, h); close()
            }
            drawPath(a, scheme.primary.copy(alpha = 0.5f))
            drawPath(b, scheme.tertiary.copy(alpha = 0.5f))
            drawLine(scheme.onSurfaceVariant.copy(alpha = 0.5f), Offset(start, 0f), Offset(start, h), 1.dp.toPx())
            drawLine(scheme.onSurfaceVariant.copy(alpha = 0.5f), Offset(end, 0f), Offset(end, h), 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Song A", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
            Text("${durationMs / 1000}s overlap", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text("Song B", style = MaterialTheme.typography.labelSmall, color = scheme.tertiary)
        }
    }
}
