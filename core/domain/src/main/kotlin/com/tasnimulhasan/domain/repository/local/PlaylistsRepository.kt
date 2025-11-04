package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlinx.coroutines.flow.Flow

interface PlaylistsRepository {
    suspend fun insertPlaylist(item: PlaylistEntity)

    suspend fun updatePlaylist(item: PlaylistEntity)

    suspend fun deletePlaylist(item: PlaylistEntity)

    suspend fun deleteAllPlaylist()

    fun getAllPlaylist(): Flow<List<PlaylistEntity>>

    fun searchPlaylistByName(playlistName: String): Flow<List<PlaylistEntity>>
}