package com.tasnimulhasan.domain.localusecase.playlists

import com.tasnimulhasan.domain.localusecase.RoomCollectableUseCaseNoParams
import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllPlaylistUseCase @Inject constructor(
    private val repository: PlaylistsRepository
) : RoomCollectableUseCaseNoParams<List<PlaylistEntity>> {

    override fun invoke(): Flow<List<PlaylistEntity>> {
        return repository.getAllPlaylist()
    }

}