package com.tasnimulhasan.domain.localusecase.datastore

import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.entity.enums.CoverArtStyle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCoverArtStyleUseCase @Inject constructor(
    private val repository: PreferencesDataStoreRepository
) {
    operator fun invoke(): Flow<CoverArtStyle> = repository.getCoverArtStyle()
}
