package com.tasnimulhasan.data.repoimpl.local

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.tasnimulhasan.database.dao.FavouriteDao
import com.tasnimulhasan.database.dao.LibrarySongDao
import com.tasnimulhasan.database.dao.PlayHistoryDao
import com.tasnimulhasan.database.dao.PlaylistDetailsDao
import com.tasnimulhasan.database.dao.PlaylistsDao
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.domain.repository.local.BackupRepository
import com.tasnimulhasan.entity.backup.BackupFile
import com.tasnimulhasan.entity.backup.BackupPlay
import com.tasnimulhasan.entity.backup.BackupPlaylist
import com.tasnimulhasan.entity.backup.BackupSong
import com.tasnimulhasan.entity.backup.RestoreSummary
import com.tasnimulhasan.entity.room.favourite.FavouriteEntity
import com.tasnimulhasan.entity.room.library.LibrarySongEntity
import com.tasnimulhasan.entity.room.library.PlayHistoryEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupRepoImpl @Inject constructor(
    private val gson: Gson,
    private val playlistsDao: PlaylistsDao,
    private val playlistDetailsDao: PlaylistDetailsDao,
    private val favouriteDao: FavouriteDao,
    private val librarySongDao: LibrarySongDao,
    private val playHistoryDao: PlayHistoryDao,
    private val preferences: PreferencesDataStoreRepository,
) : BackupRepository {

    override suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val libraryById = HashMap<Long, LibrarySongEntity>()
        suspend fun song(id: Long, fallbackTitle: String = "", fallbackArtist: String = "", fallbackDuration: Long = 0L): BackupSong {
            val lib = libraryById[id] ?: librarySongDao.getById(id)?.also { libraryById[id] = it }
            return BackupSong(
                songId = id,
                title = lib?.title ?: fallbackTitle,
                artist = lib?.artist ?: fallbackArtist,
                durationMs = lib?.durationMs ?: fallbackDuration,
                album = lib?.album,
                albumId = lib?.albumId,
            )
        }

        val playlists = playlistsDao.getAllOnce().map { playlist ->
            BackupPlaylist(
                name = playlist.playlistName,
                description = playlist.playlistDescription,
                createdAt = playlist.createdAt,
                songs = playlistDetailsDao.getForPlaylistOnce(playlist.id).map { row ->
                    song(row.songId, row.songTitle, row.artist.orEmpty(), row.duration.toLongOrNull() ?: 0L)
                },
            )
        }
        val favourites = favouriteDao.getAllOnce().map { song(it.songId) }
        val history = playHistoryDao.getAllOnce().map { BackupPlay(song(it.songId), it.playedAt) }

        gson.toJson(
            BackupFile(
                createdAt = System.currentTimeMillis(),
                playlists = playlists,
                favourites = favourites,
                playHistory = history,
                settings = preferences.exportSettings(),
            )
        )
    }

    override suspend fun restoreFromJson(json: String): RestoreSummary = withContext(Dispatchers.IO) {
        val backup = try {
            gson.fromJson(json, BackupFile::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
        require(backup != null && backup.version >= 1) { "This is not a MelodiQ backup file." }

        var notFound = 0
        // Cache so a song that appears in several places is only looked up once.
        val resolved = HashMap<BackupSong, LibrarySongEntity?>()
        suspend fun resolve(song: BackupSong): LibrarySongEntity? = resolved.getOrPut(song) {
            val byId = librarySongDao.getById(song.songId)
            if (byId != null && (song.title.isBlank() || byId.title.equals(song.title, ignoreCase = true))) byId
            else librarySongDao.findByKey(song.title.lowercase(), song.artist.lowercase(), song.durationMs)
        }

        // Playlists: merge into one with the same name, otherwise create it.
        val existingPlaylists = playlistsDao.getAllOnce().toMutableList()
        var playlistsRestored = 0
        var playlistSongs = 0
        for (backupPlaylist in backup.playlists) {
            val existing = existingPlaylists.firstOrNull { it.playlistName.equals(backupPlaylist.name, ignoreCase = true) }
            val playlistId = existing?.id ?: playlistsDao.insertPlaylistReturningId(
                PlaylistEntity(
                    playlistName = backupPlaylist.name,
                    playlistDescription = backupPlaylist.description,
                    createdAt = backupPlaylist.createdAt,
                )
            ).toInt().also { id ->
                existingPlaylists.add(PlaylistEntity(id, backupPlaylist.name, backupPlaylist.description, backupPlaylist.createdAt))
                playlistsRestored++
            }
            val alreadyIn = playlistDetailsDao.getForPlaylistOnce(playlistId).map { it.songId }.toHashSet()
            val rows = mutableListOf<PlaylistDetailsEntity>()
            for (song in backupPlaylist.songs) {
                val match = resolve(song)
                if (match == null) { notFound++; continue }
                if (!alreadyIn.add(match.songId)) continue
                rows += PlaylistDetailsEntity(
                    playlistId = playlistId,
                    contentUri = match.contentUriString,
                    songId = match.songId,
                    cover = "",
                    songTitle = match.title,
                    artist = match.artist,
                    duration = match.durationMs.toString(),
                    album = match.album,
                    albumId = match.albumId,
                )
            }
            if (rows.isNotEmpty()) playlistDetailsDao.insertMusicListToPlaylist(rows)
            playlistSongs += rows.size
        }

        // Favourites
        var favouritesRestored = 0
        val existingFavourites = favouriteDao.getAllOnce().map { it.songId }.toHashSet()
        for (song in backup.favourites) {
            val match = resolve(song)
            if (match == null) { notFound++; continue }
            if (existingFavourites.add(match.songId)) {
                favouriteDao.addFavourite(FavouriteEntity(songId = match.songId))
                favouritesRestored++
            }
        }

        // Listening history (skips entries that are already there, so restoring twice is safe)
        val existingPlays = playHistoryDao.getAllOnce().map { it.songId to it.playedAt }.toHashSet()
        val newPlays = mutableListOf<PlayHistoryEntity>()
        for (play in backup.playHistory) {
            val match = resolve(play.song) ?: continue
            if (existingPlays.add(match.songId to play.playedAt)) {
                newPlays += PlayHistoryEntity(songId = match.songId, playedAt = play.playedAt)
            }
        }
        if (newPlays.isNotEmpty()) playHistoryDao.insertAll(newPlays)

        val settingsApplied = preferences.importSettings(backup.settings)

        RestoreSummary(
            playlistsRestored = playlistsRestored,
            playlistSongsRestored = playlistSongs,
            favouritesRestored = favouritesRestored,
            playsRestored = newPlays.size,
            settingsRestored = settingsApplied,
            songsNotFound = notFound,
        )
    }
}
