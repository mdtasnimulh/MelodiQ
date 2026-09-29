package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import javax.inject.Inject

class SetReplayGainEnabledUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.saveReplayGainEnabled(enabled)
}
