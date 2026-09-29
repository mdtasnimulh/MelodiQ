package com.tasnimulhasan.domain.localusecase.playlists

import com.tasnimulhasan.domain.localusecase.RoomCollectableUseCaseNoParams
import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistWithStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllPlaylistsWithStatsUseCase @Inject constructor(
    private val repository: PlaylistsRepository
) : RoomCollectableUseCaseNoParams<List<PlaylistWithStats>> {

    override fun invoke(): Flow<List<PlaylistWithStats>> {
        return repository.getAllPlaylistsWithStats()
    }
}
