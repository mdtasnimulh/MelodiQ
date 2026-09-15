package com.tasnimulhasan.domain.repository

import com.tasnimulhasan.entity.AppConfiguration
import com.tasnimulhasan.entity.enums.DarkThemeConfig
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

    suspend fun saveLastPlayedTrack(songId: Long, positionMs: Long)
    suspend fun getLastPlayedTrack(): LastPlayedTrack?
}