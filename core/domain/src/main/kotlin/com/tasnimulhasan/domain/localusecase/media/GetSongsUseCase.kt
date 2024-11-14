package com.tasnimulhasan.domain.localusecase.media

import com.tasnimulhasan.domain.repository.SongRepository
import javax.inject.Inject

class GetSongsUseCase @Inject constructor(private val musicRepository: SongRepository) {
    operator fun invoke() = musicRepository.getSongs()
}