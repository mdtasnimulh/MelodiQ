package com.tasnimulhasan.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasnimulhasan.entity.room.library.AlbumSummary
import com.tasnimulhasan.entity.room.library.ArtistSummary
import com.tasnimulhasan.entity.room.library.FolderSummary
import com.tasnimulhasan.entity.room.library.GenreSummary
import com.tasnimulhasan.entity.room.library.LibrarySongEntity
import com.tasnimulhasan.entity.room.library.ListeningStats
import com.tasnimulhasan.entity.room.library.PlayCountRow
import com.tasnimulhasan.entity.room.library.PlayHistoryEntity
import com.tasnimulhasan.entity.room.library.RecentPlayRow
import com.tasnimulhasan.entity.room.library.SongIdAndModified
import kotlinx.coroutines.flow.Flow

@Dao
interface LibrarySongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<LibrarySongEntity>)

    @Query("DELETE FROM library_song_table WHERE songId IN (:songIds)")
    suspend fun deleteByIds(songIds: List<Long>)

    /** Cheap diff source for the incremental scanner - just ids + modified timestamps,
     * never the full rows, so comparing against a fresh MediaStore query stays lightweight
     * even at 10,000+ songs. */
    @Query("SELECT songId, dateModified FROM library_song_table")
    suspend fun getAllIdsAndModified(): List<SongIdAndModified>

    @Query("SELECT COUNT(*) FROM library_song_table")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM library_song_table")
    suspend fun getTotalCount(): Int

    // --- Windowed reads. Every list-returning query below takes limit/offset so a screen
    // only ever pulls in the page it's about to show, never the whole table. ---

    @Query(
        """SELECT * FROM library_song_table
           ORDER BY
             CASE WHEN :sortColumn = 'title' AND :ascending = 1 THEN titleKey END ASC,
             CASE WHEN :sortColumn = 'title' AND :ascending = 0 THEN titleKey END DESC,
             CASE WHEN :sortColumn = 'artist' AND :ascending = 1 THEN artistKey END ASC,
             CASE WHEN :sortColumn = 'artist' AND :ascending = 0 THEN artistKey END DESC,
             CASE WHEN :sortColumn = 'duration' AND :ascending = 1 THEN durationMs END ASC,
             CASE WHEN :sortColumn = 'duration' AND :ascending = 0 THEN durationMs END DESC,
             CASE WHEN :sortColumn = 'date' AND :ascending = 1 THEN dateModified END ASC,
             CASE WHEN :sortColumn = 'date' AND :ascending = 0 THEN dateModified END DESC
           LIMIT :limit OFFSET :offset"""
    )
    suspend fun getSongsPage(sortColumn: String, ascending: Boolean, limit: Int, offset: Int): List<LibrarySongEntity>

    /**
     * The whole library, already sorted by SQLite, as a live query. This is what backs the
     * Home list and the player queue: Room re-emits it whenever the scanner inserts, updates
     * or deletes a row, so new downloads / deleted files show up without any manual refresh.
     * songId is the final tie-break so equal keys always come back in the same order.
     */
    @Query(
        """SELECT * FROM library_song_table
           ORDER BY
             CASE WHEN :sortColumn = 'title' AND :ascending = 1 THEN titleKey END ASC,
             CASE WHEN :sortColumn = 'title' AND :ascending = 0 THEN titleKey END DESC,
             CASE WHEN :sortColumn = 'artist' AND :ascending = 1 THEN artistKey END ASC,
             CASE WHEN :sortColumn = 'artist' AND :ascending = 0 THEN artistKey END DESC,
             CASE WHEN :sortColumn = 'duration' AND :ascending = 1 THEN durationMs END ASC,
             CASE WHEN :sortColumn = 'duration' AND :ascending = 0 THEN durationMs END DESC,
             CASE WHEN :sortColumn = 'date' AND :ascending = 1 THEN dateModified END ASC,
             CASE WHEN :sortColumn = 'date' AND :ascending = 0 THEN dateModified END DESC,
             songId ASC"""
    )
    fun observeAllSorted(sortColumn: String, ascending: Boolean): Flow<List<LibrarySongEntity>>

    @Query(
        """SELECT * FROM library_song_table
           WHERE titleKey LIKE '%' || :query || '%'
              OR artistKey LIKE '%' || :query || '%'
              OR albumKey LIKE '%' || :query || '%'
              OR genre LIKE '%' || :query || '%'
              OR folderPath LIKE '%' || :query || '%'
           ORDER BY titleKey ASC
           LIMIT :limit OFFSET :offset"""
    )
    suspend fun searchSongs(query: String, limit: Int, offset: Int): List<LibrarySongEntity>

    @Query(
        """SELECT artist, COUNT(*) AS songCount, MIN(songId) AS representativeSongId
           FROM library_song_table GROUP BY artistKey ORDER BY artistKey ASC"""
    )
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query(
        """SELECT albumId, album, artist, COUNT(*) AS songCount, MIN(songId) AS representativeSongId
           FROM library_song_table GROUP BY albumId ORDER BY albumKey ASC"""
    )
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query(
        """SELECT genre, COUNT(*) AS songCount, MIN(songId) AS representativeSongId
           FROM library_song_table WHERE genre IS NOT NULL AND genre != ''
           GROUP BY genre ORDER BY genre ASC"""
    )
    fun observeGenres(): Flow<List<GenreSummary>>

    @Query(
        """SELECT folderPath, COUNT(*) AS songCount, MIN(songId) AS representativeSongId
           FROM library_song_table GROUP BY folderPath ORDER BY folderPath ASC"""
    )
    fun observeFolders(): Flow<List<FolderSummary>>

    @Query("SELECT * FROM library_song_table WHERE artistKey = :artistKey ORDER BY albumKey ASC, titleKey ASC LIMIT :limit OFFSET :offset")
    suspend fun getSongsByArtist(artistKey: String, limit: Int, offset: Int): List<LibrarySongEntity>

    @Query("SELECT * FROM library_song_table WHERE albumId = :albumId ORDER BY titleKey ASC LIMIT :limit OFFSET :offset")
    suspend fun getSongsByAlbum(albumId: Long, limit: Int, offset: Int): List<LibrarySongEntity>

    @Query("SELECT * FROM library_song_table WHERE genre = :genre ORDER BY titleKey ASC LIMIT :limit OFFSET :offset")
    suspend fun getSongsByGenre(genre: String, limit: Int, offset: Int): List<LibrarySongEntity>

    /** Exact match - the songs directly inside this folder (not subfolders), for tree browsing. */
    @Query("SELECT * FROM library_song_table WHERE folderPath = :folderPath ORDER BY titleKey ASC LIMIT :limit OFFSET :offset")
    suspend fun getSongsInFolderExact(folderPath: String, limit: Int, offset: Int): List<LibrarySongEntity>

    /** Everything under this folder, including subfolders - for "play all" on a directory. */
    @Query("SELECT * FROM library_song_table WHERE folderPath = :folderPath OR folderPath LIKE :folderPath || '/%' ORDER BY folderPath ASC, titleKey ASC")
    suspend fun getSongsUnderFolder(folderPath: String): List<LibrarySongEntity>

    /** Songs with no entry in the play history at all - the "Never played" smart playlist.
     * Room re-emits when either table changes, so a song leaves the list the moment it is played. */
    @Query(
        """SELECT * FROM library_song_table
           WHERE songId NOT IN (SELECT DISTINCT songId FROM play_history_table)
           ORDER BY dateModified DESC"""
    )
    fun observeNeverPlayed(): Flow<List<LibrarySongEntity>>

    @Query("SELECT * FROM library_song_table WHERE songId = :songId LIMIT 1")
    suspend fun getById(songId: Long): LibrarySongEntity?

    @Query("SELECT * FROM library_song_table WHERE songId IN (:songIds)")
    suspend fun getByIds(songIds: List<Long>): List<LibrarySongEntity>

    /** Backup/restore: finds "the same song" on a device where its MediaStore id changed. */
    @Query(
        """SELECT * FROM library_song_table
           WHERE titleKey = :titleKey AND artistKey = :artistKey AND ABS(durationMs - :durationMs) < 2000
           LIMIT 1"""
    )
    suspend fun findByKey(titleKey: String, artistKey: String, durationMs: Long): LibrarySongEntity?
}

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun recordPlay(entry: PlayHistoryEntity)

    @Insert
    suspend fun insertAll(entries: List<PlayHistoryEntity>)

    @Query("SELECT * FROM play_history_table")
    suspend fun getAllOnce(): List<PlayHistoryEntity>

    /** Distinct songs, most-recently-played first. */
    @Query(
        """SELECT songId FROM play_history_table
           GROUP BY songId ORDER BY MAX(playedAt) DESC LIMIT :limit"""
    )
    fun observeRecentlyPlayedIds(limit: Int): Flow<List<Long>>

    /** Same distinct-songs-most-recent-first set, but carrying each song's actual last-played
     * timestamp - for a "2 min ago" style label rather than just ordering. */
    @Query(
        """SELECT songId, MAX(playedAt) AS playedAt FROM play_history_table
           GROUP BY songId ORDER BY playedAt DESC LIMIT :limit"""
    )
    fun observeRecentlyPlayedWithTimestamp(limit: Int): Flow<List<RecentPlayRow>>

    @Query(
        """SELECT songId, COUNT(*) AS playCount FROM play_history_table
           GROUP BY songId ORDER BY playCount DESC LIMIT :limit"""
    )
    fun observeMostPlayed(limit: Int): Flow<List<PlayCountRow>>

    @Query(
        """SELECT id, songId, playedAt FROM play_history_table
           WHERE playedAt >= :sinceMillis ORDER BY playedAt DESC"""
    )
    fun observeHistorySince(sinceMillis: Long): Flow<List<PlayHistoryEntity>>

    /** Keeps the log from growing forever on a device that's played music for years. */
    @Query(
        """DELETE FROM play_history_table WHERE id NOT IN
           (SELECT id FROM play_history_table ORDER BY playedAt DESC LIMIT :keepCount)"""
    )
    suspend fun pruneOlderThan(keepCount: Int)

    /** Total plays and total time listened, joined against each song's own duration rather
     * than kept as separate running counters that could drift from the history log. */
    @Query(
        """SELECT COUNT(*) AS totalPlays, COALESCE(SUM(s.durationMs), 0) AS totalDurationMs
           FROM play_history_table h
           INNER JOIN library_song_table s ON s.songId = h.songId"""
    )
    fun observeListeningStats(): Flow<ListeningStats>
}
