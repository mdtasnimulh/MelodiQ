package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.CoverArtStyle
import javax.inject.Inject

class SetCoverArtStyleUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    suspend operator fun invoke(style: CoverArtStyle) = repository.saveCoverArtStyle(style)
}
