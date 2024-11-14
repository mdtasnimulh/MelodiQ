package com.tasnimulhasan.domain.localusecase.media

import com.tasnimulhasan.domain.service.MusicController
import com.tasnimulhasan.entity.Song
import javax.inject.Inject

class AddMediaItemsUseCase @Inject constructor(private val musicController: MusicController) {

    operator fun invoke(songs: List<Song>) {
        musicController.addMediaItems(songs)
    }
}