package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.lyrics.LyricsUiState
import kotlinx.coroutines.flow.Flow

/**
 * Resolves lyrics for a song following the offline-first priority chain: embedded tag ->
 * local .lrc file -> cached online lyrics -> online fetch (only if all of the above missed
 * AND a connection is available) -> unavailable.
 *
 * Returns a Flow rather than a single suspend result so the UI can show Loading immediately
 * and then the final state, without the caller needing to build that sequencing itself.
 */
interface LyricsRepository {
    fun observeLyrics(song: MusicEntity): Flow<LyricsUiState>
}
