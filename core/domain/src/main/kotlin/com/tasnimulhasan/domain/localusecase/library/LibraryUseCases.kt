package com.tasnimulhasan.domain.localusecase.library

import com.tasnimulhasan.domain.repository.local.LibraryRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.AlbumSummary
import com.tasnimulhasan.entity.room.library.ArtistSummary
import com.tasnimulhasan.entity.room.library.FolderSummary
import com.tasnimulhasan.entity.room.library.GenreSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanLibraryUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(force: Boolean = false) = repo.scanLibrary(force)
}

class ObserveTotalSongCountUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(): Flow<Int> = repo.observeTotalSongCount()
}

class GetSongsPageUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(sort: SortType, limit: Int, offset: Int): List<MusicEntity> =
        repo.getSongsPage(sort, limit, offset)
}

class SearchLibraryUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(query: String, limit: Int = 50, offset: Int = 0): List<MusicEntity> =
        repo.searchSongs(query, limit, offset)
}

class ObserveArtistsUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(): Flow<List<ArtistSummary>> = repo.observeArtists()
}

class ObserveAlbumsUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(): Flow<List<AlbumSummary>> = repo.observeAlbums()
}

class ObserveGenresUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(): Flow<List<GenreSummary>> = repo.observeGenres()
}

class ObserveFoldersUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(): Flow<List<FolderSummary>> = repo.observeFolders()
}

class GetSongsByArtistUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(artist: String, limit: Int = 500, offset: Int = 0): List<MusicEntity> =
        repo.getSongsByArtist(artist, limit, offset)
}

class GetSongsByAlbumUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(albumId: Long, limit: Int = 500, offset: Int = 0): List<MusicEntity> =
        repo.getSongsByAlbum(albumId, limit, offset)
}

class GetSongsByGenreUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(genre: String, limit: Int = 500, offset: Int = 0): List<MusicEntity> =
        repo.getSongsByGenre(genre, limit, offset)
}

class GetSongsInFolderUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(folderPath: String, limit: Int = 500, offset: Int = 0): List<MusicEntity> =
        repo.getSongsInFolder(folderPath, limit, offset)
}

class GetSongsUnderFolderUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(folderPath: String): List<MusicEntity> = repo.getSongsUnderFolder(folderPath)
}

class RecordPlayUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(songId: Long) = repo.recordPlay(songId)
}

class ObserveRecentlyPlayedUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(limit: Int = 30): Flow<List<MusicEntity>> = repo.observeRecentlyPlayed(limit)
}

class ObserveMostPlayedUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(limit: Int = 30): Flow<List<Pair<MusicEntity, Int>>> = repo.observeMostPlayed(limit)
}

/** Bundled the same way PlayerUseCases is, so feature ViewModels take one constructor
 * parameter instead of fifteen. */
data class LibraryUseCases(
    val scanLibrary: ScanLibraryUseCase,
    val observeTotalSongCount: ObserveTotalSongCountUseCase,
    val getSongsPage: GetSongsPageUseCase,
    val searchLibrary: SearchLibraryUseCase,
    val observeArtists: ObserveArtistsUseCase,
    val observeAlbums: ObserveAlbumsUseCase,
    val observeGenres: ObserveGenresUseCase,
    val observeFolders: ObserveFoldersUseCase,
    val getSongsByArtist: GetSongsByArtistUseCase,
    val getSongsByAlbum: GetSongsByAlbumUseCase,
    val getSongsByGenre: GetSongsByGenreUseCase,
    val getSongsInFolder: GetSongsInFolderUseCase,
    val getSongsUnderFolder: GetSongsUnderFolderUseCase,
    val recordPlay: RecordPlayUseCase,
    val observeRecentlyPlayed: ObserveRecentlyPlayedUseCase,
    val observeMostPlayed: ObserveMostPlayedUseCase,
)
