package com.tasnimulhasan.data.repoimpl

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
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

    override suspend fun saveSortType(type: SortType) {
        tryIt {
            dataStorePreferences.edit { preferences ->
                preferences[PreferencesKeys.sortType] = type.name
            }
        }
    }

    override fun getSortType(): Flow<SortType> {
        return dataStorePreferences.data.map { preferences ->
            val sortTypeName = preferences[PreferencesKeys.sortType]
            SortType.entries.find { it.name == sortTypeName } ?: SortType.DATE_MODIFIED_DESC
        }
    }

    private object PreferencesKeys {
        val eqType = stringPreferencesKey(name = "eq_type")
        val enableEqualizer = booleanPreferencesKey(name = "enable_equalizer")
        val sortType = stringPreferencesKey("sort_type")
    }
}