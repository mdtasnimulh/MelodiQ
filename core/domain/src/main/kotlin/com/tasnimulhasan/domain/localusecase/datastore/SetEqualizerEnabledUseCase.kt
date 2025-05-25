package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import javax.inject.Inject

class SetEqualizerEnabledUseCase @Inject constructor(
    private val preferencesDataStoreRepository: PreferencesDataStoreRepository
) {
    suspend fun invoke(enabled: Boolean) =
        preferencesDataStoreRepository.setEqualizerEnabled(enabled)
}