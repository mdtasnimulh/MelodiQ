package com.tasnimulhasan.melodiq.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tasnimulhasan.common.notification.MelodiqNotificationManager
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import com.tasnimulhasan.common.utils.CoroutinesDispatchers
import com.tasnimulhasan.domain.localusecase.datastore.GetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.GetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqualizerEnabledUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetSortTypeUseCase
import com.tasnimulhasan.domain.localusecase.player.BackwardTrackUseCase
import com.tasnimulhasan.domain.localusecase.player.ForwardTrackUseCase
import com.tasnimulhasan.domain.localusecase.player.GetCurrentDurationUseCase
import com.tasnimulhasan.domain.localusecase.player.GetCurrentSongInfoUseCase
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
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ) : ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setTrackSelector(DefaultTrackSelector(context))
        .build()

    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ) : MediaSession = MediaSession.Builder(context, player).build()

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ) : MelodiqNotificationManager = MelodiqNotificationManager(
        context = context,
        exoPlayer = player
    )

    @Provides
    @Singleton
    fun provideServiceHandler(exoPlayer: ExoPlayer) : MelodiqServiceHandler =
        MelodiqServiceHandler(exoPlayer = exoPlayer)

    @Provides
    @Singleton
    fun providePlayerUseCases(
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
        repeatTrackOneUseCase: RepeatTrackOneUseCase,
        repeatTrackAllUseCase: RepeatTrackAllUseCase,
        repeatTrackOffUseCase: RepeatTrackOffUseCase
    ): PlayerUseCases {
        return PlayerUseCases(
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
            forwardTrackUseCase = forward,
            backwardTrackUseCase = backward,
            repeatTrackOneUseCase = repeatTrackOneUseCase,
            repeatTrackAllUseCase = repeatTrackAllUseCase,
            repeatTrackOffUseCase = repeatTrackOffUseCase
        )
    }

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