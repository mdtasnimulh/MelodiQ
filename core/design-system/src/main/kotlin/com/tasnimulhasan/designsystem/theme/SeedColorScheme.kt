package com.tasnimulhasan.designsystem.theme

import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tasnimulhasan.entity.enums.AccentColorOption

/** Fixed seed color for each non-dynamic accent preset. DYNAMIC and ALBUM_ART are resolved
 * elsewhere (from the wallpaper, or from Palette) rather than having a fixed seed here. */
fun AccentColorOption.fixedSeedColorOrNull(): Color? = when (this) {
    AccentColorOption.PURPLE -> Color(0xFF6650A4)
    AccentColorOption.BLUE -> Color(0xFF0077B6)
    AccentColorOption.GREEN -> Color(0xFF2E7D32)
    AccentColorOption.ORANGE -> Color(0xFFEF6C00)
    AccentColorOption.PINK -> Color(0xFFD81B60)
    AccentColorOption.RED -> Color(0xFFC62828)
    AccentColorOption.TEAL -> Color(0xFF00796B)
    AccentColorOption.DYNAMIC, AccentColorOption.ALBUM_ART, AccentColorOption.APP_ICON -> null
}

/**
 * Builds a full [ColorScheme] from a single seed color using simple HSL lightness/saturation
 * shifts, rather than pulling in a full Material color-science library just for this. It
 * won't match Google's tonal palette algorithm exactly, but produces a consistent, readable
 * light/dark scheme for any seed - including one extracted live from album art, which can be
 * any arbitrary color a real Material palette generator was never guaranteed to receive
 * either.
 */
fun seedColorScheme(seed: Color, darkTheme: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsl) // reuse HSV as a stand-in for HSL shifts below
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.35f, 0.85f) // keep muted or overly vivid seeds usable

    fun tone(lightness: Float, saturation: Float = sat): Color =
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))))

    return if (darkTheme) {
        darkColorScheme(
            primary = tone(0.80f),
            onPrimary = tone(0.12f),
            primaryContainer = tone(0.30f, sat * 0.7f),
            onPrimaryContainer = tone(0.90f),
            secondary = tone(0.70f, sat * 0.5f),
            onSecondary = tone(0.12f),
            secondaryContainer = tone(0.26f, sat * 0.4f),
            onSecondaryContainer = tone(0.88f),
            tertiary = tone(0.75f, sat * 0.6f),
            onTertiary = tone(0.12f),
        )
    } else {
        lightColorScheme(
            primary = tone(0.42f),
            onPrimary = tone(0.98f),
            primaryContainer = tone(0.88f, sat * 0.5f),
            onPrimaryContainer = tone(0.18f),
            secondary = tone(0.38f, sat * 0.5f),
            onSecondary = tone(0.98f),
            secondaryContainer = tone(0.90f, sat * 0.3f),
            onSecondaryContainer = tone(0.20f),
            tertiary = tone(0.40f, sat * 0.6f),
            onTertiary = tone(0.98f),
        )
    }
}

/** Forces every background/surface role to true black, keeping whatever accent colors the
 * base [scheme] already has - this is the AMOLED "true black" variant. */
fun ColorScheme.toAmoledBlack(): ColorScheme = copy(
    background = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    surface = Color.Black,
    onSurface = Color(0xFFE6E1E5),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF141414),
)
