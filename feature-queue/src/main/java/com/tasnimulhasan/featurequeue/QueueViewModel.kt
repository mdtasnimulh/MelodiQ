package com.tasnimulhasan.featurequeue

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerUseCases: PlayerUseCases,
    private val playQueueIndex: com.tasnimulhasan.domain.localusecase.player.PlayQueueIndexUseCase,
) : BaseViewModel() {

    private val dummyAudio = MusicEntity(
        contentUri = "".toUri(),
        songId = 0L,
        cover = null,
        songTitle = "",
        artist = "",
        duration = "",
        albumId = 0L,
        album = ""
    )

    // The queue actually loaded in the player. It used to read the browsable library list, which
    // never changed when an entry was removed or moved, so Remove and Move looked like they did nothing.
    val queue: StateFlow<List<MusicEntity>> = playerUseCases.observeAudioList.queue()

    val currentSelectedAudio: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    val isPlaying: StateFlow<Boolean> = playerUseCases.observeIsPlaying()

    fun playAt(index: Int) = viewModelScope.launch { playQueueIndex(index) }

    fun removeAt(index: Int) = viewModelScope.launch { playerUseCases.removeFromQueue(index) }

    fun move(from: Int, to: Int) = viewModelScope.launch { playerUseCases.moveQueueItem(from, to) }

    fun clear() = viewModelScope.launch { playerUseCases.clearQueue() }
}
