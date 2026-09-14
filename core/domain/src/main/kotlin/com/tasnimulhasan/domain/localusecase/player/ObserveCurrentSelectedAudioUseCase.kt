package com.tasnimulhasan.domain.localusecase.player

import com.tasnimulhasan.domain.repository.PlayerRepository
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the single, shared "currently selected song" identity, always derived from the
 * same list that produced the player's current index. This is what the mini player, the
 * full player, and the song list all observe - guaranteeing they show the same song.
 */
class ObserveCurrentSelectedAudioUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    operator fun invoke(): StateFlow<MusicEntity?> = repository.currentSelectedAudio
}
