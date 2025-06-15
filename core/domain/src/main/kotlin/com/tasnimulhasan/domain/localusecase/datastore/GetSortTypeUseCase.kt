package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSortTypeUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<SortType> = repository.getSortType()
}
