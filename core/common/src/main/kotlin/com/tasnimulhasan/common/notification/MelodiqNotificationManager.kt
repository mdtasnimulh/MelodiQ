package com.tasnimulhasan.common.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSessionService
import com.tasnimulhasan.common.constant.AppConstants.NOTIFICATION_CHANNEL_ID
import com.tasnimulhasan.common.constant.AppConstants.NOTIFICATION_CHANNEL_NAME
import com.tasnimulhasan.common.constant.AppConstants.NOTIFICATION_ID
import com.tasnimulhasan.designsystem.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Owns the notification channel and the placeholder foreground notification.
 *
 * The real playback notification (title, cover art, controls, repeat and favourite buttons)
 * is built by Media3's MediaSessionService from the MediaSession - see MelodiqPlayerService
 * and MelodiqSessionCallback. This used to ALSO run a PlayerNotificationManager, so two
 * different notifications competed and the one on screen was Media3's default one, which is
 * why none of the icon / artwork / button changes made to the other one ever appeared.
 */
class MelodiqNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Suppress("unused") private val exoPlayer: ExoPlayer
) {
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    /** Must be called right after startForegroundService(); Media3 replaces it with the real
     * notification (same id) as soon as it has one. */
    fun startForegroundPlaceholder(mediaSessionService: MediaSessionService) {
        val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle("MelodiQ")
            .build()
        mediaSessionService.startForeground(NOTIFICATION_ID, notification)
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
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
