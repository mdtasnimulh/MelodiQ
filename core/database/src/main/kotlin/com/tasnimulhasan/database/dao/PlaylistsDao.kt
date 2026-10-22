package com.tasnimulhasan.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(item: PlaylistEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlaylist(item: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(item: PlaylistEntity)

    /** The songs saved in a playlist; removed together with it so they can never reappear in a later playlist. */
    @Query("DELETE FROM playlist_details_table WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: Int)

    /** Backup/restore: one-shot reads and an insert that returns the new row id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistReturningId(item: PlaylistEntity): Long

    @Query("SELECT * FROM melodiq_playlist_table")
    suspend fun getAllOnce(): List<PlaylistEntity>

    @Query("DELETE FROM melodiq_playlist_table")
    suspend fun deleteAllPlaylists()

    @Query("SELECT * FROM melodiq_playlist_table ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM melodiq_playlist_table WHERE playlistName = :playlistName")
    fun searchPlaylistsByName(playlistName: String): Flow<List<PlaylistEntity>>

    // Song count and total duration computed live from playlist_details_table, so the
    // Playlists screen always shows the real numbers instead of a hardcoded "5".
    @Query(
        """SELECT p.*,
             (SELECT COUNT(*) FROM playlist_details_table d WHERE d.playlistId = p.id) AS songCount,
             (SELECT COALESCE(SUM(CAST(d.duration AS INTEGER)), 0) FROM playlist_details_table d WHERE d.playlistId = p.id) AS totalDurationMs,
             (SELECT GROUP_CONCAT(ref, char(10)) FROM (
                 SELECT (d.songId || ':' || COALESCE(d.albumId, 0) || ':' || d.contentUri) AS ref
                 FROM playlist_details_table d WHERE d.playlistId = p.id ORDER BY d.id DESC LIMIT 5)) AS coverRefs
           FROM melodiq_playlist_table p
           ORDER BY p.id DESC"""
    )
    fun getAllPlaylistsWithStats(): Flow<List<PlaylistWithStats>>
}