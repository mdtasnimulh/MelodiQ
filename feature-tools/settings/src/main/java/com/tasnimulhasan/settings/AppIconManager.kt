package com.tasnimulhasan.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import com.tasnimulhasan.ui.image.AppIconAccentExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One selectable launcher icon. [aliasClass] must match an <activity-alias> in the manifest,
 * and [mipmapName] the adaptive icon (mipmap-anydpi) that alias uses. */
enum class AppIconOption(val aliasClass: String, val mipmapName: String, val drawableName: String, val group: Group, val label: String) {
    MAIN_1("com.tasnimulhasan.melodiq.alias.LogoMain", "ic_alias_main_1", "ic_logo_main", Group.MAIN, "Main"),
    MAIN_2("com.tasnimulhasan.melodiq.alias.LogoMain2", "ic_alias_main_2", "ic_logo_main_2", Group.MAIN, "Main 2"),
    MAIN_3("com.tasnimulhasan.melodiq.alias.LogoMain3", "ic_alias_main_3", "ic_logo_main_3", Group.MAIN, "Main 3"),
    MAIN_4("com.tasnimulhasan.melodiq.alias.LogoMain4", "ic_alias_main_4", "ic_logo_main_4", Group.MAIN, "Main 4"),
    ALT_1("com.tasnimulhasan.melodiq.alias.LogoAlt1", "ic_alias_alt_1", "ic_logo_alt", Group.CLASSIC, "Classic"),
    ALT_2("com.tasnimulhasan.melodiq.alias.LogoAlt2", "ic_alias_alt_2", "ic_logo_alt_2", Group.CLASSIC, "Classic 2"),
    ALT_3("com.tasnimulhasan.melodiq.alias.LogoAlt3", "ic_alias_alt_3", "ic_logo_alt_3", Group.CLASSIC, "Classic 3"),
    ALT_4("com.tasnimulhasan.melodiq.alias.LogoAlt4", "ic_alias_alt_4", "ic_logo_alt_4", Group.CLASSIC, "Classic 4");

    enum class Group(val title: String) { MAIN("Main logos"), CLASSIC("Classic logos") }

    /** Used only if Palette can't read the drawable. */
    val fallbackSeed: Color
        get() = when (this) {
            MAIN_1, ALT_1 -> Color(0xFF6497D6)
            MAIN_2, ALT_2 -> Color(0xFF5B6B4D)
            MAIN_3, ALT_3 -> Color(0xFFFF4D00)
            MAIN_4, ALT_4 -> Color(0xFF3A6B35)
        }

    companion object {
        val DEFAULT = MAIN_1
    }
}

/** The launcher-icon choice lives in the system's component-enabled state (the single source
 * of truth), so there is nothing to keep in sync in DataStore. */
object AppIconManager {

    private val _selected = MutableStateFlow<AppIconOption?>(null)

    /** The active launcher icon, shared so the drawer and the theme follow a change instantly.
     * Null until [refresh] has run once. */
    val selected: StateFlow<AppIconOption?> = _selected.asStateFlow()

    fun refresh(context: Context) {
        _selected.value = current(context)
    }

    private val accentCache = mutableMapOf<AppIconOption, Color>()

    /** Accent colour taken from [option]'s artwork (cached - the artwork never changes). */
    suspend fun accentColor(context: Context, option: AppIconOption): Color {
        accentCache[option]?.let { return it }
        val resId = previewResId(context, option)
        val color = (if (resId != 0) AppIconAccentExtractor.extract(context, resId) else null) ?: option.fallbackSeed
        accentCache[option] = color
        return color
    }

    fun current(context: Context): AppIconOption {
        val pm = context.packageManager
        return AppIconOption.entries.firstOrNull { option ->
            // An alias missing from the manifest throws IllegalArgumentException - treat it as
            // "not active" instead of crashing the Settings screen.
            val state = try {
                pm.getComponentEnabledSetting(ComponentName(context, option.aliasClass))
            } catch (_: IllegalArgumentException) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            when (state) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> option == AppIconOption.DEFAULT
                else -> false
            }
        } ?: AppIconOption.DEFAULT
    }

    /** Enables the chosen alias first, then disables the rest, so there is never a moment
     * with no launcher entry. DONT_KILL_APP keeps playback running. */
    fun apply(context: Context, target: AppIconOption) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            ComponentName(context, target.aliasClass),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        AppIconOption.entries.filter { it != target }.forEach { other ->
            pm.setComponentEnabledSetting(
                ComponentName(context, other.aliasClass),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        _selected.value = target
    }

    /** Resource id of the option's icon, looked up by name so this module doesn't need to
     * know which module the drawables live in. 0 if it isn't found. */
    fun previewResId(context: Context, option: AppIconOption): Int =
        context.resources.getIdentifier(option.drawableName, "drawable", context.packageName)
}
