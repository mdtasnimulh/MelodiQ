package com.tasnimulhasan.domain.localusecase.playlistdetails

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCase
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMusicFromPlaylistUseCase @Inject constructor(
    private val repository: PlaylistDetailsRepository
) : RoomSuspendableUseCase<SearchMusicFromPlaylistUseCase.Params, Flow<List<PlaylistDetailsEntity>>> {

    data class Params(
        val searchKey: String
    )

    override suspend fun invoke(params: Params): Flow<List<PlaylistDetailsEntity>> {
        return repository.searchPlaylistMusic(params.searchKey)
    }
}