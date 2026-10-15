package com.tasnimulhasan.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Drop-in replacement for material3's `Icon` that draws every icon as frosted glass: a
 * translucent two-tone gradient body, a bright highlight on the upper-left, and a soft coloured
 * glow underneath. Same parameters as the original, so switching a file over is just changing
 * the `Icon` import to `com.tasnimulhasan.designsystem.component.Icon`.
 *
 * Colourless tints (greys, black, white) stay neutral glass; coloured tints get a second hue
 * next to them for the gradient. `Color.Unspecified` draws the icon untouched.
 */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = Icon(rememberVectorPainter(imageVector), contentDescription, modifier, tint)

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val glass: Painter = if (tint == Color.Unspecified) painter else remember(painter, tint) { GlassPainter(painter, tint) }
    val unsized = painter.intrinsicSize == Size.Unspecified || painter.intrinsicSize.width.isInfinite() || painter.intrinsicSize.height.isInfinite()
    Box(
        modifier
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        this.role = Role.Image
                    }
                } else Modifier
            )
            .then(if (unsized) Modifier.size(24.dp) else Modifier)
            .paint(glass, contentScale = ContentScale.Fit)
    )
}

private class GlassPainter(private val inner: Painter, private val tint: Color) : Painter() {

    override val intrinsicSize: Size get() = inner.intrinsicSize

    private val tones = GlassTones.of(tint)

    override fun DrawScope.onDraw() {
        val s = size
        if (s.width <= 0f || s.height <= 0f) return

        // 1. Soft coloured glow under the icon.
        translate(top = s.height * 0.07f) { paintInner(tones.base.copy(alpha = 0.30f)) }
        translate(top = s.height * 0.03f) { paintInner(tones.base.copy(alpha = 0.22f)) }

        // 2. Frosted body: translucent two-tone gradient.
        layer(s) {
            paintInner(Color.White)
            drawRect(
                brush = Brush.linearGradient(
                    0.0f to tones.light,
                    0.55f to tones.base,
                    1.0f to tones.second,
                    start = Offset(0f, 0f),
                    end = Offset(s.width, s.height),
                ),
                blendMode = BlendMode.SrcIn,
            )
        }

        // 3. Glass highlight: bright on the upper-left, fading out.
        layer(s) {
            paintInner(Color.White)
            drawRect(
                brush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.60f),
                    0.45f to Color.White.copy(alpha = 0.10f),
                    1.0f to Color.Transparent,
                    start = Offset(0f, 0f),
                    end = Offset(s.width * 0.8f, s.height * 0.8f),
                ),
                blendMode = BlendMode.SrcIn,
            )
        }
    }

    private fun DrawScope.paintInner(color: Color) {
        with(inner) { draw(size, 1f, ColorFilter.tint(color)) }
    }

    private inline fun DrawScope.layer(s: Size, block: DrawScope.() -> Unit) {
        drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, s), Paint()) }
        block()
        drawIntoCanvas { it.restore() }
    }
}

private class GlassTones(val light: Color, val base: Color, val second: Color) {
    companion object {
        fun of(tint: Color): GlassTones {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(tint.copy(alpha = 1f).toArgb(), hsv)
            val colourful = hsv[1] > 0.15f && hsv[2] > 0.15f
            val a = tint.alpha.coerceIn(0f, 1f)
            val base = tint.copy(alpha = (0.88f * a))
            val light = lerpColor(tint, Color.White, 0.50f).copy(alpha = 0.92f * a)
            val second = if (colourful) {
                val h = floatArrayOf((hsv[0] + 38f) % 360f, (hsv[1] * 0.95f).coerceAtMost(1f), (hsv[2] * 0.95f).coerceAtMost(1f))
                Color(android.graphics.Color.HSVToColor(h)).copy(alpha = 0.90f * a)
            } else {
                lerpColor(tint, Color.Black, 0.22f).copy(alpha = 0.86f * a)
            }
            return GlassTones(light, base, second)
        }

        private fun lerpColor(a: Color, b: Color, t: Float) = Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = 1f,
        )
    }
}
