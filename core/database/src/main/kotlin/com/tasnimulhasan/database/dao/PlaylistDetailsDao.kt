package com.tasnimulhasan.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasnimulhasan.entity.room.playlist.PlaylistDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusicToPlaylist(item: PlaylistDetailsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusicListToPlaylist(items: List<PlaylistDetailsEntity>)

    @Delete
    suspend fun deletePlaylistMusic(item: PlaylistDetailsEntity)

    @Query("DELETE FROM playlist_details_table")
    suspend fun deleteAllPlaylistMusic()

    @Query("SELECT * FROM playlist_details_table WHERE playlistId = :playlistId ORDER BY id DESC")
    fun getAllPlaylistMusic(playlistId: Int): Flow<List<PlaylistDetailsEntity>>

    @Query("SELECT * FROM playlist_details_table WHERE songTitle || artist || album = :searchKey")
    fun searchPlaylistMusic(searchKey: String): Flow<List<PlaylistDetailsEntity>>
}