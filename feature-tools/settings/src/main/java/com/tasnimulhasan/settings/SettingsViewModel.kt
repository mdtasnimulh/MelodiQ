package com.tasnimulhasan.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.datastore.GetAccentColorUseCase
import com.tasnimulhasan.domain.localusecase.datastore.CrossfadeSettings
import com.tasnimulhasan.domain.localusecase.datastore.GetCrossfadeSettingsUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetReplayGainEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetThemeConfigUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetAccentColorUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetCrossfadeDurationUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetCrossfadeEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetReplayGainEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetThemeConfigUseCase
import com.tasnimulhasan.entity.enums.AccentColorOption
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.entity.enums.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSortTypeUseCase: GetSortTypeUseCase,
    private val setSortTypeUseCase: SetSortTypeUseCase,
    private val getThemeConfigUseCase: GetThemeConfigUseCase,
    private val setThemeConfigUseCase: SetThemeConfigUseCase,
    private val getAccentColorUseCase: GetAccentColorUseCase,
    private val setAccentColorUseCase: SetAccentColorUseCase,
    private val getReplayGainEnabledUseCase: GetReplayGainEnabledUseCase,
    private val setReplayGainEnabledUseCase: SetReplayGainEnabledUseCase,
    private val getCrossfadeSettingsUseCase: GetCrossfadeSettingsUseCase,
    private val setCrossfadeEnabledUseCase: SetCrossfadeEnabledUseCase,
    private val setCrossfadeDurationUseCase: SetCrossfadeDurationUseCase,
) : ViewModel() {

    val sortType: StateFlow<SortType> = getSortTypeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SortType.DATE_MODIFIED_DESC)

    val themeConfig: StateFlow<DarkThemeConfig> = getThemeConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DarkThemeConfig.FOLLOW_SYSTEM)

    val accentColor: StateFlow<AccentColorOption> = getAccentColorUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColorOption.DYNAMIC)

    val replayGainEnabled: StateFlow<Boolean> = getReplayGainEnabledUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val crossfadeSettings: StateFlow<CrossfadeSettings> = getCrossfadeSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CrossfadeSettings(false, 4_000L))

    fun setSortType(type: SortType) {
        viewModelScope.launch { setSortTypeUseCase(type) }
    }

    fun setThemeConfig(config: DarkThemeConfig) {
        viewModelScope.launch { setThemeConfigUseCase(config) }
    }

    fun setAccentColor(option: AccentColorOption) {
        viewModelScope.launch { setAccentColorUseCase(option) }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch { setReplayGainEnabledUseCase(enabled) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { setCrossfadeEnabledUseCase(enabled) }
    }

    fun setCrossfadeDurationMs(durationMs: Long) {
        viewModelScope.launch { setCrossfadeDurationUseCase(durationMs) }
    }

    fun sortTypeToDisplayString(sortType: SortType): String = when (sortType) {
        SortType.DATE_MODIFIED_ASC -> "Date Modified (oldest first)"
        SortType.DATE_MODIFIED_DESC -> "Date Modified (newest first)"
        SortType.NAME_ASC -> "Name (A-Z)"
        SortType.NAME_DESC -> "Name (Z-A)"
        SortType.ARTIST_ASC -> "Artist (A-Z)"
        SortType.ARTIST_DESC -> "Artist (Z-A)"
        SortType.DURATION_ASC -> "Duration (shortest first)"
        SortType.DURATION_DESC -> "Duration (longest first)"
    }

    fun themeConfigToDisplayString(config: DarkThemeConfig): String = when (config) {
        DarkThemeConfig.FOLLOW_SYSTEM -> "Follow system"
        DarkThemeConfig.LIGHT -> "Light"
        DarkThemeConfig.DARK -> "Dark"
        DarkThemeConfig.AMOLED -> "AMOLED (true black)"
    }

    fun accentColorLabel(option: AccentColorOption): String = when (option) {
        AccentColorOption.DYNAMIC -> "Dynamic (wallpaper)"
        AccentColorOption.ALBUM_ART -> "Album art"
        AccentColorOption.PURPLE -> "Purple"
        AccentColorOption.BLUE -> "Blue"
        AccentColorOption.GREEN -> "Green"
        AccentColorOption.ORANGE -> "Orange"
        AccentColorOption.PINK -> "Pink"
        AccentColorOption.RED -> "Red"
        AccentColorOption.TEAL -> "Teal"
    }
}
