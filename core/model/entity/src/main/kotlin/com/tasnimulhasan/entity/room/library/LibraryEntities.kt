package com.tasnimulhasan.entity.room.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A locally-cached copy of one MediaStore audio row, kept in Room so the library (songs,
 * artists, albums, genres, folders) can be browsed, searched and paged straight from the
 * database instead of re-querying the ContentResolver and holding the whole result set in
 * memory every time a screen needs it.
 *
 * Populated and kept up to date by MediaStoreLibraryScanner, which diffs against
 * [dateModified] to update only what actually changed on an incremental rescan.
 */
@Entity(
    tableName = "library_song_table",
    indices = [
        Index(value = ["titleKey"]),
        Index(value = ["artistKey"]),
        Index(value = ["albumKey"]),
        Index(value = ["genre"]),
        Index(value = ["folderPath"]),
        Index(value = ["albumId"]),
        Index(value = ["dateModified"]),
    ]
)
data class LibrarySongEntity(
    @PrimaryKey
    val songId: Long,
    val title: String,
    // Lowercased copies used for fast, index-backed LIKE search - keeps the search query
    // itself simple and case-insensitive without a COLLATE NOCASE scan on every row.
    val titleKey: String,
    val artist: String,
    val artistKey: String,
    val album: String,
    val albumKey: String,
    val albumId: Long,
    val genre: String?,
    val durationMs: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    // Parent directory (e.g. "Music/English/Rock") - drives folder-based browsing/playback.
    val folderPath: String,
    val contentUriString: String,
)

/** One playback start event - the raw log that "recently played" and "most played" are both
 * derived from via aggregation, rather than maintaining separate counters that can drift. */
@Entity(
    tableName = "play_history_table",
    indices = [Index(value = ["songId"]), Index(value = ["playedAt"])]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

/** Row shape for the "which ids does Room already have, and when were they last modified"
 * diff query the incremental scanner runs before deciding what to insert/update/delete. */
data class SongIdAndModified(
    @ColumnInfo(name = "songId") val songId: Long,
    @ColumnInfo(name = "dateModified") val dateModified: Long,
)

data class ArtistSummary(
    val artist: String,
    val songCount: Int,
    val representativeSongId: Long,
)

data class AlbumSummary(
    val albumId: Long,
    val album: String,
    val artist: String,
    val songCount: Int,
    val representativeSongId: Long,
)

data class GenreSummary(
    val genre: String,
    val songCount: Int,
    val representativeSongId: Long,
)

data class FolderSummary(
    val folderPath: String,
    val songCount: Int,
    val representativeSongId: Long,
)

data class PlayCountRow(
    val songId: Long,
    val playCount: Int,
)
