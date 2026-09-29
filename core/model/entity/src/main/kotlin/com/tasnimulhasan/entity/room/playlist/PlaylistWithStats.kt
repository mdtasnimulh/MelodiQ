package com.tasnimulhasan.entity.room.playlist

import androidx.room.Embedded

/**
 * Not a Room @Entity/table of its own - just the shape of a joined query result: a
 * [PlaylistEntity] plus stats computed live from playlist_details_table. Room maps a
 * @Query's columns into this via @Embedded, so the Playlists screen can show a real,
 * always-current "12 songs - 42:10" instead of a hardcoded string.
 */
data class PlaylistWithStats(
    @Embedded
    val playlist: PlaylistEntity,
    val songCount: Int,
    val totalDurationMs: Long,
)
