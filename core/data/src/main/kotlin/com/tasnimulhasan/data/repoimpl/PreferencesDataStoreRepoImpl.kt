package com.tasnimulhasan.data.repoimpl

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.tasnimulhasan.entity.player.LastPlayedTrack
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.AppConfiguration
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.tasnimulhasan.common.constant.AppConstants.FLAT
import com.tasnimulhasan.common.constant.AppConstants.PRESET_FLAT
import com.tasnimulhasan.entity.enums.SortType
import kotlinx.coroutines.flow.distinctUntilChanged
import com.tasnimulhasan.entity.enums.AccentColorOption
import com.tasnimulhasan.entity.enums.CoverArtStyle
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.entity.enums.VisualizerStyle

class PreferencesDataStoreRepoImpl @Inject constructor(
    private val gson: Gson,
    private val dataStorePreferences: DataStore<Preferences>
) : PreferencesDataStoreRepository {
    private val tag = this::class.java.simpleName

    private suspend fun tryIt(action: suspend () -> Unit) {
        try {
            action()
        } catch (exception: Exception) {
            exception.localizedMessage?.let { Log.e(tag, it) }
        }
    }

    override suspend fun setEqType(eqType: AudioEffects) {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences[PreferencesKeys.eqType] = gson.toJson(eqType)
            }
        }
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences[PreferencesKeys.enableEqualizer] = enabled
            }
        }
    }

    override val appConfigurationStream: Flow<AppConfiguration> = dataStorePreferences.data
        .catch { exception ->
            exception.localizedMessage?.let { Log.e(tag, it) }
            emit(emptyPreferences())
        }
        .map { preferences ->
            val audioEffectsJson = preferences[PreferencesKeys.eqType] ?: ""
            val audioEffects = if (audioEffectsJson.isNotEmpty()) {
                try {
                    gson.fromJson(audioEffectsJson, AudioEffects::class.java)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to deserialize AudioEffects: ${e.localizedMessage}")
                    AudioEffects(PRESET_FLAT, FLAT)
                }
            } else {
                AudioEffects(PRESET_FLAT, FLAT)
            }
            val enableEqualizer = preferences[PreferencesKeys.enableEqualizer] ?: false
            AppConfiguration(audioEffects = audioEffects, enableEqualizer = enableEqualizer)
        }
        .distinctUntilChanged()

    override suspend fun saveSortType(type: SortType) {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences[PreferencesKeys.sortType] = type.name
            }
        }
    }

    override fun getSortType(): Flow<SortType> {
        return dataStorePreferences.data
            .map { preferences ->
                val sortTypeName = preferences[PreferencesKeys.sortType]
                SortType.entries.find { it.name == sortTypeName } ?: SortType.DATE_MODIFIED_DESC
            }
            .distinctUntilChanged()
    }


    override suspend fun saveThemeConfig(config: DarkThemeConfig) {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences[PreferencesKeys.themeConfig] = config.name
            }
        }
    }

    override fun getThemeConfig(): Flow<DarkThemeConfig> {
        return dataStorePreferences.data
            .map { preferences ->
                val name = preferences[PreferencesKeys.themeConfig]
                DarkThemeConfig.entries.find { it.name == name } ?: DarkThemeConfig.FOLLOW_SYSTEM
            }
            .distinctUntilChanged()
    }

    override suspend fun saveAccentColor(option: AccentColorOption) {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences[PreferencesKeys.accentColor] = option.name
            }
        }
    }

    override fun getAccentColor(): Flow<AccentColorOption> {
        return dataStorePreferences.data
            .map { preferences ->
                val name = preferences[PreferencesKeys.accentColor]
                AccentColorOption.entries.find { it.name == name } ?: AccentColorOption.DYNAMIC
            }
            .distinctUntilChanged()
    }

    override suspend fun saveReplayGainEnabled(enabled: Boolean) {
        tryIt {
            dataStorePreferences.edit { preferences -> preferences[PreferencesKeys.replayGainEnabled] = enabled }
        }
    }

    override fun getReplayGainEnabled(): Flow<Boolean> {
        return dataStorePreferences.data
            .map { preferences -> preferences[PreferencesKeys.replayGainEnabled] ?: false }
            .distinctUntilChanged()
    }

    override suspend fun saveCrossfadeEnabled(enabled: Boolean) {
        tryIt {
            dataStorePreferences.edit { preferences -> preferences[PreferencesKeys.crossfadeEnabled] = enabled }
        }
    }

    override fun getCrossfadeEnabled(): Flow<Boolean> {
        return dataStorePreferences.data
            .map { preferences -> preferences[PreferencesKeys.crossfadeEnabled] ?: false }
            .distinctUntilChanged()
    }

    override suspend fun saveCrossfadeDurationMs(durationMs: Long) {
        tryIt {
            dataStorePreferences.edit { preferences -> preferences[PreferencesKeys.crossfadeDurationMs] = durationMs }
        }
    }

    override fun getCrossfadeDurationMs(): Flow<Long> {
        return dataStorePreferences.data
            .map { preferences -> preferences[PreferencesKeys.crossfadeDurationMs] ?: 4_000L }
            .distinctUntilChanged()
    }

    override suspend fun saveVisualizerStyle(style: VisualizerStyle) {
        tryIt {
            dataStorePreferences.edit { preferences -> preferences[PreferencesKeys.visualizerStyle] = style.name }
        }
    }

    override fun getVisualizerStyle(): Flow<VisualizerStyle> {
        return dataStorePreferences.data
            .map { preferences ->
                val name = preferences[PreferencesKeys.visualizerStyle]
                VisualizerStyle.entries.find { it.name == name } ?: VisualizerStyle.OFF
            }
            .distinctUntilChanged()
    }

    override suspend fun saveCoverArtStyle(style: CoverArtStyle) {
        tryIt {
            dataStorePreferences.edit { preferences -> preferences[PreferencesKeys.coverArtStyle] = style.name }
        }
    }

    override fun getCoverArtStyle(): Flow<CoverArtStyle> {
        return dataStorePreferences.data
            .map { preferences ->
                val name = preferences[PreferencesKeys.coverArtStyle]
                CoverArtStyle.entries.find { it.name == name } ?: CoverArtStyle.HALF
            }
            .distinctUntilChanged()
    }

    override suspend fun saveLastPlayedTrack(songId: Long, positionMs: Long) {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences[PreferencesKeys.lastPlayedSongId] = songId
                preferences[PreferencesKeys.lastPlayedPositionMs] = positionMs
            }
        }
    }

    override suspend fun getLastPlayedTrack(): LastPlayedTrack? {
        return try {
            val preferences = dataStorePreferences.data.first()
            val songId = preferences[PreferencesKeys.lastPlayedSongId] ?: return null
            val position = preferences[PreferencesKeys.lastPlayedPositionMs] ?: 0L
            LastPlayedTrack(songId = songId, positionMs = position)
        } catch (exception: Exception) {
            exception.localizedMessage?.let { Log.e(tag, it) }
            null
        }
    }

    override suspend fun clearLastPlayedTrack() {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences.remove(PreferencesKeys.lastPlayedSongId)
                preferences.remove(PreferencesKeys.lastPlayedPositionMs)
            }
        }
    }

    override suspend fun saveMiniPlayerPosition(position: com.tasnimulhasan.entity.enums.MiniPlayerPosition) {
        tryIt {
            dataStorePreferences.edit { preferences -> preferences[PreferencesKeys.miniPlayerPosition] = position.name }
        }
    }

    override fun getMiniPlayerPosition(): Flow<com.tasnimulhasan.entity.enums.MiniPlayerPosition> {
        return dataStorePreferences.data
            .map { preferences ->
                val name = preferences[PreferencesKeys.miniPlayerPosition]
                com.tasnimulhasan.entity.enums.MiniPlayerPosition.entries.find { it.name == name }
                    ?: com.tasnimulhasan.entity.enums.MiniPlayerPosition.BOTTOM_END
            }
            .distinctUntilChanged()
    }

    private object PreferencesKeys {
        val miniPlayerPosition = stringPreferencesKey("mini_player_position")
        val eqType = stringPreferencesKey(name = "eq_type")
        val enableEqualizer = booleanPreferencesKey(name = "enable_equalizer")
        val sortType = stringPreferencesKey("sort_type")
        val themeConfig = stringPreferencesKey("theme_config")
        val accentColor = stringPreferencesKey("accent_color")
        val replayGainEnabled = booleanPreferencesKey("replaygain_enabled")
        val crossfadeEnabled = booleanPreferencesKey("crossfade_enabled")
        val crossfadeDurationMs = longPreferencesKey("crossfade_duration_ms")
        val visualizerStyle = stringPreferencesKey("visualizer_style")
        val coverArtStyle = stringPreferencesKey("cover_art_style")
        val lastPlayedSongId = longPreferencesKey("last_played_song_id")
        val lastPlayedPositionMs = longPreferencesKey("last_played_position_ms")
    }
}