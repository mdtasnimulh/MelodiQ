package com.tasnimulhasan.designsystem.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Everything the Glass UI needs beyond the colour scheme. null in [LocalGlass] means the regular
 * (solid) interface is active, so any screen can ask `LocalGlass.current` and branch on it.
 */
@Immutable
class GlassTokens(
    /** The dark (or AMOLED) variant: deep gradient. false is the light, periwinkle variant. */
    val dark: Boolean,
    val amoled: Boolean,
    /** The accent, lightened so it reads on glass. */
    val accent: Color,
    /** Top, middle and bottom of the backdrop. */
    val gradient: List<Color>,
    val glowA: Color,
    val glowB: Color,
    /** A solid, deep tone for surfaces that must not be see-through (sheets, menus). */
    val panel: Color,
)

val LocalGlass = staticCompositionLocalOf<GlassTokens?> { null }

/** True while the Glass UI is selected. */
@Composable
fun isGlassUi(): Boolean = LocalGlass.current != null

internal fun Color.forGlass(): Color = if (luminance() < 0.45f) lerp(this, Color.White, 0.6f) else this

internal fun glassTokens(primary: Color, dark: Boolean, amoled: Boolean): GlassTokens {
    val accent = primary.forGlass()
    val deep = lerp(primary, Color.Black, 0.35f)
    return when {
        dark && amoled -> GlassTokens(
            dark = true, amoled = true, accent = accent,
            gradient = listOf(Color(0xFF040407), Color(0xFF0A0916), Color(0xFF000000)),
            glowA = accent.copy(alpha = 0.16f),
            glowB = Color(0xFF3B82F6).copy(alpha = 0.10f),
            panel = Color(0xFF16151F),
        )
        dark -> GlassTokens(
            dark = true, amoled = false, accent = accent,
            gradient = listOf(
                lerp(Color(0xFF241B63), deep, 0.22f),
                lerp(Color(0xFF30248A), deep, 0.28f),
                lerp(Color(0xFF181241), deep, 0.18f),
            ),
            glowA = accent.copy(alpha = 0.34f),
            glowB = Color(0xFF3B82F6).copy(alpha = 0.22f),
            panel = Color(0xFF2B2570),
        )
        else -> GlassTokens(
            dark = false, amoled = false, accent = accent,
            gradient = listOf(
                lerp(Color(0xFF6C74EA), primary, 0.18f),
                lerp(Color(0xFF8B79EC), primary, 0.20f),
                lerp(Color(0xFFB48CE8), primary, 0.15f),
            ),
            glowA = Color.White.copy(alpha = 0.28f),
            glowB = Color(0xFFFF9AD5).copy(alpha = 0.26f),
            panel = Color(0xFF5A55C8),
        )
    }
}

/** The colour scheme of the Glass UI: translucent white surfaces, light text, a bright accent. */
internal fun ColorScheme.toGlass(g: GlassTokens): ColorScheme {
    val w = Color.White
    val accent = g.accent
    val sec = secondary.forGlass()
    val ter = tertiary.forGlass()
    return copy(
        primary = accent,
        onPrimary = Color(0xFF1E1650),
        primaryContainer = accent.copy(alpha = 0.30f),
        onPrimaryContainer = w,
        inversePrimary = accent,
        secondary = sec,
        onSecondary = Color(0xFF1E1650),
        secondaryContainer = sec.copy(alpha = 0.26f),
        onSecondaryContainer = w,
        tertiary = ter,
        onTertiary = Color(0xFF1E1650),
        tertiaryContainer = ter.copy(alpha = 0.26f),
        onTertiaryContainer = w,
        background = Color.Transparent,
        onBackground = w.copy(alpha = 0.96f),
        surface = w.copy(alpha = 0.08f),
        onSurface = w.copy(alpha = 0.96f),
        surfaceVariant = w.copy(alpha = 0.12f),
        onSurfaceVariant = w.copy(alpha = 0.72f),
        surfaceTint = accent,
        inverseSurface = w.copy(alpha = 0.92f),
        inverseOnSurface = Color(0xFF1E1650),
        outline = w.copy(alpha = 0.42f),
        outlineVariant = w.copy(alpha = 0.18f),
        surfaceBright = w.copy(alpha = 0.20f),
        surfaceDim = w.copy(alpha = 0.05f),
        surfaceContainerLowest = w.copy(alpha = 0.04f),
        surfaceContainerLow = w.copy(alpha = 0.09f),
        surfaceContainer = w.copy(alpha = 0.12f),
        surfaceContainerHigh = w.copy(alpha = 0.15f),
        surfaceContainerHighest = w.copy(alpha = 0.19f),
    )
}

/** Rounder corners than the solid UI: glass reads as soft, large panels. */
internal val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

/**
 * Paints the Glass backdrop: a soft vertical gradient with two blurred "light" blobs, behind
 * everything. Screens are transparent in Glass mode, so this is what shows through the glass.
 */
fun Modifier.glassBackdrop(g: GlassTokens): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(g.gradient))
    val r = size.maxDimension * 0.62f
    drawRect(
        Brush.radialGradient(
            colors = listOf(g.glowA, Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.10f),
            radius = r,
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(g.glowB, Color.Transparent),
            center = Offset(size.width * 0.95f, size.height * 0.62f),
            radius = r,
        )
    )
}

/** A bright-to-faint hairline: the edge of a glass panel catching the light. */
fun glassEdge(strong: Float = 0.55f, faint: Float = 0.10f): Brush =
    Brush.linearGradient(listOf(Color.White.copy(alpha = strong), Color.White.copy(alpha = faint), Color.White.copy(alpha = strong * 0.4f)))

/** The border for a Card in Glass mode, or null in the solid UI, so `Card(border = glassBorder())` is a no-op there. */
@Composable
fun glassBorder(width: Dp = 1.dp): BorderStroke? =
    if (LocalGlass.current != null) BorderStroke(width, glassEdge()) else null

/** A translucent glass fill plus its lit edge, for custom surfaces. */
fun Modifier.glassPanel(shape: Shape, fill: Float = 0.14f, edge: Brush = glassEdge()): Modifier =
    this
        .background(Color.White.copy(alpha = fill), shape)
        .border(1.dp, edge, shape)

/** Sheet / menu container: solid deep glass in Glass mode, otherwise [default] untouched. */
@Composable
fun glassSheetColor(default: Color): Color = LocalGlass.current?.panel ?: default
