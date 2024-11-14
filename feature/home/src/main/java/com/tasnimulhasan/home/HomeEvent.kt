package com.tasnimulhasan.home

import com.tasnimulhasan.entity.Song

sealed class HomeEvent {
    data object PlaySong : HomeEvent()
    data object PauseSong : HomeEvent()
    data object ResumeSong : HomeEvent()
    data object FetchSong : HomeEvent()
    data object SkipToNextSong : HomeEvent()
    data object SkipToPreviousSong : HomeEvent()
    data class OnSongSelected(val selectedSong: Song) : HomeEvent()
}