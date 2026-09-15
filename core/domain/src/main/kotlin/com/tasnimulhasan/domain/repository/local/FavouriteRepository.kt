package com.tasnimulhasan.domain.repository.local

import kotlinx.coroutines.flow.Flow

interface FavouriteRepository {
    fun observeFavouriteIds(): Flow<Set<Long>>
    suspend fun toggleFavourite(songId: Long)
}
