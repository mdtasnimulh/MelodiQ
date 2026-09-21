package com.tasnimulhasan.featureplayer.lyrics

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.lyrics.ObserveLyricsUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.player.PlaybackState
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.lyrics.LyricLine
import com.tasnimulhasan.entity.lyrics.LyricsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val observeLyricsUseCase: ObserveLyricsUseCase,
    private val playerUseCases: PlayerUseCases,
) : ViewModel() {

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

    val currentSong: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    // Re-resolves lyrics whenever the playing song actually changes (not on every emission -
    // observeCurrentSelectedAudio can re-emit the same song). Loading a new song's lyrics
    // never blocks or interrupts playback: this is a plain reactive read on Dispatchers.IO,
    // entirely separate from the playback pipeline.
    val lyricsState: StateFlow<LyricsUiState> = playerUseCases.observeCurrentSelectedAudio()
        .filter { it != null && it.songId != 0L }
        .distinctUntilChangedBy { it!!.songId }
        .flatMapLatest { song -> observeLyricsUseCase(song!!) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsUiState.Loading)

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = playerUseCases.getPlaybackSnapshot()
            _positionMs.value = snapshot.position
        }
        viewModelScope.launch {
            playerUseCases.observeAudioState().collectLatest { state ->
                when (state) {
                    is PlaybackState.Progress -> _positionMs.value = state.position
                    is PlaybackState.Buffering -> _positionMs.value = state.position
                    else -> Unit
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch { playerUseCases.seekTo(positionMs) }
    }
}

/** Index of the last line whose timestamp has already passed - i.e. the line that should be
 * highlighted right now. Returns -1 for unsynchronized lyrics or before the first line. */
fun currentLyricLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty() || lines.first().timestampMs == null) return -1
    var result = -1
    for (i in lines.indices) {
        val ts = lines[i].timestampMs ?: continue
        if (ts <= positionMs) result = i else break
    }
    return result
}
