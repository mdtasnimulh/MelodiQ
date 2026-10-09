package com.tasnimulhasan.common.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.tasnimulhasan.common.constant.AppConstants.NOTIFICATION_CHANNEL_ID
import com.tasnimulhasan.common.constant.AppConstants.NOTIFICATION_CHANNEL_NAME
import com.tasnimulhasan.common.constant.AppConstants.NOTIFICATION_ID
import com.tasnimulhasan.designsystem.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MelodiqNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    // Built once and reused - previously a brand new PlayerNotificationManager was created
    // (and never released) every time the service's onStartCommand ran, which can happen
    // more than once per process. Each extra instance keeps its own internal update loop
    // attached to the same ExoPlayer, competing to draw the same notification ID, which
    // showed up as a stuck or inconsistent notification (wrong title, no progress).
    private var playerNotificationManager: PlayerNotificationManager? = null

    init {
        createNotificationChannel()
    }

    fun startNotificationService(
        mediaSessionService: MediaSessionService,
        mediaSession: MediaSession
    ) {
        if (playerNotificationManager == null) {
            playerNotificationManager = buildNotification(mediaSession)
        }
        startForegroundNotificationService(mediaSessionService)
        // The placeholder above reuses the same id and would otherwise sit there until the
        // next player event; force the real title/art/controls back immediately.
        playerNotificationManager?.invalidate()
    }

    fun release() {
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
    }

    private fun startForegroundNotificationService(mediaSessionService: MediaSessionService) {
        // This placeholder must never fail to build - it's what satisfies the OS's
        // startForeground() timing requirement before anything else has happened yet, so a
        // missing required field here (a small icon is mandatory on API 26+) would trade one
        // crash for another right at the most crash-sensitive point in the app.
        val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle("MelodiQ")
            .build()
        mediaSessionService.startForeground(NOTIFICATION_ID, notification)
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(mediaSession: MediaSession): PlayerNotificationManager =
        PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(
                MelodiqNotificationAdapter(
                    context = context,
                    pendingIntent = mediaSession.sessionActivity
                )
            )
            .setSmallIconResourceId(R.drawable.ic_stat_music)
            .setCustomActionReceiver(repeatReceiver)
            .build()
            .also {
                it.setMediaSessionToken(mediaSession.platformToken)
                it.setUseFastForwardActionInCompactView(true)
                it.setUseRewindActionInCompactView(true)
                it.setUseNextActionInCompactView(true)
                it.setPriority(NotificationCompat.PRIORITY_LOW)
                it.setPlayer(exoPlayer)
                exoPlayer.removeListener(repeatInvalidateListener)
                exoPlayer.addListener(repeatInvalidateListener)
            }

    private val repeatInvalidateListener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) {
            playerNotificationManager?.invalidate()
        }
    }

    private val repeatReceiver = @OptIn(UnstableApi::class) object : PlayerNotificationManager.CustomActionReceiver {
        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ): MutableMap<String, NotificationCompat.Action> {
            fun action(key: String, icon: Int, title: String) = NotificationCompat.Action(
                icon, title,
                PendingIntent.getBroadcast(
                    context, instanceId,
                    Intent(key).setPackage(context.packageName).putExtra(PlayerNotificationManager.EXTRA_INSTANCE_ID, instanceId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            return mutableMapOf(
                ACTION_REPEAT_OFF to action(ACTION_REPEAT_OFF, R.drawable.ic_notif_repeat_off, "Repeat off"),
                ACTION_REPEAT_ALL to action(ACTION_REPEAT_ALL, R.drawable.ic_notif_repeat_all, "Repeat all"),
                ACTION_REPEAT_ONE to action(ACTION_REPEAT_ONE, R.drawable.ic_notif_repeat_one, "Repeat one"),
            )
        }

        // Shows the button for the CURRENT state; tapping advances OFF -> ALL -> ONE -> OFF.
        override fun getCustomActions(player: Player): MutableList<String> = mutableListOf(
            when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> ACTION_REPEAT_ALL
                Player.REPEAT_MODE_ONE -> ACTION_REPEAT_ONE
                else -> ACTION_REPEAT_OFF
            }
        )

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            player.repeatMode = when (action) {
                ACTION_REPEAT_OFF -> Player.REPEAT_MODE_ALL
                ACTION_REPEAT_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private companion object {
        const val ACTION_REPEAT_OFF = "com.tasnimulhasan.melodiq.REPEAT_OFF"
        const val ACTION_REPEAT_ALL = "com.tasnimulhasan.melodiq.REPEAT_ALL"
        const val ACTION_REPEAT_ONE = "com.tasnimulhasan.melodiq.REPEAT_ONE"
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}