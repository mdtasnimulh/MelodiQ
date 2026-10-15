package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.PlayerType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayerTypeUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<PlayerType> = repository.getPlayerType()
}

class SetPlayerTypeUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(type: PlayerType) = repository.savePlayerType(type)
}
