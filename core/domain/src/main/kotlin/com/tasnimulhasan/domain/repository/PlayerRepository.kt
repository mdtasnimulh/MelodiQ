package com.tasnimulhasan.domain.repository

import com.tasnimulhasan.domain.player.PlaybackSnapshot
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    // Single source of truth for "what queue is loaded" and "what song is currently
    // selected". Every screen (mini player, full player, home list) reads these SAME
    // StateFlow instances instead of independently fetching the library and indexing
    // into its own copy - that duplication was the root cause of the mini/full player
    // showing different songs after a sort change or library update.
    val audioList: StateFlow<List<MusicEntity>>
    val currentSelectedAudio: StateFlow<MusicEntity?>
    val isPlaying: StateFlow<Boolean>

    suspend fun loadPlaylist(musicList: List<MusicEntity>, sortType: SortType, keepCurrentTrack: Boolean = true)

    /**
     * Plays a specific, self-contained list of tracks (e.g. a user playlist) starting at
     * [startIndex], WITHOUT touching [audioList] - that StateFlow is the full library that
     * Home/Songs render from, and must never be overwritten by a smaller curated list or
     * those screens would start showing the wrong songs after the user leaves this queue.
     */
    suspend fun playCuratedQueue(musicList: List<MusicEntity>, startIndex: Int)
    // --- Queue management ---
    suspend fun playNext(song: MusicEntity)
    suspend fun playLater(song: MusicEntity)
    suspend fun addToQueue(songs: List<MusicEntity>)
    suspend fun removeFromQueue(index: Int)
    suspend fun moveQueueItem(from: Int, to: Int)
    suspend fun clearQueue()

    // --- Playback tuning ---
    suspend fun setPlaybackSpeed(speed: Float)
    fun getPlaybackSpeed(): Float
    fun setSeekStepMs(stepMs: Long)
    fun getSeekStepMs(): Long

    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(position: Long)
    suspend fun next()
    suspend fun previous()
    suspend fun forward()
    suspend fun backward()
    suspend fun getCurrentDuration(): Long
    suspend fun selectAudio(index: Int)
    suspend fun updateProgress(progress: Float)
    suspend fun observeAudioState(): StateFlow<PlaybackState>
    suspend fun getCurrentSongInfo(): MusicEntity?
    suspend fun getPlaybackSnapshot(): PlaybackSnapshot
    suspend fun repeatTrackOne()
    suspend fun repeatTrackAll()
    suspend fun repeatTrackOff()
    fun isPlaybackServiceRunning(): Boolean
    fun ensurePlaybackServiceStarted()
}