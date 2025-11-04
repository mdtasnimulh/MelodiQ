package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.database.dao.PlaylistsDao
import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlaylistRepoImpl @Inject constructor(
    private val playlistsDao: PlaylistsDao
): PlaylistsRepository {
    override suspend fun insertPlaylist(item: PlaylistEntity) {
        return playlistsDao.insertPlaylist(item)
    }

    override suspend fun updatePlaylist(item: PlaylistEntity) {
        return playlistsDao.updatePlaylist(item)
    }

    override suspend fun deletePlaylist(item: PlaylistEntity) {
        return playlistsDao.deletePlaylist(item)
    }

    override suspend fun deleteAllPlaylist() {
        return playlistsDao.deleteAllPlaylists()
    }

    override fun getAllPlaylist(): Flow<List<PlaylistEntity>> {
        return playlistsDao.getAllPlaylists()
    }

    override fun searchPlaylistByName(playlistName: String): Flow<List<PlaylistEntity>> {
        return playlistsDao.searchPlaylistsByName(playlistName = playlistName)
    }
}