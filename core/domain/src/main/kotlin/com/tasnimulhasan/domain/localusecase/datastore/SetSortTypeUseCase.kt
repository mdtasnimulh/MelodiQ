package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import javax.inject.Inject

class SetSortTypeUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(type: SortType) = repository.saveSortType(type)
}
