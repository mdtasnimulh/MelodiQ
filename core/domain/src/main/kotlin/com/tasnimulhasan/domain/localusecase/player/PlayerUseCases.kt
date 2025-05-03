package com.tasnimulhasan.domain.localusecase.player

data class PlayerUseCases(
    val play: PlayUseCase,
    val pause: PauseUseCase,
    val seekTo: SeekToUseCase,
    val next: NextTrackUseCase,
    val previous: PreviousTrackUseCase,
    val getCurrentDuration: GetCurrentDurationUseCase,
    val selectAudioChange: SelectAudioChangeUseCase,
    val updateProgress: UpdateProgressUseCase
)