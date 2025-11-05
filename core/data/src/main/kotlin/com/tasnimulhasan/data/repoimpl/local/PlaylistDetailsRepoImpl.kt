package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.database.dao.PlaylistDetailsDao
import com.tasnimulhasan.domain.localusecase.playlistdetails.GetAllMusicFromPlaylistUseCase
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlaylistDetailsRepoImpl @Inject constructor(
    private val dao: PlaylistDetailsDao
): PlaylistDetailsRepository {
    override suspend fun insertMusicToPlaylist(item: PlaylistDetailsEntity) {
        return dao.insertMusicToPlaylist(item)
    }

    override suspend fun insertMusicListToPlaylist(item: List<PlaylistDetailsEntity>) {
        return dao.insertMusicListToPlaylist(item)
    }

    override suspend fun deleteMusicFromPlaylist(item: PlaylistDetailsEntity) {
        return dao.deletePlaylistMusic(item)
    }

    override suspend fun deleteAllPlaylistMusic() {
        return dao.deleteAllPlaylistMusic()
    }

    override fun getAllPlaylistMusic(params: GetAllMusicFromPlaylistUseCase.Params): Flow<List<PlaylistDetailsEntity>> {
        return dao.getAllPlaylistMusic(params.playlistId)
    }

    override fun searchPlaylistMusic(searchKey: String): Flow<List<PlaylistDetailsEntity>> {
        return dao.searchPlaylistMusic(searchKey)
    }
}