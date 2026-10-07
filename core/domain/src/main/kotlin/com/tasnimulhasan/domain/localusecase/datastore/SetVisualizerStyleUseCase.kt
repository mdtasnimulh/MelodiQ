package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.VisualizerStyle
import javax.inject.Inject

class SetVisualizerStyleUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(style: VisualizerStyle) = repository.saveVisualizerStyle(style)
}
