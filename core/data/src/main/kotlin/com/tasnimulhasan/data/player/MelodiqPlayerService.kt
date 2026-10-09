package com.tasnimulhasan.data.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tasnimulhasan.common.constant.AppConstants
import com.tasnimulhasan.common.notification.MelodiqNotificationManager
import com.tasnimulhasan.designsystem.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MelodiqPlayerService : MediaSessionService() {

    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var notificationManager: MelodiqNotificationManager

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // MUST call startForeground() here, synchronously (see the OS timeout for
        // startForegroundService); Media3 swaps in the real notification right after.
        notificationManager.startForegroundPlaceholder(this)

        // Media3 builds the playback notification itself. Pointing it at our channel/id (the
        // same ones as the placeholder) makes it replace the placeholder, and gives it our
        // small icon instead of its generic default.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(AppConstants.NOTIFICATION_ID)
                .setChannelId(AppConstants.NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.app_name)
                .build()
                .also { it.setSmallIcon(R.drawable.ic_stat_music) }
        )
        // This service is started directly (nothing binds a MediaController to it), so the
        // session has to be registered by hand or Media3 never posts a notification for it.
        addSession(mediaSession)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE) {
            // "Close player": drop out of the foreground and remove the notification, but
            // keep the service object alive - destroying it from here crashed/closed the app.
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            // Nothing playing: don't leave a stale paused notification behind.
            stopSelf()
            super.onTaskRemoved(rootIntent)
        }
        // Music IS playing: deliberately do NOT call super, which may stop the service when
        // the task is swiped away.
    }

    // The MediaSession is a process-wide singleton shared with the player, so it is not
    // released here.

    companion object {
        const val ACTION_CLOSE = "com.tasnimulhasan.melodiq.action.CLOSE_PLAYER"
    }
}
