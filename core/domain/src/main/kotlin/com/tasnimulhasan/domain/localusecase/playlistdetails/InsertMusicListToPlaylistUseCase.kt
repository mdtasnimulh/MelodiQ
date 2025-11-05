package com.tasnimulhasan.domain.localusecase.playlistdetails

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCaseNonReturn
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import javax.inject.Inject

class InsertMusicListToPlaylistUseCase @Inject constructor(
    private val repository: PlaylistDetailsRepository
) : RoomSuspendableUseCaseNonReturn<InsertMusicListToPlaylistUseCase.Params> {

    data class Params(
        val item: List<PlaylistDetailsEntity>
    )

    override suspend fun invoke(params: Params) {
        return repository.insertMusicListToPlaylist(params.item)
    }
}