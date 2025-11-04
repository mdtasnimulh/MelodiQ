package com.tasnimulhasan.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(item: PlaylistEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlaylist(item: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(item: PlaylistEntity)

    @Query("DELETE FROM melodiq_playlist_table")
    suspend fun deleteAllPlaylists()

    @Query("SELECT * FROM melodiq_playlist_table ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM melodiq_playlist_table WHERE playlistName = :playlistName")
    fun searchPlaylistsByName(playlistName: String): Flow<List<PlaylistEntity>>
}