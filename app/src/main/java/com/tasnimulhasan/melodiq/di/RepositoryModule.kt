package com.tasnimulhasan.melodiq.di

import com.tasnimulhasan.data.repoimpl.MusicRepoImpl
import com.tasnimulhasan.data.repoimpl.PreferencesDataStoreRepoImpl
import com.tasnimulhasan.data.repoimpl.local.MelodiQRepoImpl
import com.tasnimulhasan.data.repoimpl.local.PlaylistDetailsRepoImpl
import com.tasnimulhasan.data.repoimpl.local.PlaylistRepoImpl
import com.tasnimulhasan.data.repoimpl.player.PlayerRepositoryImpl
import com.tasnimulhasan.domain.repository.MusicRepository
import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.domain.repository.local.PlaylistDetailsRepository
import com.tasnimulhasan.domain.repository.local.PlaylistsRepository
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

    @Binds
    fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    fun bindPreferencesDataStoreRepository(impl: PreferencesDataStoreRepoImpl): PreferencesDataStoreRepository

    @Binds
    fun bindPlaylistsRepository(impl: PlaylistRepoImpl): PlaylistsRepository

    @Binds
    fun bindPlaylistDetailsRepository(impl: PlaylistDetailsRepoImpl): PlaylistDetailsRepository
}