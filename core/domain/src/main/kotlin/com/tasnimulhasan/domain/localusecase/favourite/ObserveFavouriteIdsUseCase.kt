package com.tasnimulhasan.domain.localusecase.favourite

import com.tasnimulhasan.domain.repository.local.FavouriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavouriteIdsUseCase @Inject constructor(
    private val repository: FavouriteRepository
) {
    operator fun invoke(): Flow<Set<Long>> = repository.observeFavouriteIds()
}
