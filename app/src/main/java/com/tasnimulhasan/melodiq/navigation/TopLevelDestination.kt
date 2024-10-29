package com.tasnimulhasan.melodiq.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.home.R as homeR
import com.tasnimulhasan.settings.R as settingsR

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector,
    val iconTextId: Int,
    val titleTextId: Int
) {
    HOME(
        selectedIcon = MelodiqIcons.Home,
        unSelectedIcon = MelodiqIcons.HomeOutline,
        iconTextId = homeR.string.feature_home_title,
        titleTextId = homeR.string.feature_home_title
    ),

    SETTINGS(
        selectedIcon = MelodiqIcons.Settings,
        unSelectedIcon = MelodiqIcons.SettingsOutline,
        iconTextId = settingsR.string.feature_settings_title,
        titleTextId = settingsR.string.feature_settings_title
    )
}