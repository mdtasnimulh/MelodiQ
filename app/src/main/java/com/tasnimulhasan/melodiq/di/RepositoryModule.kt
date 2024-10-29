package com.tasnimulhasan.melodiq.di

import com.tasnimulhasan.data.repoimpl.local.IncomeExpenseRepoImpl
import com.tasnimulhasan.domain.repository.local.IncomeExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindIncomeExpenseRepository(incomeExpenseRepoImpl: IncomeExpenseRepoImpl): IncomeExpenseRepository
}