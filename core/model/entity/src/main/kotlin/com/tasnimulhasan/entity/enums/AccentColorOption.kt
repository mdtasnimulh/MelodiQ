package com.tasnimulhasan.entity.enums

/**
 * How the app's accent (Material "primary") color is chosen.
 *
 * DYNAMIC and ALBUM_ART are computed at runtime (from the system wallpaper, or from the
 * currently playing song's artwork) rather than being a fixed value - everything else is a
 * fixed preset seed color applied the same way everywhere in the app.
 */
enum class AccentColorOption {
    /** Android 12+ Material You wallpaper-based color. Falls back to PURPLE on older OS. */
    DYNAMIC,

    /** Derived live from the currently playing song's album art (Palette API). */
    ALBUM_ART,

    /** Derived from the colours of the launcher icon the user picked (Settings > App icon). */
    APP_ICON,

    PURPLE,
    BLUE,
    GREEN,
    ORANGE,
    PINK,
    RED,
    TEAL,
}
