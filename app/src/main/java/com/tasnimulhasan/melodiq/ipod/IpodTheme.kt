package com.tasnimulhasan.melodiq.ipod

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** The three body colours of the iPod (Player B). The display itself always stays light. */
internal enum class IpodColor(val label: String) {
    WHITE("White"),
    BLACK("Black"),
    AMOLED("AMOLED"),
}

/** Everything about the device body and click wheel that changes with [IpodColor]. */
internal class IpodTheme(
    val body: Brush,
    val wheel: Brush,
    val wheelBorder: Color,
    val label: Color,
    val centerIdle: Brush,
    val centerPressed: Brush,
    val centerBorder: Color,
    /** true: dark body, so status / navigation bar icons must be light. */
    val darkBody: Boolean,
)

internal fun ipodTheme(color: IpodColor): IpodTheme = when (color) {
    IpodColor.WHITE -> IpodTheme(
        body = Brush.horizontalGradient(
            0.00f to Color(0xFFB7B7B9),
            0.10f to Color(0xFFCFCFD1),
            0.90f to Color(0xFFCFCFD1),
            1.00f to Color(0xFFB7B7B9),
        ),
        wheel = Brush.radialGradient(0.0f to Color.White, 0.7f to Color(0xFFF6F6F6), 1.0f to Color(0xFFE3E3E3)),
        wheelBorder = Color(0xFFD2D2D4),
        label = Color(0xFF9B9BA0),
        centerIdle = Brush.radialGradient(0.0f to Color(0xFFEFEFEF), 0.75f to Color(0xFFE4E4E4), 1.0f to Color(0xFFD4D4D4)),
        centerPressed = Brush.radialGradient(0.0f to Color(0xFFD8D8D8), 0.75f to Color(0xFFCDCDCD), 1.0f to Color(0xFFD4D4D4)),
        centerBorder = Color(0xFFCFCFD1),
        darkBody = false,
    )

    IpodColor.BLACK -> IpodTheme(
        body = Brush.horizontalGradient(
            0.00f to Color(0xFF151517),
            0.10f to Color(0xFF2B2B2E),
            0.90f to Color(0xFF2B2B2E),
            1.00f to Color(0xFF151517),
        ),
        wheel = Brush.radialGradient(0.0f to Color(0xFF3C3C40), 0.7f to Color(0xFF2A2A2D), 1.0f to Color(0xFF1C1C1E)),
        wheelBorder = Color(0xFF0E0E0F),
        label = Color(0xFFE4E4E7),
        centerIdle = Brush.radialGradient(0.0f to Color(0xFF3A3A3E), 0.75f to Color(0xFF2C2C2F), 1.0f to Color(0xFF232325)),
        centerPressed = Brush.radialGradient(0.0f to Color(0xFF222225), 0.75f to Color(0xFF1B1B1D), 1.0f to Color(0xFF161618)),
        centerBorder = Color(0xFF0E0E0F),
        darkBody = true,
    )

    IpodColor.AMOLED -> IpodTheme(
        body = Brush.horizontalGradient(0.0f to Color.Black, 1.0f to Color.Black),
        wheel = Brush.radialGradient(0.0f to Color(0xFF1A1A1C), 0.7f to Color(0xFF111112), 1.0f to Color(0xFF0A0A0B)),
        wheelBorder = Color(0xFF2A2A2D),
        label = Color(0xFFB4B4BA),
        centerIdle = Brush.radialGradient(0.0f to Color(0xFF1C1C1E), 0.75f to Color(0xFF131314), 1.0f to Color(0xFF0C0C0D)),
        centerPressed = Brush.radialGradient(0.0f to Color(0xFF0E0E0F), 0.75f to Color(0xFF080808), 1.0f to Color(0xFF050505)),
        centerBorder = Color(0xFF2A2A2D),
        darkBody = true,
    )
}

internal val LocalIpodTheme = staticCompositionLocalOf { ipodTheme(IpodColor.WHITE) }
