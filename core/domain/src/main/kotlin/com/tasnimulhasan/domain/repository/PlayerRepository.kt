package com.tasnimulhasan.domain.repository

import com.tasnimulhasan.common.service.MelodiqAudioState
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
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
    suspend fun observeAudioState(): StateFlow<MelodiqAudioState>
    suspend fun getCurrentSongInfo(): MusicEntity?
    suspend fun repeatTrackOne()
    suspend fun repeatTrackAll()
    suspend fun repeatTrackOff()
}