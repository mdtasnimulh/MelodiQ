package com.tasnimulhasan.designsystem.theme

import android.annotation.SuppressLint
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import com.tasnimulhasan.entity.enums.PaletteStyle

/**
 * Builds a Material 3 [ColorScheme] from a [seed] using Google's color science (HCT tonal palettes),
 * in the chosen [style]. The tone mapping is the standard Material 3 baseline.
 */
@SuppressLint("RestrictedApi")
fun paletteColorScheme(seed: Color, style: PaletteStyle, dark: Boolean): ColorScheme {
    val hct = Hct.fromInt(seed.toArgbInt())
    val s: DynamicScheme = when (style) {
        PaletteStyle.HUE -> SchemeTonalSpot(hct, dark, 0.0)
        PaletteStyle.NEUTRAL -> SchemeNeutral(hct, dark, 0.0)
        PaletteStyle.VIBRANT -> SchemeVibrant(hct, dark, 0.0)
        PaletteStyle.EXPRESSIVE -> SchemeExpressive(hct, dark, 0.0)
        PaletteStyle.CONTENT -> SchemeContent(hct, dark, 0.0)
        PaletteStyle.RAINBOW -> SchemeRainbow(hct, dark, 0.0)
        PaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(hct, dark, 0.0)
        PaletteStyle.MONO -> SchemeMonochrome(hct, dark, 0.0)
    }
    fun p(tone: Int) = Color(s.primaryPalette.tone(tone))
    fun sec(tone: Int) = Color(s.secondaryPalette.tone(tone))
    fun t(tone: Int) = Color(s.tertiaryPalette.tone(tone))
    fun n(tone: Int) = Color(s.neutralPalette.tone(tone))
    fun nv(tone: Int) = Color(s.neutralVariantPalette.tone(tone))

    return if (dark) darkColorScheme(
        primary = p(80), onPrimary = p(20), primaryContainer = p(30), onPrimaryContainer = p(90),
        inversePrimary = p(40),
        secondary = sec(80), onSecondary = sec(20), secondaryContainer = sec(30), onSecondaryContainer = sec(90),
        tertiary = t(80), onTertiary = t(20), tertiaryContainer = t(30), onTertiaryContainer = t(90),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = n(6), onBackground = n(90),
        surface = n(6), onSurface = n(90),
        surfaceVariant = nv(30), onSurfaceVariant = nv(80),
        surfaceTint = p(80),
        inverseSurface = n(90), inverseOnSurface = n(20),
        outline = nv(60), outlineVariant = nv(30),
        surfaceDim = n(6), surfaceBright = n(24),
        surfaceContainerLowest = n(4), surfaceContainerLow = n(10), surfaceContainer = n(12),
        surfaceContainerHigh = n(17), surfaceContainerHighest = n(22),
    ) else lightColorScheme(
        primary = p(40), onPrimary = p(100), primaryContainer = p(90), onPrimaryContainer = p(10),
        inversePrimary = p(80),
        secondary = sec(40), onSecondary = sec(100), secondaryContainer = sec(90), onSecondaryContainer = sec(10),
        tertiary = t(40), onTertiary = t(100), tertiaryContainer = t(90), onTertiaryContainer = t(10),
        error = Color(0xFFBA1A1A), onError = Color.White,
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = n(98), onBackground = n(10),
        surface = n(98), onSurface = n(10),
        surfaceVariant = nv(90), onSurfaceVariant = nv(30),
        surfaceTint = p(40),
        inverseSurface = n(20), inverseOnSurface = n(95),
        outline = nv(50), outlineVariant = nv(80),
        surfaceDim = n(87), surfaceBright = n(98),
        surfaceContainerLowest = n(100), surfaceContainerLow = n(96), surfaceContainer = n(94),
        surfaceContainerHigh = n(92), surfaceContainerHighest = n(90),
    )
}

private fun Color.toArgbInt(): Int = androidx.compose.ui.graphics.toArgb(this)

/** The 12 seed colors offered in every style tab (hue wheel, starting at warm). */
val PaletteSeeds: List<Color> = listOf(
    Color(0xFFE65100), Color(0xFFD81B60), Color(0xFFB8860B), Color(0xFF6750A4),
    Color(0xFF2E7D32), Color(0xFFC62828), Color(0xFF00838F), Color(0xFFF9A825),
    Color(0xFF7C4DFF), Color(0xFF5D7F4F), Color(0xFF00897B), Color(0xFFEF6C00),
)

/** The two accent colors previewed on a palette swatch: top half primary, bottom half tertiary. */
@SuppressLint("RestrictedApi")
fun paletteSwatch(seed: Color, style: PaletteStyle, dark: Boolean): Triple<Color, Color, Color> {
    val c = paletteColorScheme(seed, style, dark)
    return Triple(c.primary, c.tertiary, c.surfaceContainerHigh)
}
