package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.MiniPlayerPosition
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMiniPlayerPositionUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<MiniPlayerPosition> = repository.getMiniPlayerPosition()
}

class SetMiniPlayerPositionUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(position: MiniPlayerPosition) = repository.saveMiniPlayerPosition(position)
}
