package com.tasnimulhasan.database.di

import com.tasnimulhasan.database.MelodiQDatabase
import com.tasnimulhasan.database.dao.FavouriteDao
import com.tasnimulhasan.database.dao.LibrarySongDao
import com.tasnimulhasan.database.dao.MelodiQDao
import com.tasnimulhasan.database.dao.PlayHistoryDao
import com.tasnimulhasan.database.dao.PlaylistDetailsDao
import com.tasnimulhasan.database.dao.PlaylistsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {

    @Provides
    fun providesIncomeExpenseDao(
        database: MelodiQDatabase
    ): MelodiQDao = database.melodiQDao()

    @Provides
    fun providesPlaylistDao(
        database: MelodiQDatabase
    ): PlaylistsDao = database.playlistsDao()

    @Provides
    fun providesPlaylistDetailsDao(
        database: MelodiQDatabase
    ): PlaylistDetailsDao = database.playlistDetailsDao()

    @Provides
    fun providesFavouriteDao(
        database: MelodiQDatabase
    ): FavouriteDao = database.favouriteDao()

    @Provides
    fun providesLibrarySongDao(
        database: MelodiQDatabase
    ): LibrarySongDao = database.librarySongDao()

    @Provides
    fun providesPlayHistoryDao(
        database: MelodiQDatabase
    ): PlayHistoryDao = database.playHistoryDao()

}