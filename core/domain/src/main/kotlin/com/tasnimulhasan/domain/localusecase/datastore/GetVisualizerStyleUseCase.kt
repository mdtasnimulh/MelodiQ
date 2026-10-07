package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.VisualizerStyle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVisualizerStyleUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<VisualizerStyle> = repository.getVisualizerStyle()
}
