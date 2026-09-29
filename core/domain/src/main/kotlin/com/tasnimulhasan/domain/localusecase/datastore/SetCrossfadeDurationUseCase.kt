package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import javax.inject.Inject

class SetCrossfadeDurationUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(durationMs: Long) = repository.saveCrossfadeDurationMs(durationMs)
}
