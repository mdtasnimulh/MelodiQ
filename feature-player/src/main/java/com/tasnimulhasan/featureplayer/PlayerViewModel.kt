package com.tasnimulhasan.featureplayer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val fetchMusicUseCase: FetchMusicUseCase,
) : BaseViewModel() {

    val musics = mutableStateListOf<MusicEntity>()

    private val _uiState = mutableStateOf<UiState>(UiState.Loading)
    val uiState get() = _uiState

    init {
        fetchMusicList()
    }

    private fun fetchMusicList() {
        viewModelScope.launch {
            musics.addAll(fetchMusicUseCase.execute())
            _uiState.value = UiState.MusicList(musics)
        }
    }

    fun getSelectedMusic(id: String) : MusicEntity {
        return musics.find { it.songId.toString() == id }!!
    }

}

sealed interface UiState {
    data object Loading : UiState
    data object Error : UiState
    data class MusicList(val musics: List<MusicEntity>) : UiState
}