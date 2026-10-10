package com.tasnimulhasan.melodiq.di

import com.tasnimulhasan.data.repoimpl.MusicRepoImpl
import com.tasnimulhasan.data.repoimpl.PreferencesDataStoreRepoImpl
import com.tasnimulhasan.data.repoimpl.local.FavouriteRepoImpl
import com.tasnimulhasan.data.repoimpl.local.FileManagementRepoImpl
import com.tasnimulhasan.data.repoimpl.local.LibraryRepoImpl
import com.tasnimulhasan.data.repoimpl.local.LyricsRepoImpl
import com.tasnimulhasan.data.repoimpl.local.MelodiQRepoImpl
import com.tasnimulhasan.data.repoimpl.local.MetadataRepoImpl
import com.tasnimulhasan.data.repoimpl.local.PlaylistDetailsRepoImpl
import com.tasnimulhasan.data.repoimpl.local.PlaylistRepoImpl
import com.tasnimulhasan.data.repoimpl.player.PlayerRepositoryImpl
import com.tasnimulhasan.domain.lyrics.LyricsProvider
import com.tasnimulhasan.data.lyrics.LrcLibLyricsProvider
import com.tasnimulhasan.data.lyrics.MlKitLyricsTranslator
import com.tasnimulhasan.domain.lyrics.LyricsTranslator
import com.tasnimulhasan.domain.repository.MusicRepository
import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import com.tasnimulhasan.domain.repository.local.FavouriteRepository
import com.tasnimulhasan.domain.repository.local.FileManagementRepository
import com.tasnimulhasan.domain.repository.local.LibraryRepository
import com.tasnimulhasan.domain.repository.local.LyricsRepository
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.domain.repository.local.MetadataRepository
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

    @Binds
    fun bindFavouriteRepository(impl: FavouriteRepoImpl): FavouriteRepository

    @Binds
    fun bindLibraryRepository(impl: LibraryRepoImpl): LibraryRepository

    @Binds
    fun bindBackupRepository(impl: com.tasnimulhasan.data.repoimpl.local.BackupRepoImpl): com.tasnimulhasan.domain.repository.local.BackupRepository

    @Binds
    fun bindLyricsRepository(impl: LyricsRepoImpl): LyricsRepository

    @Binds
    fun bindLyricsProvider(impl: LrcLibLyricsProvider): LyricsProvider

    @Binds
    fun bindMetadataRepository(impl: MetadataRepoImpl): MetadataRepository

    @Binds
    fun bindFileManagementRepository(impl: FileManagementRepoImpl): FileManagementRepository

    @Binds
    fun bindLyricsTranslator(impl: MlKitLyricsTranslator): LyricsTranslator
}