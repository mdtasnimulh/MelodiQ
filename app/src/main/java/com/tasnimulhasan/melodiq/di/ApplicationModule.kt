package com.tasnimulhasan.melodiq.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tasnimulhasan.common.utils.CoroutinesDispatchers
import com.tasnimulhasan.domain.localusecase.datastore.GetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqualizerEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.player.BackwardTrackUseCase
import com.tasnimulhasan.domain.localusecase.player.EnsurePlaybackServiceStartedUseCase
import com.tasnimulhasan.domain.localusecase.player.ForwardTrackUseCase
import com.tasnimulhasan.domain.localusecase.player.GetCurrentDurationUseCase
import com.tasnimulhasan.domain.localusecase.player.GetCurrentSongInfoUseCase
import com.tasnimulhasan.domain.localusecase.player.GetPlaybackSnapshotUseCase
import com.tasnimulhasan.domain.localusecase.player.IsPlaybackServiceRunningUseCase
import com.tasnimulhasan.domain.localusecase.player.LoadPlaylistUseCase
import com.tasnimulhasan.domain.localusecase.player.NextTrackUseCase
import com.tasnimulhasan.domain.localusecase.player.ObserveAudioStateUseCase
import com.tasnimulhasan.domain.localusecase.player.PauseUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.localusecase.player.PreviousTrackUseCase
import com.tasnimulhasan.domain.localusecase.player.RepeatTrackAllUseCase
import com.tasnimulhasan.domain.localusecase.player.RepeatTrackOffUseCase
import com.tasnimulhasan.domain.localusecase.player.RepeatTrackOneUseCase
import com.tasnimulhasan.domain.localusecase.player.SeekToUseCase
import com.tasnimulhasan.domain.localusecase.player.SelectAudioChangeUseCase
import com.tasnimulhasan.domain.localusecase.player.UpdateProgressUseCase
import com.tasnimulhasan.domain.repository.PreferencesDataStoreRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun provideDataStorePreferences(
        @ApplicationContext context: Context,
        coroutinesDispatchers: CoroutinesDispatchers
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(context = coroutinesDispatchers.io + SupervisorJob()),
        produceFile = {
            context.preferencesDataStoreFile(name = "user_preferences")
        }
    )

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context) = context

    @Provides
    @Singleton
    fun provideGson(): Gson {
        val gsonBuilder = GsonBuilder()
        return gsonBuilder.create()
    }

    @Provides
    @Singleton
    fun providePlayerUseCases(
        loadPlaylist: LoadPlaylistUseCase,
        play: PlayUseCase,
        pause: PauseUseCase,
        seekTo: SeekToUseCase,
        next: NextTrackUseCase,
        previous: PreviousTrackUseCase,
        forward: ForwardTrackUseCase,
        backward: BackwardTrackUseCase,
        getCurrentDuration: GetCurrentDurationUseCase,
        selectAudioChange: SelectAudioChangeUseCase,
        updateProgress: UpdateProgressUseCase,
        observeAudioState: ObserveAudioStateUseCase,
        getCurrentSongInfoUseCase: GetCurrentSongInfoUseCase,
        getPlaybackSnapshot: GetPlaybackSnapshotUseCase,
        isPlaybackServiceRunning: IsPlaybackServiceRunningUseCase,
        ensurePlaybackServiceStarted: EnsurePlaybackServiceStartedUseCase,
        repeatTrackOneUseCase: RepeatTrackOneUseCase,
        repeatTrackAllUseCase: RepeatTrackAllUseCase,
        repeatTrackOffUseCase: RepeatTrackOffUseCase
    ): PlayerUseCases = PlayerUseCases(
        loadPlaylist = loadPlaylist,
        play = play,
        pause = pause,
        seekTo = seekTo,
        next = next,
        previous = previous,
        getCurrentDuration = getCurrentDuration,
        selectAudioChange = selectAudioChange,
        updateProgress = updateProgress,
        observeAudioState = observeAudioState,
        getCurrentSongInfoUseCase = getCurrentSongInfoUseCase,
        getPlaybackSnapshot = getPlaybackSnapshot,
        isPlaybackServiceRunning = isPlaybackServiceRunning,
        ensurePlaybackServiceStarted = ensurePlaybackServiceStarted,
        forwardTrackUseCase = forward,
        backwardTrackUseCase = backward,
        repeatTrackOneUseCase = repeatTrackOneUseCase,
        repeatTrackAllUseCase = repeatTrackAllUseCase,
        repeatTrackOffUseCase = repeatTrackOffUseCase

    )

    @Provides
    @Singleton
    fun provideSetEqTypeUseCase(
        repository: PreferencesDataStoreRepository
    ): SetEqTypeUseCase = SetEqTypeUseCase(repository)

    @Provides
    @Singleton
    fun provideGetEqTypeUseCase(
        repository: PreferencesDataStoreRepository
    ): GetEqTypeUseCase = GetEqTypeUseCase(repository)

    @Provides
    @Singleton
    fun provideSetEqualizerEnabledUseCase(
        repository: PreferencesDataStoreRepository
    ): SetEqualizerEnabledUseCase = SetEqualizerEnabledUseCase(repository)

    @Provides
    @Singleton
    fun provideSetSortTypeUseCase(
        repository: PreferencesDataStoreRepository
    ): SetSortTypeUseCase = SetSortTypeUseCase(repository)

    @Provides
    @Singleton
    fun provideGetSortTypeUseCase(
        repository: PreferencesDataStoreRepository
    ): GetSortTypeUseCase = GetSortTypeUseCase(repository)
}