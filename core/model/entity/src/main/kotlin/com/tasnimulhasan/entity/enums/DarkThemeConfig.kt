package com.tasnimulhasan.entity.enums

enum class DarkThemeConfig {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK,
    // True-black variant: same dark palette, but background/surface forced to pure black
    // instead of Material's dark gray, for OLED power saving and a deeper black.
    AMOLED,
}
