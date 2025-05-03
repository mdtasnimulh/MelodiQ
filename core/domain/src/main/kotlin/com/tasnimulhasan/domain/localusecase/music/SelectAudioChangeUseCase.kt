package com.tasnimulhasan.domain.localusecase.music

import com.tasnimulhasan.common.service.MelodiqPlayerEvent
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import javax.inject.Inject

class SelectAudioChangeUseCase @Inject constructor(
    private val serviceHandler: MelodiqServiceHandler
) {
    suspend operator fun invoke(index: Int) {
        serviceHandler.onPlayerEvents(
            MelodiqPlayerEvent.SelectAudioChange,
            selectedAudionIndex = index
        )
    }
}