package com.tasnimulhasan.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Picks an accent [Color] from a launcher-icon drawable for the "App icon" theme option.
 * Renders the drawable small, runs Palette, then prefers the swatch that is both common and
 * colourful while skipping near-white / near-black ones (a cream or black logo background
 * makes a useless accent).
 */
object AppIconAccentExtractor {
    private const val SIZE_PX = 96

    suspend fun extract(context: Context, @DrawableRes resId: Int): Color? = withContext(Dispatchers.Default) {
        runCatching {
            val drawable = ContextCompat.getDrawable(context, resId) ?: return@runCatching null
            val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, SIZE_PX, SIZE_PX)
            drawable.draw(Canvas(bitmap))

            val palette = Palette.from(bitmap).maximumColorCount(16).generate()
            val hsl = FloatArray(3)
            val best = palette.swatches.mapNotNull { swatch ->
                ColorUtils.colorToHSL(swatch.rgb, hsl)
                val lightness = hsl[2]
                if (lightness < 0.12f || lightness > 0.85f) null
                else swatch to swatch.population * (0.25f + hsl[1])
            }.maxByOrNull { it.second }?.first ?: palette.dominantSwatch
            best?.let { Color(it.rgb) }
        }.getOrNull()
    }
}
