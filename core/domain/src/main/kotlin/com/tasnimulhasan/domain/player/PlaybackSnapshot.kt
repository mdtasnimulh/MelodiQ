package com.tasnimulhasan.domain.player

import com.tasnimulhasan.entity.enums.SortType

data class PlaybackSnapshot(
    val currentIndex: Int,
    val duration: Long,
    val position: Long,
    val isPlaying: Boolean,
    val mediaItemCount: Int,
    val sortType: SortType,
)