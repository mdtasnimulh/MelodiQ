package com.tasnimulhasan.domain.localusecase.media

import com.tasnimulhasan.domain.service.MusicController
import com.tasnimulhasan.entity.Song
import javax.inject.Inject

class SkipToPreviousSongUseCase @Inject constructor(private val musicController: MusicController) {
    operator fun invoke(updateHomeUi: (Song?) -> Unit) {
        musicController.skipToPreviousSong()
        updateHomeUi(musicController.getCurrentSong())
    }
}