package com.tasnimulhasan.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tasnimulhasan.designsystem.R
import com.tasnimulhasan.entity.enums.AppFont

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

val LatoFontFamily = FontFamily(
    Font(R.font.lato_light, FontWeight.Light),
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_black, FontWeight.Black),
    Font(R.font.lato_italic, FontWeight.Normal),
)

val RobotoFontFamily = FontFamily(
    Font(R.font.roboto_light, FontWeight.Light),
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.SemiBold),
    Font(R.font.roboto_black, FontWeight.Bold),
    Font(R.font.roboto_italic, FontWeight.Normal),
)

val CaveatFontFamily = FontFamily(
    Font(R.font.caveat_regular, FontWeight.Normal),
    Font(R.font.caveat_medium, FontWeight.Medium),
    Font(R.font.caveat_semi_bold, FontWeight.SemiBold),
    Font(R.font.caveat_bold, FontWeight.Bold),
)

val PlaypenSansFontFamily = FontFamily(
    Font(R.font.playpen_sans_variable_font_weight)
)

// ---- user-selectable app font ----------------------------------------------------------------

/**
 * The family the user picked in Settings, or null for [AppFont.DEFAULT] (the fonts the app shipped
 * with). Screens that set a fontFamily on their own TextStyle read this so they follow the choice
 * too: `fontFamily = LocalAppFontFamily.current ?: RobotoFontFamily`.
 */
val LocalAppFontFamily = compositionLocalOf<FontFamily?> { null }

/** One variable font file used at the four weights the app needs. */
private fun variableFamily(res: Int) = FontFamily(
    Font(res, FontWeight.Normal),
    Font(res, FontWeight.Medium),
    Font(res, FontWeight.SemiBold),
    Font(res, FontWeight.Bold),
)

val BodoniModaFontFamily = variableFamily(R.font.bodoni_moda_variable)
val BricolageGrotesqueFontFamily = variableFamily(R.font.bricolage_grotesque_variable)
val JetBrainsMonoFontFamily = variableFamily(R.font.jetbrains_mono_variable)
val OswaldFontFamily = variableFamily(R.font.oswald_variable)
val PlayfairDisplayFontFamily = variableFamily(R.font.playfair_display_variable)
val PlaywriteItTradFontFamily = variableFamily(R.font.playwrite_it_trad_variable)
val RobotoMonoFontFamily = variableFamily(R.font.roboto_mono_variable)

val LobsterTwoFontFamily = FontFamily(
    Font(R.font.lobster_two_regular, FontWeight.Normal),
    Font(R.font.lobster_two_bold, FontWeight.Bold),
)

val TuffyFontFamily = FontFamily(
    Font(R.font.tuffy_regular, FontWeight.Normal),
    Font(R.font.tuffy_bold, FontWeight.Bold),
)

/** The font family for [font]; null means "leave everything as the app ships it". */
fun appFontFamily(font: AppFont): FontFamily? = when (font) {
    AppFont.DEFAULT -> null
    AppFont.BODONI_MODA -> BodoniModaFontFamily
    AppFont.BRICOLAGE_GROTESQUE -> BricolageGrotesqueFontFamily
    AppFont.CAVEAT -> CaveatFontFamily
    AppFont.JETBRAINS_MONO -> JetBrainsMonoFontFamily
    AppFont.LOBSTER_TWO -> LobsterTwoFontFamily
    AppFont.OSWALD -> OswaldFontFamily
    AppFont.PLAYFAIR_DISPLAY -> PlayfairDisplayFontFamily
    AppFont.PLAYWRITE_IT_TRAD -> PlaywriteItTradFontFamily
    AppFont.ROBOTO_MONO -> RobotoMonoFontFamily
    AppFont.TUFFY -> TuffyFontFamily
}

/** Material typography with every style (titles, labels, body, headlines, dialogs...) in [family]. */
fun typographyFor(family: FontFamily?): Typography {
    if (family == null) return Typography
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
