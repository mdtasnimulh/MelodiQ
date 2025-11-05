package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.domain.localusecase.playlistdetails.GetAllMusicFromPlaylistUseCase
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import kotlinx.coroutines.flow.Flow

interface PlaylistDetailsRepository {
    suspend fun insertMusicToPlaylist(item: PlaylistDetailsEntity)

    suspend fun insertMusicListToPlaylist(item: List<PlaylistDetailsEntity>)

    suspend fun deleteMusicFromPlaylist(item: PlaylistDetailsEntity)

    suspend fun deleteAllPlaylistMusic()

    fun getAllPlaylistMusic(params: GetAllMusicFromPlaylistUseCase.Params): Flow<List<PlaylistDetailsEntity>>

    fun searchPlaylistMusic(searchKey: String): Flow<List<PlaylistDetailsEntity>>
}