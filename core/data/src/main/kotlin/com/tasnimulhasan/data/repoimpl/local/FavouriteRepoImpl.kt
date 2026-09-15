package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.database.dao.FavouriteDao
import com.tasnimulhasan.domain.repository.local.FavouriteRepository
import com.tasnimulhasan.entity.room.favourite.FavouriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavouriteRepoImpl @Inject constructor(
    private val favouriteDao: FavouriteDao
) : FavouriteRepository {

    override fun observeFavouriteIds(): Flow<Set<Long>> =
        favouriteDao.getFavouriteIds().map { it.toSet() }

    override suspend fun toggleFavourite(songId: Long) {
        if (favouriteDao.isFavourite(songId)) {
            favouriteDao.removeFavourite(songId)
        } else {
            favouriteDao.addFavourite(FavouriteEntity(songId = songId))
        }
    }
}
