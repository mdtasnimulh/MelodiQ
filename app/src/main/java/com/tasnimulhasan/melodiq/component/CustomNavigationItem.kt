package com.tasnimulhasan.melodiq.component

import androidx.compose.ui.graphics.vector.ImageVector
import com.tasnimulhasan.designsystem.R as Res
import com.tasnimulhasan.designsystem.icon.MelodiqIcons

enum class CustomNavigationItem(
    val title: String,
    val icon: Int,
) {
    ABOUT(
        title = "About",
        icon = Res.drawable.ic_about,
    ),
    FEEDBACK(
        title = "Feedback",
        icon = Res.drawable.ic_feedback,
    ),
    FAVOURITE(
        title = "Favourite",
        icon = Res.drawable.ic_favorite,
    ),
    SETTINGS(
        title = "Settings",
        icon = Res.drawable.ic_settings,
    ),
}