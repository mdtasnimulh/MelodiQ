package com.tasnimulhasan.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.music.FetchMusicUseCase
import com.tasnimulhasan.entity.home.MusicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fetchMusicUseCase: FetchMusicUseCase,
) : BaseViewModel() {

    val musics = mutableStateListOf<MusicEntity>()
    private var initialized = false

    private val _uiState = mutableStateOf<UiState>(UiState.Loading)
    val uiState get() = _uiState

    init {
        initializeListIfNeeded()
    }

    fun initializeListIfNeeded() {
        if (initialized) return

        viewModelScope.launch {
            musics.addAll(fetchMusicUseCase.execute())
            _uiState.value = UiState.MusicList(musics)
        }
        initialized = true
    }

    fun loadBitmapIfNeeded(context: Context, index: Int) {
        if (musics[index].cover != null) return
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = getAlbumArt(context, musics[index].contentUri)
            musics[index] = musics[index].copy(cover = bitmap)
        }
    }

    private fun getAlbumArt(context: Context, uri: Uri): Bitmap? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        val data = mmr.embeddedPicture

        return if (data != null) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } else {
            null
        }
    }
}

sealed interface UiState {
    data object Loading : UiState
    data object Error : UiState
    data class MusicList(val musics: List<MusicEntity>) : UiState
}