package com.tasnimulhasan.melodiq

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.designsystem.theme.fixedSeedColorOrNull
import com.tasnimulhasan.domain.localusecase.datastore.GetAccentColorUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetThemeConfigUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.enums.AccentColorOption
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.ui.image.AlbumArtAccentExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    getThemeConfigUseCase: GetThemeConfigUseCase,
    getAccentColorUseCase: GetAccentColorUseCase,
    playerUseCases: PlayerUseCases,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val themeConfig: StateFlow<DarkThemeConfig> = getThemeConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DarkThemeConfig.FOLLOW_SYSTEM)

    private val accentColorOption: StateFlow<AccentColorOption> = getAccentColorUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColorOption.DYNAMIC)

    val useDynamicColor: StateFlow<Boolean> = accentColorOption
        .map { it == AccentColorOption.DYNAMIC }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Recomputed (Palette extraction) only when the accent mode is ALBUM_ART AND the
    // currently playing song actually changes - never runs for any other accent mode, and
    // never re-runs on every progress tick since only songId is compared.
    val accentSeedColor: StateFlow<Color?> = combine(
        accentColorOption,
        playerUseCases.observeCurrentSelectedAudio(),
    ) { option, song -> option to song }
        .distinctUntilChanged { old, new -> old.first == new.first && old.second?.songId == new.second?.songId }
        .map { (option, song) ->
            when (option) {
                AccentColorOption.ALBUM_ART -> song?.let {
                    AlbumArtAccentExtractor.extractAccentColor(
                        context = appContext,
                        contentUri = it.contentUri,
                        albumId = it.albumId ?: 0L,
                    )
                }
                else -> option.fixedSeedColorOrNull()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
