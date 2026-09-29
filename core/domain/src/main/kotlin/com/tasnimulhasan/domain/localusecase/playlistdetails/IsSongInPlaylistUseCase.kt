package com.tasnimulhasan.domain.localusecase.playlistdetails

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCase
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import javax.inject.Inject

class IsSongInPlaylistUseCase @Inject constructor(
    private val repository: PlaylistDetailsRepository
) : RoomSuspendableUseCase<IsSongInPlaylistUseCase.Params, Boolean> {

    data class Params(
        val playlistId: Int,
        val songId: Long,
    )

    override suspend fun invoke(params: Params): Boolean {
        return repository.isSongInPlaylist(params.playlistId, params.songId)
    }
}
