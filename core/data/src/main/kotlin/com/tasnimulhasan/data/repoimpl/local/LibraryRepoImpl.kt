package com.tasnimulhasan.data.repoimpl.local

import android.net.Uri
import com.tasnimulhasan.data.library.MediaStoreLibraryScanner
import com.tasnimulhasan.database.dao.LibrarySongDao
import com.tasnimulhasan.database.dao.PlayHistoryDao
import com.tasnimulhasan.domain.repository.local.LibraryRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.AlbumSummary
import com.tasnimulhasan.entity.room.library.ArtistSummary
import com.tasnimulhasan.entity.room.library.FolderSummary
import com.tasnimulhasan.entity.room.library.GenreSummary
import com.tasnimulhasan.entity.room.library.LibrarySongEntity
import com.tasnimulhasan.entity.room.library.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val HISTORY_KEEP_COUNT = 2_000

class LibraryRepoImpl @Inject constructor(
    private val librarySongDao: LibrarySongDao,
    private val playHistoryDao: PlayHistoryDao,
    private val scanner: MediaStoreLibraryScanner,
) : LibraryRepository {

    override suspend fun scanLibrary(force: Boolean) = scanner.scanIfNeeded(force)

    override fun observeTotalSongCount(): Flow<Int> = librarySongDao.observeTotalCount()

    override suspend fun getSongsPage(sort: SortType, limit: Int, offset: Int): List<MusicEntity> {
        val (column, ascending) = sort.toColumnAndDirection()
        return librarySongDao.getSongsPage(column, ascending, limit, offset).map { it.toMusicEntity() }
    }

    override suspend fun searchSongs(query: String, limit: Int, offset: Int): List<MusicEntity> {
        if (query.isBlank()) return emptyList()
        return librarySongDao.searchSongs(query.trim().lowercase(), limit, offset).map { it.toMusicEntity() }
    }

    override fun observeArtists(): Flow<List<ArtistSummary>> = librarySongDao.observeArtists()
    override fun observeAlbums(): Flow<List<AlbumSummary>> = librarySongDao.observeAlbums()
    override fun observeGenres(): Flow<List<GenreSummary>> = librarySongDao.observeGenres()
    override fun observeFolders(): Flow<List<FolderSummary>> = librarySongDao.observeFolders()

    override suspend fun getSongsByArtist(artist: String, limit: Int, offset: Int): List<MusicEntity> =
        librarySongDao.getSongsByArtist(artist.lowercase(), limit, offset).map { it.toMusicEntity() }

    override suspend fun getSongsByAlbum(albumId: Long, limit: Int, offset: Int): List<MusicEntity> =
        librarySongDao.getSongsByAlbum(albumId, limit, offset).map { it.toMusicEntity() }

    override suspend fun getSongsByGenre(genre: String, limit: Int, offset: Int): List<MusicEntity> =
        librarySongDao.getSongsByGenre(genre, limit, offset).map { it.toMusicEntity() }

    override suspend fun getSongsInFolder(folderPath: String, limit: Int, offset: Int): List<MusicEntity> =
        librarySongDao.getSongsInFolderExact(folderPath, limit, offset).map { it.toMusicEntity() }

    override suspend fun getSongsUnderFolder(folderPath: String): List<MusicEntity> =
        librarySongDao.getSongsUnderFolder(folderPath).map { it.toMusicEntity() }

    override suspend fun recordPlay(songId: Long) {
        playHistoryDao.recordPlay(PlayHistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))
        playHistoryDao.pruneOlderThan(HISTORY_KEEP_COUNT)
    }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<MusicEntity>> =
        playHistoryDao.observeRecentlyPlayedIds(limit).map { ids ->
            val songs = librarySongDao.getByIds(ids).associateBy { it.songId }
            // getByIds doesn't preserve order - reassert "most recent first" from the ids list.
            ids.mapNotNull { songs[it] }.map { it.toMusicEntity() }
        }

    override fun observeMostPlayed(limit: Int): Flow<List<Pair<MusicEntity, Int>>> =
        playHistoryDao.observeMostPlayed(limit).map { rows ->
            val songs = librarySongDao.getByIds(rows.map { it.songId }).associateBy { it.songId }
            rows.mapNotNull { row -> songs[row.songId]?.let { it.toMusicEntity() to row.playCount } }
        }

    private fun SortType.toColumnAndDirection(): Pair<String, Boolean> = when (this) {
        SortType.NAME_ASC -> "title" to true
        SortType.NAME_DESC -> "title" to false
        SortType.ARTIST_ASC -> "artist" to true
        SortType.ARTIST_DESC -> "artist" to false
        SortType.DURATION_ASC -> "duration" to true
        SortType.DURATION_DESC -> "duration" to false
        SortType.DATE_MODIFIED_ASC -> "date" to true
        SortType.DATE_MODIFIED_DESC -> "date" to false
    }

    private fun LibrarySongEntity.toMusicEntity(): MusicEntity = MusicEntity(
        contentUri = Uri.parse(contentUriString),
        songId = songId,
        cover = null,
        songTitle = title,
        artist = artist,
        duration = durationMs.toString(),
        album = album,
        albumId = albumId,
        dateAdded = dateAdded,
    )
}
