package com.tasnimulhasan.entity.enums

/** How a custom seed colour is expanded into a full colour scheme (the tabs in Settings > Custom palette). */
enum class PaletteStyle(val label: String) {
    HUE("Hue"),
    NEUTRAL("Neutral"),
    VIBRANT("Vibrant"),
    EXPRESSIVE("Expressive"),
    CONTENT("Content"),
    RAINBOW("Rainbow"),
    FRUIT_SALAD("Fruit Salad"),
    MONO("Mono"),
}

/** The saved custom palette: a [style] applied to a [seed] colour (ARGB int). */
data class PaletteChoice(val style: PaletteStyle = PaletteStyle.HUE, val seed: Int = 0xFF6650A4.toInt())
