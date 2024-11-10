package com.tasnimulhasan.melodiq.di

import com.tasnimulhasan.data.repoimpl.MusicRepoImpl
import com.tasnimulhasan.data.repoimpl.local.MelodiQRepoImpl
import com.tasnimulhasan.domain.repository.MusicRepository
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindMelodiQRepository(incomeExpenseRepoImpl: MelodiQRepoImpl): MelodiQRepository

    @Binds
    fun bindMusicRepository(musicRepoImpl: MusicRepoImpl): MusicRepository
}