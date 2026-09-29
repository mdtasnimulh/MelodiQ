package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.AccentColorOption
import javax.inject.Inject

class SetAccentColorUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(option: AccentColorOption) = repository.saveAccentColor(option)
}
