package com.tasnimulhasan.domain.localusecase.media

import com.tasnimulhasan.domain.service.MusicController
import javax.inject.Inject

class PauseSongUseCase @Inject constructor(private val musicController: MusicController) {
    operator fun invoke() = musicController.pause()
}