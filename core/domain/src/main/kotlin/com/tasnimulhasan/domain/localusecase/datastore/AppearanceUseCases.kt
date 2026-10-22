package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.PaletteChoice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCustomPaletteUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<PaletteChoice> = repository.getCustomPalette()
}

class SetCustomPaletteUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(choice: PaletteChoice) = repository.saveCustomPalette(choice)
}

class GetNavLabelsUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.getNavLabels()
}

class SetNavLabelsUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(show: Boolean) = repository.saveNavLabels(show)
}
