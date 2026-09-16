package com.tasnimulhasan.domain.localusecase.player

data class PlayerUseCases(
    val loadPlaylist: LoadPlaylistUseCase,
    val playCuratedQueue: PlayCuratedQueueUseCase,
    val play: PlayUseCase,
    val pause: PauseUseCase,
    val next: NextTrackUseCase,
    val previous: PreviousTrackUseCase,
    val forwardTrackUseCase: ForwardTrackUseCase,
    val backwardTrackUseCase: BackwardTrackUseCase,
    val seekTo: SeekToUseCase,
    val getCurrentDuration: GetCurrentDurationUseCase,
    val selectAudioChange: SelectAudioChangeUseCase,
    val updateProgress: UpdateProgressUseCase,
    val observeAudioState: ObserveAudioStateUseCase,
    val observeAudioList: ObserveAudioListUseCase,
    val observeCurrentSelectedAudio: ObserveCurrentSelectedAudioUseCase,
    val observeIsPlaying: ObserveIsPlayingUseCase,
    val getCurrentSongInfoUseCase: GetCurrentSongInfoUseCase,
    val getPlaybackSnapshot: GetPlaybackSnapshotUseCase,
    val isPlaybackServiceRunning: IsPlaybackServiceRunningUseCase,
    val ensurePlaybackServiceStarted: EnsurePlaybackServiceStartedUseCase,
    val repeatTrackOneUseCase: RepeatTrackOneUseCase,
    val repeatTrackAllUseCase: RepeatTrackAllUseCase,
    val repeatTrackOffUseCase: RepeatTrackOffUseCase
)