package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.AccentColorOption
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccentColorUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<AccentColorOption> = repository.getAccentColor()
}
