package com.tasnimulhasan.entity.room.lyrics

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Successfully fetched online lyrics, cached so the same song never needs a second network
 * request. Local sources (embedded tag, .lrc file) are never cached here - they're already
 * on disk and re-reading them costs nothing, so caching them would just be a second copy of
 * data that can go stale if the user edits the file.
 */
@Entity(tableName = "lyrics_cache_table")
data class LyricsCacheEntity(
    @PrimaryKey
    val songId: Long,
    val rawLyrics: String,
    val isSynced: Boolean,
    val providerName: String,
    val fetchedAt: Long,
)
