package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class CrossfadeSettings(val enabled: Boolean, val durationMs: Long)

class GetCrossfadeSettingsUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<CrossfadeSettings> =
        repository.getCrossfadeEnabled().combine(repository.getCrossfadeDurationMs()) { enabled, duration ->
            CrossfadeSettings(enabled, duration)
        }
}
