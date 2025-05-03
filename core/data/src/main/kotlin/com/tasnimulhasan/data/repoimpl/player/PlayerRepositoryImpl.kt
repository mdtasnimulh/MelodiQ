package com.tasnimulhasan.data.repoimpl.player

import androidx.media3.session.MediaSession
import com.tasnimulhasan.domain.repository.PlayerRepository
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val mediaSession: MediaSession
) : PlayerRepository {

    private val player get() = mediaSession.player

    override fun play() {
        player.playWhenReady = true
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun skipToNext() {
        player.seekToNext()
    }

    override fun skipToPrevious() {
        player.seekToPrevious()
    }

    override fun getCurrentPosition(): Long {
        return player.currentPosition
    }
}