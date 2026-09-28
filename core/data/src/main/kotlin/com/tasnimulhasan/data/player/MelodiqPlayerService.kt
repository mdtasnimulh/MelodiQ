package com.tasnimulhasan.data.player

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

    override fun onCreate() {
        super.onCreate()
        // MUST call startForeground() here, synchronously, regardless of playback state.
        //
        // ensurePlaybackServiceStarted() calls Context.startForegroundService() the moment
        // the user taps a song - before ExoPlayer has prepared anything, let alone started
        // playing. Android enforces a hard timeout on that call: if startForeground() isn't
        // reached in time, the OS kills the whole process with
        // ForegroundServiceDidNotStartInTimeException, a FATAL crash. Media3's own
        // automatic notification promotion only happens once the player is genuinely
        // playing - which never happens in time on a slow prepare, and never happens at all
        // if playback fails outright (e.g. an unreadable file) - so relying on it alone left
        // a real window where this crashed on every single tap. Calling this immediately,
        // unconditionally, with whatever notification is available right now closes that
        // window; the notification's content is then kept live by the same
        // PlayerNotificationManager for the rest of playback.
        notificationManager.startNotificationService(this, mediaSession)
    }

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
        notificationManager.release()
        mediaSession.release()
        releaseVolumeBoost()
        super.onDestroy()
    }

    fun releaseVolumeBoost() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}
