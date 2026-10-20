package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.UiStyle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUiStyleUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<UiStyle> = repository.getUiStyle()
}

class SetUiStyleUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(style: UiStyle) = repository.saveUiStyle(style)
}
