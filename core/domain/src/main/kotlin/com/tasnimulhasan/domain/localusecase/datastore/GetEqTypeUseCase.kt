package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.AppConfiguration
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEqTypeUseCase @Inject constructor(
    private val preferencesDataStoreRepository: PreferencesDataStoreRepository
) {
    fun invoke(): Flow<AppConfiguration> = preferencesDataStoreRepository.appConfigurationStream
}