package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import javax.inject.Inject

class SetThemeConfigUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(config: DarkThemeConfig) = repository.saveThemeConfig(config)
}
