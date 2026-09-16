package com.tasnimulhasan.data.player

import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MelodiqPlayerService : MediaSessionService() {

    @Inject
    lateinit var mediaSession: MediaSession

    private var loudnessEnhancer: LoudnessEnhancer? = null

    // NOTE: this service deliberately does NOT build its own notification any more.
    //
    // It previously created a PlayerNotificationManager AND called
    // startForeground(NOTIFICATION_ID, <empty Notification>) using the same notification id.
    // The blank foreground notification overwrote the real media notification, which is why
    // playback continued in the background with nothing useful shown in the shade (no
    // title, no artwork, no working progress or transport controls).
    //
    // MediaSessionService already publishes and maintains a proper media notification from
    // the MediaSession's metadata via DefaultMediaNotificationProvider, and handles the
    // foreground-service lifecycle itself, including keeping the notification alive while
    // audio plays after the app is swiped away. Letting it do that is both less code and
    // strictly more correct than the hand-rolled version.

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away should not silently orphan a paused session: if nothing is
        // actually playing, tear the service down so no stale notification lingers.
        val player = mediaSession.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

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
