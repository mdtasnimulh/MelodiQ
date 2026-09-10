package com.tasnimulhasan.domain.player

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data class Ready(val duration: Long) : PlaybackState()
    data class Buffering(val position: Long) : PlaybackState()
    data class Playing(val isPlaying: Boolean) : PlaybackState()
    data class Progress(val position: Long) : PlaybackState()
    data class TrackChanged(val index: Int) : PlaybackState()
}