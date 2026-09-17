package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.AlbumSummary
import com.tasnimulhasan.entity.room.library.ArtistSummary
import com.tasnimulhasan.entity.room.library.FolderSummary
import com.tasnimulhasan.entity.room.library.GenreSummary
import kotlinx.coroutines.flow.Flow

/**
 * The local library: a Room-backed cache of the device's MediaStore audio collection, kept
 * fresh by MediaStoreLibraryScanner. Every read here is either a reactive grouped summary
 * (artists/albums/genres/folders - cheap, bounded by distinct-value count, not song count)
 * or an explicitly windowed page (limit/offset) - nothing here loads the whole library into
 * memory at once, which matters once a library reaches into the thousands of songs.
 */
interface LibraryRepository {
    suspend fun scanLibrary(force: Boolean = false)
    fun observeTotalSongCount(): Flow<Int>

    suspend fun getSongsPage(sort: SortType, limit: Int, offset: Int): List<MusicEntity>
    suspend fun searchSongs(query: String, limit: Int, offset: Int): List<MusicEntity>

    fun observeArtists(): Flow<List<ArtistSummary>>
    fun observeAlbums(): Flow<List<AlbumSummary>>
    fun observeGenres(): Flow<List<GenreSummary>>
    fun observeFolders(): Flow<List<FolderSummary>>

    suspend fun getSongsByArtist(artist: String, limit: Int, offset: Int): List<MusicEntity>
    suspend fun getSongsByAlbum(albumId: Long, limit: Int, offset: Int): List<MusicEntity>
    suspend fun getSongsByGenre(genre: String, limit: Int, offset: Int): List<MusicEntity>
    suspend fun getSongsInFolder(folderPath: String, limit: Int, offset: Int): List<MusicEntity>
    suspend fun getSongsUnderFolder(folderPath: String): List<MusicEntity>

    suspend fun recordPlay(songId: Long)
    fun observeRecentlyPlayed(limit: Int): Flow<List<MusicEntity>>
    fun observeMostPlayed(limit: Int): Flow<List<Pair<MusicEntity, Int>>>
}
