package com.tasnimulhasan.melodiq.ipod

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/** The three body colours of the iPod (Player B). The display itself always stays light. */
internal enum class IpodColor(val label: String) {
    WHITE("White"),
    BLACK("Black"),
    AMOLED("AMOLED"),
}

/** Colours of the iPod's LCD and its contents. The defaults are the classic light display. */
internal class IpodDisplayColors(
    val bezel: Color = Color(0xFF0B0B0C),
    val content: Color = Color.White,
    val barBrush: Brush = Brush.verticalGradient(listOf(Color(0xFFEDEFF1), Color(0xFFA7ADB3))),
    val barText: Color = Color.Black,
    val barIcon: Color = Color(0xFF6E7378),
    val empty: Color = Color(0xFF8A8F94),
    val selection: Brush = Brush.verticalGradient(listOf(Color(0xFF4FB2F2), Color(0xFF1C7CD0))),
    val text: Color = Color.Black,
    val textSelected: Color = Color.White,
    val sub: Color = Color(0xFF7D8287),
    val subSelected: Color = Color(0xFFE6F2FC),
    val artist: Color = Color(0xFF8E9297),
    val icon: Color = Color(0xFF6E7378),
    val placeholder: Color = Color(0xFFD9DCE0),
    val placeholderIcon: Color = Color.White,
    val flowBg: Brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F2F4))),
    val reflectTop: Color = Color.White.copy(alpha = 0.55f),
    val reflectBottom: Color = Color(0xFFF1F2F4),
    val nowReflectEnd: Color = Color.White,
    val progressTrack: Color = Color(0xFFEDEEF0),
    val progressFillTop: Color = Color(0xFF8CCBFA),
    val progressFillBottom: Color = Color(0xFF3A9CE8),
    val thumb: Color = Color(0xFF1C7CD0),
    val menuPanel: Color = Color.White,
    val menuEdge: Color = Color(0x33000000),
)

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
    val display: IpodDisplayColors = IpodDisplayColors(),
)

/** Glass UI: a frosted body (the app backdrop shows through), a translucent wheel and a violet LCD. */
private fun ipodGlassTheme(): IpodTheme {
    val screen = Color(0xFF2A2573)
    return IpodTheme(
        body = SolidColor(Color.Transparent),
        wheel = Brush.radialGradient(0.0f to Color.White.copy(alpha = 0.30f), 0.7f to Color.White.copy(alpha = 0.20f), 1.0f to Color.White.copy(alpha = 0.12f)),
        wheelBorder = Color.White.copy(alpha = 0.45f),
        label = Color.White.copy(alpha = 0.92f),
        centerIdle = Brush.radialGradient(0.0f to Color.White.copy(alpha = 0.34f), 0.75f to Color.White.copy(alpha = 0.22f), 1.0f to Color.White.copy(alpha = 0.16f)),
        centerPressed = Brush.radialGradient(0.0f to Color.White.copy(alpha = 0.14f), 0.75f to Color.White.copy(alpha = 0.10f), 1.0f to Color.White.copy(alpha = 0.08f)),
        centerBorder = Color.White.copy(alpha = 0.5f),
        darkBody = true,
        display = IpodDisplayColors(
            bezel = Color.White.copy(alpha = 0.28f),
            content = screen,
            barBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.12f))),
            barText = Color.White,
            barIcon = Color.White.copy(alpha = 0.80f),
            empty = Color.White.copy(alpha = 0.60f),
            selection = Brush.verticalGradient(listOf(Color(0xFFB9A6FF), Color(0xFF7A62E8))),
            text = Color.White,
            textSelected = Color(0xFF1E1650),
            sub = Color.White.copy(alpha = 0.62f),
            subSelected = Color(0xFF3B2F8A),
            artist = Color.White.copy(alpha = 0.66f),
            icon = Color.White.copy(alpha = 0.80f),
            placeholder = Color.White.copy(alpha = 0.16f),
            placeholderIcon = Color.White.copy(alpha = 0.7f),
            flowBg = Brush.verticalGradient(listOf(Color(0xFF3A33A0), Color(0xFF221C5E))),
            reflectTop = Color(0xFF221C5E).copy(alpha = 0.45f),
            reflectBottom = Color(0xFF221C5E),
            nowReflectEnd = screen,
            progressTrack = Color.White.copy(alpha = 0.20f),
            progressFillTop = Color(0xFFD9CCFF),
            progressFillBottom = Color(0xFF9F8BF2),
            thumb = Color(0xFFE6DEFF),
            menuPanel = screen,
            menuEdge = Color(0x55000000),
        ),
    )
}

internal fun ipodTheme(color: IpodColor, glass: Boolean = false): IpodTheme = if (glass) ipodGlassTheme() else when (color) {
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
