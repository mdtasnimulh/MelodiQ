package com.tasnimulhasan.database.di

import com.tasnimulhasan.database.MelodiQDatabase
import com.tasnimulhasan.database.dao.MelodiQDao
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

}