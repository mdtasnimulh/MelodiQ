package com.tasnimulhasan.domain.localusecase.music

import com.tasnimulhasan.common.service.MelodiqPlayerEvent
import com.tasnimulhasan.common.service.MelodiqServiceHandler
import javax.inject.Inject

class UpdateProgressUseCase @Inject constructor(
    private val serviceHandler: MelodiqServiceHandler
) {
    suspend operator fun invoke(progress: Float) {
        serviceHandler.onPlayerEvents(
            MelodiqPlayerEvent.UpdateProgress(progress)
        )
    }
}