package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeConfigUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<DarkThemeConfig> = repository.getThemeConfig()
}
