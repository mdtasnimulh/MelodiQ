package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import javax.inject.Inject

class SetEqTypeUseCase @Inject constructor(
    private val preferencesDataStoreRepository: PreferencesDataStoreRepository
) {
    suspend fun invoke(eqType: AudioEffects) =
        preferencesDataStoreRepository.setEqType(eqType = eqType)
}