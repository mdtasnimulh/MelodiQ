package com.tasnimulhasan.domain.localusecase.playlists

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCaseNonReturn
import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val repository: PlaylistsRepository
) : RoomSuspendableUseCaseNonReturn<DeletePlaylistUseCase.Params> {

    data class Params(
        val playlist: PlaylistEntity
    )

    override suspend fun invoke(params: Params) {
        return repository.deletePlaylist(params.playlist)
    }
}