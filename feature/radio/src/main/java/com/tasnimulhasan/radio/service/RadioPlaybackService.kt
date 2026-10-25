package com.tasnimulhasan.radio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.tasnimulhasan.designsystem.R as Res
import com.tasnimulhasan.radio.R
import com.tasnimulhasan.radio.data.RadioController
import com.tasnimulhasan.radio.domain.FmBand
import com.tasnimulhasan.radio.domain.RadioMode
import com.tasnimulhasan.radio.domain.RadioStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for the radio: it keeps playback alive with the screen off or the app in the
 * background, and it owns the radio's notification.
 *
 * The notification is built and updated here (not left to Media3's automatic one), so it always
 * exists while the radio is running and always reflects [RadioController]'s state: station name or
 * FM frequency, the stream title when the station sends one, a Play/Pause button and a Stop button.
 * It uses the media style, so it also appears as the lock-screen / quick-settings media control.
 * While playing it is ongoing (foreground); when paused it can be swiped away, which stops the radio.
 *
 * A [MediaSession] over [RadioSessionPlayer] is kept alive for headset and Bluetooth media buttons.
 * It is deliberately not handed to Media3's service machinery, so Media3 never posts a second
 * notification of its own.
 *
 * Audio focus is not handled here: ExoPlayer requests it for the stream and HardwareRadioTuner
 * requests it for the chip. On Android 13+ the notification is only visible if the user has granted
 * POST_NOTIFICATIONS (the app already asks for it); playback works either way.
 */
@AndroidEntryPoint
class RadioPlaybackService : Service() {

    @Inject lateinit var controller: RadioController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionPlayer: RadioSessionPlayer? = null
    private var session: MediaSession? = null
    private var foreground = false

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.radio_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
        )

        val player = RadioSessionPlayer(controller).also { sessionPlayer = it }
        session = MediaSession.Builder(this, player).setId(SESSION_ID).build()

        // startForegroundService() obliges the service to call startForeground() within seconds.
        enterForeground()

        scope.launch {
            var wasActive = false
            controller.state.collect { state ->
                player.refresh()
                val active = state.status != RadioStatus.IDLE
                if (!active && !wasActive) return@collect
                wasActive = wasActive || active
                when {
                    // Idle again after running (stopped, failed, headphones pulled): shut down.
                    !active -> shutDown()
                    state.status == RadioStatus.PAUSED -> {
                        if (foreground) {
                            stopForeground(STOP_FOREGROUND_DETACH)
                            foreground = false
                        }
                        notificationManager.notify(NOTIFICATION_ID, buildNotification())
                    }
                    else -> enterForeground()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> controller.play()
            ACTION_PAUSE -> controller.pause()
            ACTION_STOP -> controller.stop()
            // Started with startForegroundService(): the foreground call must follow promptly.
            else -> enterForeground()
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Radio keeps playing when the app is swiped away; only an idle service is cleaned up.
        if (controller.state.value.status == RadioStatus.IDLE) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        session?.release()
        session = null
        sessionPlayer = null
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun shutDown() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        foreground = false
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun enterForeground() {
        val notification = buildNotification()
        // Starting a foreground service from the background can be refused; the notification is still shown.
        runCatching {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            foreground = true
        }.onFailure {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(): Notification {
        val s = controller.state.value
        val hardware = s.mode == RadioMode.HARDWARE
        val playing = s.status == RadioStatus.PLAYING || s.status == RadioStatus.BUFFERING

        val title = if (hardware) "FM ${FmBand.mhzLabel(s.frequencyKhz)} MHz" else s.stationName ?: "Internet radio"
        val text = s.nowPlaying ?: when (s.status) {
            RadioStatus.BUFFERING -> "Connecting…"
            RadioStatus.PAUSED -> "Paused"
            else -> if (hardware) "Hardware FM tuner" else "Internet radio"
        }

        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val toggle = if (playing) {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", action(ACTION_PAUSE, 1))
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", action(ACTION_PLAY, 2))
        }
        val stop = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", action(ACTION_STOP, 3))

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(Res.drawable.ic_stat_music)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(if (hardware) "FM radio" else "Internet radio")
            .setContentIntent(openApp)
            .setDeleteIntent(action(ACTION_STOP, 3))
            .addAction(toggle)
            .addAction(stop)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        session?.let {
            builder.setStyle(MediaStyleNotificationHelper.MediaStyle(it).setShowActionsInCompactView(0, 1))
        }
        return builder.build()
    }

    private fun action(name: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code, Intent(this, RadioPlaybackService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val SESSION_ID = "melodiq_radio_session"
        const val CHANNEL_ID = "melodiq_radio_channel"
        const val NOTIFICATION_ID = 4108
        const val ACTION_PLAY = "com.tasnimulhasan.radio.PLAY"
        const val ACTION_PAUSE = "com.tasnimulhasan.radio.PAUSE"
        const val ACTION_STOP = "com.tasnimulhasan.radio.STOP"
    }
}
