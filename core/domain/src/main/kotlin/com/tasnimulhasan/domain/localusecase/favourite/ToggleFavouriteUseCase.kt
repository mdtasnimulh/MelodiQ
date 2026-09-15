package com.tasnimulhasan.domain.localusecase.favourite

import com.tasnimulhasan.domain.repository.local.FavouriteRepository
import javax.inject.Inject

class ToggleFavouriteUseCase @Inject constructor(
    private val repository: FavouriteRepository
) {
    suspend operator fun invoke(songId: Long) = repository.toggleFavourite(songId)
}
