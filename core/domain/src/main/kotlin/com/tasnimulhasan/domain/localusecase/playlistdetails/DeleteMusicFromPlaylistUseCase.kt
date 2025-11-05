package com.tasnimulhasan.domain.localusecase.playlistdetails

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCaseNonReturn
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import javax.inject.Inject

class DeleteMusicFromPlaylistUseCase @Inject constructor(
    private val repository: PlaylistDetailsRepository
) : RoomSuspendableUseCaseNonReturn<DeleteMusicFromPlaylistUseCase.Params> {

    data class Params(
        val item: PlaylistDetailsEntity
    )

    override suspend fun invoke(params: Params) {
        return repository.deleteMusicFromPlaylist(params.item)
    }
}