package com.tasnimulhasan.common.constant

object AppConstants {
    const val CHANNEL_ID = "melodiq_channel_id"
    const val CHANNEL_NAME = "melodiq_notification_channel"
    const val NOTIFICATION_REQUEST_CODE = 107

    const val NOTIFICATION_ID = 107
    const val NOTIFICATION_CHANNEL_NAME = "melodiq_notification_channel"
    const val NOTIFICATION_CHANNEL_ID = "melodiq_channel_id"

    const val FOREGROUND_ID = 122

    const val ACTION_PREVIOUS = "previous"
    const val ACTION_PLAY_PAUSE = "play_pause"
    const val ACTION_NEXT = "next"
    const val PLAYLISTS_TABLE_NAME = "melodiq_playlist_table"

    const val PRESET_CUSTOM = 0
    const val PRESET_FLAT = 1
    const val PRESET_ACOUSTIC = 2
    const val PRESET_DANCE_LOUNGE = 3
    const val PRESET_HIP_HOP = 4
    const val PRESET_JAZZ_BLUES = 5
    const val PRESET_POP = 6
    const val PRESET_ROCK = 7
    const val PRESET_PODCAST = 8

    // 5-band presets
    val FLAT = listOf(0.0, 0.0, 0.0, 0.0, 0.0)
    val ACOUSTIC = listOf(0.44, 0.12, 0.12, 0.34, 0.2)
    val DANCE = listOf(0.52, 0.08, 0.28, 0.48, 0.06)
    val HIP_HOPE = listOf(0.44, 0.06, -0.14, 0.1, 0.38)
    val JAZZ = listOf(0.32, 0.0, 0.22, 0.1, 0.2)
    val POP = listOf(-0.14, 0.28, 0.38, 0.22, -0.2)
    val ROCK = listOf(0.38, 0.2, -0.04, 0.02, 0.34)
    val PODCAST = listOf(-0.12, 0.26, 0.36, 0.16, -0.2)

    // 5-band presets
    /*val FLAT = List(5) { 0.0 }
    val ACOUSTIC = listOf(0.3, 0.2, 0.0, -0.1, 0.2)
    val DANCE = listOf(0.5, 0.3, 0.0, 0.3, 0.5)
    val HIP_HOPE = listOf(0.4, 0.2, 0.0, 0.2, 0.4)
    val JAZZ = listOf(0.2, 0.1, 0.0, 0.1, 0.3)
    val POP = listOf(-0.1, 0.2, 0.3, 0.2, -0.1)
    val ROCK = listOf(0.3, 0.1, -0.1, 0.1, 0.3)
    val PODCAST = listOf(0.0, 0.2, 0.3, 0.2, 0.0)*/

    // 10-band presets
    val FLAT_10 = List(10) { 0.0 }
    val ACOUSTIC_10 = listOf(0.4, 0.3, 0.2, 0.1, 0.0, -0.1, -0.1, 0.0, 0.1, 0.2)
    val DANCE_10 = listOf(0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.2, 0.3, 0.4, 0.5)
    val HIP_HOPE_10 = listOf(0.5, 0.4, 0.3, 0.2, 0.1, 0.0, 0.1, 0.2, 0.3, 0.4)
    val JAZZ_10 = listOf(0.3, 0.2, 0.1, 0.0, 0.0, 0.1, 0.2, 0.2, 0.3, 0.4)
    val POP_10 = listOf(-0.2, -0.1, 0.0, 0.2, 0.3, 0.3, 0.2, 0.1, 0.0, -0.1)
    val ROCK_10 = listOf(0.4, 0.3, 0.2, 0.0, -0.1, -0.1, 0.0, 0.2, 0.3, 0.4)
    val PODCAST_10 = listOf(0.0, 0.0, 0.1, 0.2, 0.3, 0.3, 0.2, 0.1, 0.0, 0.0)

    val effectType = listOf(
        "Custom", "Flat", "Acoustic", "Dance",
        "Hip Hop", "Jazz", "Pop", "Rock", "Podcast"
    )

    /*val effectType = listOf(
        "Flat",
        "Acoustic",
        "Dance",
        "Hip Hop",
        "Jazz/Blues",
        "Pop",
        "Rock",
        "Podcast",
        "Custom"
    )*/
}