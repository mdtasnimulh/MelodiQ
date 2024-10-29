package com.tasnimulhasan.database.di

import com.tasnimulhasan.database.MmDatabase
import com.tasnimulhasan.database.dao.IncomeExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {

    @Provides
    fun providesIncomeExpenseDao(
        database: MmDatabase
    ): IncomeExpenseDao = database.incomeExpenseDao()

}