package com.tasnimulhasan.domain.localusecase.lyrics

import com.tasnimulhasan.domain.repository.local.LyricsRepository
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.lyrics.LyricsUiState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLyricsUseCase @Inject constructor(
    private val repository: LyricsRepository
) {
    operator fun invoke(song: MusicEntity): Flow<LyricsUiState> = repository.observeLyrics(song)
}
