package com.tasnimulhasan.ui

import com.tasnimulhasan.ui.NavRoutes.ABOUT_ROUTE
import com.tasnimulhasan.ui.NavRoutes.ALBUMS_ROUTE
import com.tasnimulhasan.ui.NavRoutes.FAVOURITE_ROUTE
import com.tasnimulhasan.ui.NavRoutes.FEEDBACK_ROUTE
import com.tasnimulhasan.ui.NavRoutes.HOME_ROUTE
import com.tasnimulhasan.ui.NavRoutes.PLAYER_ROUTE
import com.tasnimulhasan.ui.NavRoutes.PLAYLISTS_ROUTE
import com.tasnimulhasan.ui.NavRoutes.QUEUE_ROUTE
import com.tasnimulhasan.ui.NavRoutes.SETTINGS_ROUTE
import com.tasnimulhasan.ui.NavRoutes.SONGS_ROUTE

object DeepLinks {
    const val DEEP_LINK_HOME = "app://com.melodiq.user/{$HOME_ROUTE}"
    const val DEEP_LINK_ALBUMS = "app://com.melodiq.user/{$ALBUMS_ROUTE}"
    const val DEEP_LINK_SONGS = "app://com.melodiq.user/{$SONGS_ROUTE}"
    const val DEEP_LINK_PLAYLISTS = "app://com.melodiq.user/{$PLAYLISTS_ROUTE}"
    const val DEEP_LINK_FAVOURITE = "app://com.melodiq.user/{$FAVOURITE_ROUTE}"
    const val DEEP_LINK_PLAYER = "app://com.melodiq.user/{$PLAYER_ROUTE}"
    const val DEEP_LINK_QUEUE = "app://com.melodiq.user/{$QUEUE_ROUTE}"
    const val DEEP_LINK_ABOUT = "app://com.melodiq.user/{$ABOUT_ROUTE}"
    const val DEEP_LINK_FEEDBACK = "app://com.melodiq.user/{$FEEDBACK_ROUTE}"
    const val DEEP_LINK_SETTINGS = "app://com.melodiq.user/{$SETTINGS_ROUTE}"
}