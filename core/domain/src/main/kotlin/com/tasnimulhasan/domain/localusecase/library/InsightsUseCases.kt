package com.tasnimulhasan.domain.localusecase.library

import com.tasnimulhasan.domain.repository.local.LibraryRepository
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.ArtistPlay
import com.tasnimulhasan.entity.room.library.ListeningStats
import com.tasnimulhasan.entity.room.library.PlayTimeRow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Smart playlists: lists the app builds by itself from the library and the play history.
 * Kept as its own injectable class (not part of [LibraryUseCases]) so adding it needs no
 * Hilt module changes.
 */
class SmartPlaylistUseCases @Inject constructor(private val repo: LibraryRepository) {
    /** Songs played most since [sinceMillis], with their play counts. */
    fun mostPlayedSince(sinceMillis: Long, limit: Int = 100): Flow<List<Pair<MusicEntity, Int>>> =
        repo.observeMostPlayedSince(sinceMillis, limit)

    fun recentlyAdded(sinceSeconds: Long, limit: Int = 200): Flow<List<MusicEntity>> =
        repo.observeRecentlyAdded(sinceSeconds, limit)

    fun longTracks(minDurationMs: Long, limit: Int = 200): Flow<List<MusicEntity>> =
        repo.observeLongTracks(minDurationMs, limit)

    /** Played at some point, but not since [beforeMillis]. */
    fun notPlayedSince(beforeMillis: Long, limit: Int = 200): Flow<List<MusicEntity>> =
        repo.observeNotPlayedSince(beforeMillis, limit)
}

/** Everything the Listening stats page shows. [sinceMillis] = 0 means all time. */
class ListeningInsightsUseCases @Inject constructor(private val repo: LibraryRepository) {
    fun totals(sinceMillis: Long): Flow<ListeningStats> = repo.observeListeningStatsSince(sinceMillis)

    fun topSongs(sinceMillis: Long, limit: Int = 5): Flow<List<Pair<MusicEntity, Int>>> =
        repo.observeMostPlayedSince(sinceMillis, limit)

    fun topArtists(sinceMillis: Long, limit: Int = 5): Flow<List<ArtistPlay>> =
        repo.observeTopArtistsSince(sinceMillis, limit)

    fun playTimes(sinceMillis: Long): Flow<List<PlayTimeRow>> = repo.observePlayTimesSince(sinceMillis)
}
