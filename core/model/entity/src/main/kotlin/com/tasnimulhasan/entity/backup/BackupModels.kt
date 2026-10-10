package com.tasnimulhasan.entity.backup

/** Everything that is written to / read from a MelodiQ backup file. */
data class BackupFile(
    val version: Int = 1,
    val createdAt: Long = 0L,
    val playlists: List<BackupPlaylist> = emptyList(),
    val favourites: List<BackupSong> = emptyList(),
    val playHistory: List<BackupPlay> = emptyList(),
    /** Preference name -> "type:value" (see PreferencesDataStoreRepository.exportSettings). */
    val settings: Map<String, String> = emptyMap(),
)

/** A song identified by more than its MediaStore id, so it can still be found after a
 * reinstall or on another phone where that id is different. */
data class BackupSong(
    val songId: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val album: String? = null,
    val albumId: Long? = null,
)

data class BackupPlaylist(
    val name: String = "",
    val description: String = "",
    val createdAt: Long = 0L,
    val songs: List<BackupSong> = emptyList(),
)

data class BackupPlay(
    val song: BackupSong = BackupSong(),
    val playedAt: Long = 0L,
)

data class RestoreSummary(
    val playlistsRestored: Int,
    val playlistSongsRestored: Int,
    val favouritesRestored: Int,
    val playsRestored: Int,
    val settingsRestored: Int,
    /** Songs from the backup that could not be found in this device's library. */
    val songsNotFound: Int,
)
