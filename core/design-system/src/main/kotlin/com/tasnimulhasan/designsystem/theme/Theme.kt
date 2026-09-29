package com.tasnimulhasan.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun MelodiqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You, from the wallpaper) is only used when there's no
    // explicit [seedColor] - an accent pick or an album-art color always wins, since the
    // user (or the currently playing song) chose it deliberately.
    dynamicColor: Boolean = true,
    // True-black variant of the dark theme - forces background/surface to pure black
    // instead of Material's dark gray. Ignored when [darkTheme] is false.
    amoledBlack: Boolean = false,
    // A specific accent seed - either a fixed preset or one extracted live from album art.
    // Null means "use the app's default purple, or Material You if [dynamicColor] is true".
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    var colorScheme = when {
        seedColor != null -> seedColorScheme(seedColor, darkTheme)

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (darkTheme && amoledBlack) {
        colorScheme = colorScheme.toAmoledBlack()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
