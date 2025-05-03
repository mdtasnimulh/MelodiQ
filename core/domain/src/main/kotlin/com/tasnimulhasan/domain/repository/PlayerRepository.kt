package com.tasnimulhasan.domain.repository

interface PlayerRepository {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun getCurrentPosition(): Long
}