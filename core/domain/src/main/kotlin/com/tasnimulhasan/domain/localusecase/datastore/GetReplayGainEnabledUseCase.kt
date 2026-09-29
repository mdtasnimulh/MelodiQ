package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReplayGainEnabledUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.getReplayGainEnabled()
}
