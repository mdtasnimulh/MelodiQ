package com.tasnimulhasan.domain.localusecase.player

data class PlayerUseCases(
    val play: PlayUseCase,
    val pause: PauseUseCase,
    val next: NextTrackUseCase,
    val previous: PreviousTrackUseCase,
    val seekTo: SeekToUseCase,
    val getCurrentDuration: GetCurrentDurationUseCase,
    val selectAudioChange: SelectAudioChangeUseCase,
    val updateProgress: UpdateProgressUseCase,
    val observeAudioState: ObserveAudioStateUseCase
)