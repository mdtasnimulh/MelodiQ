package com.tasnimulhasan.domain.repository

import com.tasnimulhasan.domain.player.PlaybackSnapshot
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    suspend fun loadPlaylist(musicList: List<MusicEntity>, sortType: SortType, keepCurrentTrack: Boolean = true)
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