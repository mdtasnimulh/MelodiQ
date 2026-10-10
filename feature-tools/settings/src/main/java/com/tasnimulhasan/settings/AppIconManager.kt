package com.tasnimulhasan.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** One selectable launcher icon. [aliasClass] must match an <activity-alias> in the manifest,
 * and [mipmapName] the adaptive icon (mipmap-anydpi) that alias uses. */
internal enum class AppIconOption(val aliasClass: String, val mipmapName: String, val drawableName: String, val group: Group, val label: String) {
    MAIN_1("com.tasnimulhasan.melodiq.alias.LogoMain", "ic_alias_main_1", "ic_logo_main", Group.MAIN, "Main"),
    MAIN_2("com.tasnimulhasan.melodiq.alias.LogoMain2", "ic_alias_main_2", "ic_logo_main_2", Group.MAIN, "Main 2"),
    MAIN_3("com.tasnimulhasan.melodiq.alias.LogoMain3", "ic_alias_main_3", "ic_logo_main_3", Group.MAIN, "Main 3"),
    MAIN_4("com.tasnimulhasan.melodiq.alias.LogoMain4", "ic_alias_main_4", "ic_logo_main_4", Group.MAIN, "Main 4"),
    ALT_1("com.tasnimulhasan.melodiq.alias.LogoAlt1", "ic_alias_alt_1", "ic_logo_alt", Group.CLASSIC, "Classic"),
    ALT_2("com.tasnimulhasan.melodiq.alias.LogoAlt2", "ic_alias_alt_2", "ic_logo_alt_2", Group.CLASSIC, "Classic 2"),
    ALT_3("com.tasnimulhasan.melodiq.alias.LogoAlt3", "ic_alias_alt_3", "ic_logo_alt_3", Group.CLASSIC, "Classic 3"),
    ALT_4("com.tasnimulhasan.melodiq.alias.LogoAlt4", "ic_alias_alt_4", "ic_logo_alt_4", Group.CLASSIC, "Classic 4");

    enum class Group(val title: String) { MAIN("Main logos"), CLASSIC("Classic logos") }

    companion object {
        val DEFAULT = MAIN_1
    }
}

/** The launcher-icon choice lives in the system's component-enabled state (the single source
 * of truth), so there is nothing to keep in sync in DataStore. */
internal object AppIconManager {

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
    }

    /** Resource id of the option's icon, looked up by name so this module doesn't need to
     * know which module the drawables live in. 0 if it isn't found. */
    fun previewResId(context: Context, option: AppIconOption): Int =
        context.resources.getIdentifier(option.drawableName, "drawable", context.packageName)
}
