package com.tasnimulhasan.domain.localusecase.playlists

import com.tasnimulhasan.domain.localusecase.RoomSuspendableUseCase
import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchPlaylistByNameUseCase @Inject constructor(
    private val repository: PlaylistsRepository
) : RoomSuspendableUseCase<SearchPlaylistByNameUseCase.Params, Flow<List<PlaylistEntity>>> {

    data class Params(
        val playlistName: String
    )

    override suspend fun invoke(params: Params): Flow<List<PlaylistEntity>> {
        return repository.searchPlaylistByName(params.playlistName)
    }
}