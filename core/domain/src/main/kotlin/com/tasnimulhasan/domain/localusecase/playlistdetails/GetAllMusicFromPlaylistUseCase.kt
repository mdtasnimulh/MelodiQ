package com.tasnimulhasan.domain.localusecase.playlistdetails

import com.tasnimulhasan.domain.localusecase.RoomCollectableUseCase
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllMusicFromPlaylistUseCase @Inject constructor(
    private val repository: PlaylistDetailsRepository
) : RoomCollectableUseCase<GetAllMusicFromPlaylistUseCase.Params, List<PlaylistDetailsEntity>> {

    data class Params(
        val playlistId: Int
    )

    override fun invoke(params: Params): Flow<List<PlaylistDetailsEntity>> {
        return repository.getAllPlaylistMusic(params)
    }
}