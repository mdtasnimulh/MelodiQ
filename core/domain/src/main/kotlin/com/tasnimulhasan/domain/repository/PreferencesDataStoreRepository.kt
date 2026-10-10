package com.tasnimulhasan.domain.repository

import com.tasnimulhasan.entity.AppConfiguration
import com.tasnimulhasan.entity.enums.AccentColorOption
import com.tasnimulhasan.entity.enums.CoverArtStyle
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.entity.enums.VisualizerStyle
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import com.tasnimulhasan.entity.player.LastPlayedTrack
import kotlinx.coroutines.flow.Flow

interface PreferencesDataStoreRepository {
    suspend fun setEqType(eqType: AudioEffects)
    suspend fun setEqualizerEnabled(enabled: Boolean)
    val appConfigurationStream: Flow<AppConfiguration>

    suspend fun saveSortType(type: SortType)
    fun getSortType(): Flow<SortType>

    suspend fun saveThemeConfig(config: DarkThemeConfig)
    fun getThemeConfig(): Flow<DarkThemeConfig>

    suspend fun saveAccentColor(option: AccentColorOption)
    fun getAccentColor(): Flow<AccentColorOption>

    suspend fun saveReplayGainEnabled(enabled: Boolean)
    fun getReplayGainEnabled(): Flow<Boolean>

    suspend fun saveCrossfadeEnabled(enabled: Boolean)
    fun getCrossfadeEnabled(): Flow<Boolean>

    suspend fun saveCrossfadeDurationMs(durationMs: Long)
    fun getCrossfadeDurationMs(): Flow<Long>

    suspend fun saveVisualizerStyle(style: VisualizerStyle)
    fun getVisualizerStyle(): Flow<VisualizerStyle>

    suspend fun saveCoverArtStyle(style: CoverArtStyle)
    fun getCoverArtStyle(): Flow<CoverArtStyle>

    suspend fun saveLastPlayedTrack(songId: Long, positionMs: Long)
    suspend fun getLastPlayedTrack(): LastPlayedTrack?
    suspend fun clearLastPlayedTrack()

    /** Every saved setting except playback-session state, as name -> "type:value". */
    suspend fun exportSettings(): Map<String, String>
    /** Writes settings produced by [exportSettings]; returns how many were applied. */
    suspend fun importSettings(settings: Map<String, String>): Int

    suspend fun saveMiniPlayerPosition(position: com.tasnimulhasan.entity.enums.MiniPlayerPosition)
    fun getMiniPlayerPosition(): Flow<com.tasnimulhasan.entity.enums.MiniPlayerPosition>
}