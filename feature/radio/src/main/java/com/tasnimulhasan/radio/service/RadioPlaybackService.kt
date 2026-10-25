package com.tasnimulhasan.radio.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tasnimulhasan.designsystem.R as Res
import com.tasnimulhasan.radio.R
import com.tasnimulhasan.radio.data.RadioController
import com.tasnimulhasan.radio.domain.RadioStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground media service for the radio.
 *
 * It exists so that listening continues with the screen off or the app in the background, and so
 * the radio gets a media notification, lock-screen controls and headset-button handling. The
 * MediaSession wraps a [RadioSessionPlayer] (a view of [RadioController]), which is why the very same
 * notification works for the internet stream and for the FM chip.
 *
 * Audio focus is NOT handled here: ExoPlayer requests it for the stream and HardwareRadioTuner
 * requests it for the chip. The service only owns the process-lifetime and notification side.
 *
 * Declared in this module's AndroidManifest with foregroundServiceType="mediaPlayback"; the
 * FOREGROUND_SERVICE and FOREGROUND_SERVICE_MEDIA_PLAYBACK permissions are required (also declared
 * there). It is started with startForegroundService() by RadioController when playback begins, and
 * stops itself when the radio goes idle.
 */
@AndroidEntryPoint
class RadioPlaybackService : MediaSessionService() {

    @Inject lateinit var controller: RadioController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionPlayer: RadioSessionPlayer? = null
    private var session: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // startForegroundService() obliges the service to call startForeground() within a few
        // seconds, so a placeholder goes up immediately. Media3 then replaces it: it is pointed at
        // the same notification id and channel below.
        createChannel()
        runCatching {
            startForeground(
                NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(Res.drawable.ic_stat_music)
                    .setContentTitle(getString(R.string.radio_notification_title))
                    .setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(NOTIFICATION_ID)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.radio_notification_channel)
                .build()
                .also { it.setSmallIcon(Res.drawable.ic_stat_music) }
        )

        val player = RadioSessionPlayer(controller).also { sessionPlayer = it }
        val newSession = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .build()
            .also { session = it }
        // Nothing binds a MediaController to this service, so the session is registered by hand.
        addSession(newSession)

        scope.launch {
            var wasActive = false
            controller.state.collect { state ->
                player.refresh()
                val active = state.status != RadioStatus.IDLE
                // Once the radio has been running and goes idle (stopped, failed, unplugged), shut down.
                if (wasActive && !active) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                wasActive = wasActive || active
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Radio keeps playing when the app is swiped away; only an idle service is cleaned up.
        if (controller.state.value.status == RadioStatus.IDLE) {
            stopSelf()
            super.onTaskRemoved(rootIntent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        session?.release()
        session = null
        sessionPlayer = null
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.radio_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
        )
    }

    private companion object {
        const val SESSION_ID = "melodiq_radio_session"
        const val CHANNEL_ID = "melodiq_radio_channel"
        const val NOTIFICATION_ID = 4108
    }
}
