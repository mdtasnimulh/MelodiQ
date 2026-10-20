package com.tasnimulhasan.designsystem.theme

import android.content.Context
import com.tasnimulhasan.entity.enums.UiStyle

/**
 * A tiny synchronous copy of the chosen [UiStyle], so the very first frame after a cold start is
 * already drawn in the right style instead of flashing the solid UI until DataStore has loaded.
 * DataStore stays the source of truth; this is only refreshed from it.
 */
object UiStyleCache {
    private const val FILE = "ui_style_cache"
    private const val KEY = "style"

    fun read(context: Context): UiStyle = runCatching {
        UiStyle.valueOf(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null) ?: UiStyle.DEFAULT.name)
    }.getOrDefault(UiStyle.DEFAULT)

    fun write(context: Context, style: UiStyle) {
        runCatching { context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, style.name).apply() }
    }
}
