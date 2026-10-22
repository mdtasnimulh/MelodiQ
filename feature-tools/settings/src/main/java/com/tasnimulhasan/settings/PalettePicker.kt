package com.tasnimulhasan.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.designsystem.theme.PaletteSeeds
import com.tasnimulhasan.designsystem.theme.paletteSwatch
import com.tasnimulhasan.entity.enums.PaletteChoice
import com.tasnimulhasan.entity.enums.PaletteStyle

/**
 * Style tabs (Hue, Vibrant, Content, ...) over a grid of 12 seed colours. Every swatch previews the
 * scheme that seed would produce in the chosen style: a ring that is primary on top and tertiary below,
 * on a tile tinted with the scheme's surface. [locked] greys the grid out (system Material You is on).
 */
@Composable
internal fun CustomPaletteSection(
    locked: Boolean,
    selected: PaletteChoice?,
    savedStyle: PaletteStyle,
    onPick: (PaletteStyle, Color) -> Unit,
    onLockedClick: () -> Unit,
) {
    var tabName by rememberSaveable { mutableStateOf(savedStyle.name) }
    val style = PaletteStyle.entries.firstOrNull { it.name == tabName } ?: PaletteStyle.HUE
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .horizontalScroll(rememberScrollState())
                .padding(6.dp),
        ) {
            PaletteStyle.entries.forEach { s ->
                val on = s == style
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { tabName = s.name }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = s.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (on) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(Modifier.padding(top = 12.dp)) {
            PaletteSeeds.chunked(3).forEach { rowSeeds ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                ) {
                    rowSeeds.forEach { seed ->
                        val isSelected = selected != null && selected.style == style && selected.seed == seed.toArgb()
                        PaletteSwatch(
                            seed = seed,
                            style = style,
                            dark = dark,
                            selected = isSelected,
                            locked = locked,
                            onClick = { if (locked) onLockedClick() else onPick(style, seed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    seed: Color,
    style: PaletteStyle,
    dark: Boolean,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val (top, bottom, tile) = remember(seed, style, dark) { paletteSwatch(seed, style, dark) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .size(76.dp)
            .alpha(if (locked) 0.6f else 1f)
            .clip(shape)
            .background(tile)
            .then(if (selected) Modifier.border(2.5.dp, top, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(58.dp)) {
            drawArc(color = top, startAngle = 180f, sweepAngle = 180f, useCenter = true)
            drawArc(color = bottom, startAngle = 0f, sweepAngle = 180f, useCenter = true)
        }
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            if (locked || selected) {
                Icon(
                    imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.Check,
                    contentDescription = if (locked) "Locked" else "Selected",
                    tint = top,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
