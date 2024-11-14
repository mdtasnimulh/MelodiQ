package com.tasnimulhasan.domain.localusecase.media

import com.tasnimulhasan.domain.service.MusicController
import javax.inject.Inject

class GetCurrentSongPositionUseCase @Inject constructor(
    private val musicController: MusicController
) {
    operator fun invoke() = musicController.getCurrentPosition()
}