package com.tasnimulhasan.domain.localusecase.playlistdetails

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCaseNonReturn
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import javax.inject.Inject

class InsertMusicToPlaylistUseCase @Inject constructor(
    private val repository: PlaylistDetailsRepository
) : RoomSuspendableUseCaseNonReturn<InsertMusicToPlaylistUseCase.Params> {

    data class Params(
        val item: PlaylistDetailsEntity
    )

    override suspend fun invoke(params: Params) {
        return repository.insertMusicToPlaylist(params.item)
    }
}