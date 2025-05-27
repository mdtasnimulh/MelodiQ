package com.tasnimulhasan.common.service

import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tasnimulhasan.common.notification.MelodiqNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MelodiqPlayerService : MediaSessionService() {

    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var notificationManager: MelodiqNotificationManager

    private var loudnessEnhancer: LoudnessEnhancer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager.startNotificationService(
            mediaSessionService = this,
            mediaSession = mediaSession
        )
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    override fun onDestroy() {
        mediaSession.release()
        releaseVolumeBoost()
        super.onDestroy()
    }

    fun releaseVolumeBoost() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}