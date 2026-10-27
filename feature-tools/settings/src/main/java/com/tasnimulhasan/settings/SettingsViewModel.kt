package com.tasnimulhasan.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.datastore.GetAccentColorUseCase
import com.tasnimulhasan.domain.localusecase.datastore.CrossfadeSettings
import com.tasnimulhasan.domain.localusecase.datastore.GetCoverArtStyleUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetCrossfadeSettingsUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetCoverArtStyleUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetReplayGainEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetThemeConfigUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetVisualizerStyleUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetVisualizerStyleUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetAccentColorUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetCrossfadeDurationUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetCrossfadeEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetReplayGainEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetThemeConfigUseCase
import com.tasnimulhasan.entity.enums.AccentColorOption
import com.tasnimulhasan.entity.enums.CoverArtStyle
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.enums.VisualizerStyle
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.tasnimulhasan.domain.localusecase.backup.CreateBackupUseCase
import com.tasnimulhasan.domain.localusecase.backup.RestoreBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
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
    private val getVisualizerStyleUseCase: GetVisualizerStyleUseCase,
    private val setVisualizerStyleUseCase: SetVisualizerStyleUseCase,
    private val getCoverArtStyleUseCase: GetCoverArtStyleUseCase,
    private val setCoverArtStyleUseCase: SetCoverArtStyleUseCase,
    private val getMiniPlayerPositionUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetMiniPlayerPositionUseCase,
    private val setMiniPlayerPositionUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetMiniPlayerPositionUseCase,
    private val getPlayerTypeUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetPlayerTypeUseCase,
    private val setPlayerTypeUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetPlayerTypeUseCase,
    private val getUiStyleUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetUiStyleUseCase,
    private val setUiStyleUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetUiStyleUseCase,
    private val getCustomPaletteUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetCustomPaletteUseCase,
    private val setCustomPaletteUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetCustomPaletteUseCase,
    private val getNavLabelsUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetNavLabelsUseCase,
    private val setNavLabelsUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetNavLabelsUseCase,
    private val getAppFontUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetAppFontUseCase,
    private val setAppFontUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetAppFontUseCase,
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _appIcon = kotlinx.coroutines.flow.MutableStateFlow(AppIconManager.current(context))
    val appIcon: StateFlow<AppIconOption> = _appIcon

    fun setAppIcon(option: AppIconOption) {
        if (option == _appIcon.value) return
        try {
            AppIconManager.apply(context, option)
            _appIcon.value = option
            _messages.tryEmit("Icon changed. Your launcher may take a few seconds to show it.")
        } catch (e: Exception) {
            _messages.tryEmit("Couldn't change the icon: ${e.message ?: "unknown error"}")
        }
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun exportBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            val msg = try {
                val json = createBackupUseCase()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: error("Could not open the file")
                }
                "Backup saved"
            } catch (e: Exception) {
                "Backup failed: ${e.message ?: "unknown error"}"
            }
            _messages.emit(msg)
        }
    }

    fun importBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            val msg = try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("Could not open the file")
                }
                val r = restoreBackupUseCase(json)
                buildString {
                    append("Restored ${r.playlistsRestored} playlists, ${r.favouritesRestored} favourites, ${r.playsRestored} plays")
                    if (r.songsNotFound > 0) append(" (${r.songsNotFound} songs not on this device)")
                }
            } catch (e: Exception) {
                "Restore failed: ${e.message ?: "invalid backup file"}"
            }
            _messages.emit(msg)
        }
    }

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

    val visualizerStyle: StateFlow<VisualizerStyle> = getVisualizerStyleUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VisualizerStyle.OFF)

    val coverArtStyle: StateFlow<CoverArtStyle> = getCoverArtStyleUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoverArtStyle.HALF)

    val miniPlayerPosition: StateFlow<com.tasnimulhasan.entity.enums.MiniPlayerPosition> = getMiniPlayerPositionUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tasnimulhasan.entity.enums.MiniPlayerPosition.BOTTOM_END)

    fun setMiniPlayerPosition(position: com.tasnimulhasan.entity.enums.MiniPlayerPosition) {
        viewModelScope.launch { setMiniPlayerPositionUseCase(position) }
    }

    fun miniPlayerPositionLabel(position: com.tasnimulhasan.entity.enums.MiniPlayerPosition): String = when (position) {
        com.tasnimulhasan.entity.enums.MiniPlayerPosition.BOTTOM_END -> "Bottom right (default)"
        com.tasnimulhasan.entity.enums.MiniPlayerPosition.BOTTOM_START -> "Bottom left"
        com.tasnimulhasan.entity.enums.MiniPlayerPosition.TOP_END -> "Top right"
        com.tasnimulhasan.entity.enums.MiniPlayerPosition.TOP_START -> "Top left"
    }

    val uiStyle: StateFlow<com.tasnimulhasan.entity.enums.UiStyle> = getUiStyleUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            com.tasnimulhasan.designsystem.theme.UiStyleCache.read(context),
        )

    fun setUiStyle(style: com.tasnimulhasan.entity.enums.UiStyle) {
        // The synchronous copy first, so the next launch (and the very next frame) already match.
        com.tasnimulhasan.designsystem.theme.UiStyleCache.write(context, style)
        viewModelScope.launch { setUiStyleUseCase(style) }
    }

    fun uiStyleLabel(style: com.tasnimulhasan.entity.enums.UiStyle): String = when (style) {
        com.tasnimulhasan.entity.enums.UiStyle.DEFAULT -> "Default (solid)"
        com.tasnimulhasan.entity.enums.UiStyle.GLASS -> "Glass UI"
    }

    val playerType: StateFlow<com.tasnimulhasan.entity.enums.PlayerType> = getPlayerTypeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tasnimulhasan.entity.enums.PlayerType.CLASSIC)

    fun setPlayerType(type: com.tasnimulhasan.entity.enums.PlayerType) {
        viewModelScope.launch { setPlayerTypeUseCase(type) }
    }

    fun playerTypeLabel(type: com.tasnimulhasan.entity.enums.PlayerType): String = when (type) {
        com.tasnimulhasan.entity.enums.PlayerType.CLASSIC -> "A · MelodiQ (default)"
        com.tasnimulhasan.entity.enums.PlayerType.IPOD -> "B · iPod style"
    }

    fun setCoverArtStyle(style: CoverArtStyle) {
        viewModelScope.launch { setCoverArtStyleUseCase(style) }
    }

    fun coverArtStyleLabel(style: CoverArtStyle): String = when (style) {
        CoverArtStyle.HALF -> "Half screen (default)"
        CoverArtStyle.CIRCLE -> "Circle"
        CoverArtStyle.FULL -> "Full screen"
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        viewModelScope.launch { setVisualizerStyleUseCase(style) }
    }

    fun visualizerStyleLabel(style: VisualizerStyle): String = when (style) {
        VisualizerStyle.OFF -> "Off"
        VisualizerStyle.BARS -> "Bars"
        VisualizerStyle.WAVEFORM -> "Waveform"
        VisualizerStyle.CIRCULAR -> "Circular"
    }

    fun setSortType(type: SortType) {
        viewModelScope.launch { setSortTypeUseCase(type) }
    }

    fun setThemeConfig(config: DarkThemeConfig) {
        viewModelScope.launch { setThemeConfigUseCase(config) }
    }

    val customPalette: StateFlow<com.tasnimulhasan.entity.enums.PaletteChoice> = getCustomPaletteUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tasnimulhasan.entity.enums.PaletteChoice())

    /** Picks a swatch: stores the style + seed and switches the accent over to the custom palette. */
    fun setCustomPalette(style: com.tasnimulhasan.entity.enums.PaletteStyle, seed: androidx.compose.ui.graphics.Color) {
        viewModelScope.launch {
            setCustomPaletteUseCase(com.tasnimulhasan.entity.enums.PaletteChoice(style, seed.toArgb()))
            setAccentColorUseCase(AccentColorOption.CUSTOM)
        }
    }

    /** "Use system Material You": on = wallpaper colours, off = the saved custom palette. */
    fun setSystemColors(on: Boolean) {
        viewModelScope.launch { setAccentColorUseCase(if (on) AccentColorOption.DYNAMIC else AccentColorOption.CUSTOM) }
    }

    val showNavLabels: StateFlow<Boolean> = getNavLabelsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val appFont: StateFlow<com.tasnimulhasan.entity.enums.AppFont> = getAppFontUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tasnimulhasan.entity.enums.AppFont.DEFAULT)

    fun setAppFont(font: com.tasnimulhasan.entity.enums.AppFont) {
        viewModelScope.launch { setAppFontUseCase(font) }
    }

    fun setNavLabels(show: Boolean) {
        viewModelScope.launch { setNavLabelsUseCase(show) }
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
        AccentColorOption.APP_ICON -> "App icon"
        AccentColorOption.CUSTOM -> "Custom palette"
        AccentColorOption.PURPLE -> "Purple"
        AccentColorOption.BLUE -> "Blue"
        AccentColorOption.GREEN -> "Green"
        AccentColorOption.ORANGE -> "Orange"
        AccentColorOption.PINK -> "Pink"
        AccentColorOption.RED -> "Red"
        AccentColorOption.TEAL -> "Teal"
    }
}
