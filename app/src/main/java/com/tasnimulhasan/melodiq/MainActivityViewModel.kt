package com.tasnimulhasan.melodiq

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.designsystem.theme.fixedSeedColorOrNull
import com.tasnimulhasan.domain.localusecase.datastore.GetAccentColorUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetThemeConfigUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetUiStyleUseCase
import com.tasnimulhasan.designsystem.theme.UiStyleCache
import com.tasnimulhasan.entity.enums.UiStyle
import kotlinx.coroutines.flow.onEach
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.melodiq.shortcut.AppShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tasnimulhasan.domain.localusecase.library.ScanLibraryUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.enums.AccentColorOption
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.settings.AppIconManager
import com.tasnimulhasan.ui.image.AlbumArtAccentExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
    getUiStyleUseCase: GetUiStyleUseCase,
    getCustomPaletteUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetCustomPaletteUseCase,
    getNavLabelsUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetNavLabelsUseCase,
    getAppFontUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetAppFontUseCase,
    getPlayerTypeUseCase: com.tasnimulhasan.domain.localusecase.datastore.GetPlayerTypeUseCase,
    private val setPlayerTypeUseCase: com.tasnimulhasan.domain.localusecase.datastore.SetPlayerTypeUseCase,
    private val playerUseCases: PlayerUseCases,
    private val observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
    private val scanLibraryUseCase: ScanLibraryUseCase,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val themeConfig: StateFlow<DarkThemeConfig> = getThemeConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DarkThemeConfig.FOLLOW_SYSTEM)

    /** Default or Glass. Starts from a synchronous copy so a cold start never flashes the wrong look. */
    val uiStyle: StateFlow<UiStyle> = getUiStyleUseCase()
        .onEach { UiStyleCache.write(appContext, it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiStyleCache.read(appContext))

    /** null until the saved choice has been read, so neither UI flashes before the right one. */
    val playerType: StateFlow<com.tasnimulhasan.entity.enums.PlayerType?> = getPlayerTypeUseCase()
        .map<com.tasnimulhasan.entity.enums.PlayerType, com.tasnimulhasan.entity.enums.PlayerType?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setPlayerType(type: com.tasnimulhasan.entity.enums.PlayerType) {
        viewModelScope.launch { setPlayerTypeUseCase(type) }
    }

    private val accentColorOption: StateFlow<AccentColorOption> = getAccentColorUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColorOption.DYNAMIC)

    val useDynamicColor: StateFlow<Boolean> = accentColorOption
        .map { it == AccentColorOption.DYNAMIC }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Recomputed (Palette extraction) only when the accent mode is ALBUM_ART AND the
    // currently playing song actually changes - never runs for any other accent mode, and
    // never re-runs on every progress tick since only songId is compared.
    init {
        AppIconManager.refresh(appContext)
    }

    /** Runs a launcher shortcut; true if playback started (so the caller can open the player). */
    suspend fun runShortcut(action: String): Boolean = withContext(Dispatchers.Main) {
        AppShortcuts.execute(action, playerUseCases, observeFavouriteIdsUseCase)
    }

    private val customPalette: StateFlow<com.tasnimulhasan.entity.enums.PaletteChoice> = getCustomPaletteUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.tasnimulhasan.entity.enums.PaletteChoice())

    /** Non-null only while the accent is CUSTOM; tells the theme which scheme style to expand the seed with. */
    val paletteStyle: StateFlow<com.tasnimulhasan.entity.enums.PaletteStyle?> = combine(accentColorOption, customPalette) { o, p ->
        if (o == AccentColorOption.CUSTOM) p.style else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The typeface chosen in Settings; applied to the whole app by MelodiqTheme. */
    val appFont: StateFlow<com.tasnimulhasan.entity.enums.AppFont> = getAppFontUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.tasnimulhasan.entity.enums.AppFont.DEFAULT)

    /** Show the tab name next to the selected icon in the bottom bar. */
    val showNavLabels: StateFlow<Boolean> = getNavLabelsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val accentSeedColor: StateFlow<Color?> = combine(
        accentColorOption,
        playerUseCases.observeCurrentSelectedAudio(),
        AppIconManager.selected,
        customPalette,
    ) { option, song, icon, custom -> AccentInputs(option, song, icon, custom) }
        .distinctUntilChanged { old, new ->
            old.option == new.option && old.song?.songId == new.song?.songId &&
                old.icon == new.icon && old.custom == new.custom
        }
        .map { (option, song, icon, custom) ->
            when (option) {
                AccentColorOption.CUSTOM -> Color(custom.seed)
                AccentColorOption.APP_ICON ->
                    AppIconManager.accentColor(appContext, icon ?: com.tasnimulhasan.settings.AppIconOption.DEFAULT)
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

    /**
     * The very first library scan runs when the app process starts - before the user has
     * answered the permission dialog - so it finds nothing and is never retried. That left
     * a black screen / endless loader after the first grant. Scanning again right after the
     * permission is granted fills Room, and every list updates by itself from there.
     */
    fun onStoragePermissionGranted() {
        viewModelScope.launch { runCatching { scanLibraryUseCase(force = true) } }
    }
}

private data class AccentInputs<S, I>(
    val option: AccentColorOption,
    val song: S,
    val icon: I,
    val custom: com.tasnimulhasan.entity.enums.PaletteChoice,
)
