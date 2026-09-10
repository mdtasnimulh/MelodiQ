package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import javax.inject.Inject

class LoadPlaylistUseCase @Inject constructor(
    private val repo: PlayerRepository
) {
    suspend operator fun invoke(musicList: List<MusicEntity>, sortType: SortType, keepCurrentTrack: Boolean = true) =
        repo.loadPlaylist(musicList, sortType, keepCurrentTrack)
}