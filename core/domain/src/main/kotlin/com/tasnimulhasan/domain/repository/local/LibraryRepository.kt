package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.ListeningStats
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

    /** Live, fully sorted library straight from Room. Emits again on every library change. */
    fun observeAllSongs(sort: SortType): Flow<List<MusicEntity>>

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
    /** Same as [observeRecentlyPlayed] but paired with each song's actual last-played time
     * (epoch millis), for a "2 min ago" style label. */
    fun observeRecentlyPlayedWithTimestamp(limit: Int): Flow<List<Pair<MusicEntity, Long>>>
    fun observeMostPlayed(limit: Int): Flow<List<Pair<MusicEntity, Int>>>
    fun observeListeningStats(): Flow<ListeningStats>

    /** Smart playlist: songs that have never been played, newest first. */
    fun observeNeverPlayed(): Flow<List<MusicEntity>>
}
