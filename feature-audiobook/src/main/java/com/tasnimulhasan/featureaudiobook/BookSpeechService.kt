package com.tasnimulhasan.featureaudiobook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the app alive and shows a notification (play/pause, stop) while a book is being read aloud,
 * so listening continues with the screen off. All the real work is in [BookSpeaker].
 */
@AndroidEntryPoint
class BookSpeechService : Service() {

    @Inject lateinit var speaker: BookSpeaker

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null
    private var foreground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Audio book narration", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> speaker.pause()
            ACTION_PLAY -> speaker.play()
            ACTION_STOP -> {
                speaker.stop()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // Must be called promptly after startForegroundService().
        enterForeground()
        if (watcher == null) {
            watcher = scope.launch {
                speaker.state
                    .map { Triple(it.playing, it.page, it.bookId) }
                    .distinctUntilChanged()
                    .collect { (playing, _, bookId) ->
                        if (bookId == null) {
                            stopSelf()
                        } else if (playing) {
                            enterForeground()
                        } else {
                            if (foreground) {
                                stopForeground(STOP_FOREGROUND_DETACH)
                                foreground = false
                            }
                            getSystemService(NotificationManager::class.java).notify(ID, buildNotification())
                        }
                    }
            }
        }
        return START_NOT_STICKY
    }

    private fun enterForeground() {
        val notification = buildNotification()
        // Starting a foreground service from the background can be refused; the notification is still shown.
        runCatching {
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            foreground = true
        }.onFailure {
            getSystemService(NotificationManager::class.java).notify(ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val s = speaker.state.value
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val toggle = if (s.playing) {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", action(ACTION_PAUSE, 1))
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", action(ACTION_PLAY, 2))
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentTitle(s.title.ifEmpty { "Audio book" })
            .setContentText(if (s.pageCount > 0) "Page ${s.page + 1} of ${s.pageCount}" else "")
            .setContentIntent(openApp)
            .setDeleteIntent(action(ACTION_STOP, 3))
            .addAction(toggle)
            .addAction(NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", action(ACTION_STOP, 3)))
            .setOnlyAlertOnce(true)
            .setOngoing(s.playing)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun action(name: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code, Intent(this, BookSpeechService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun onDestroy() {
        scope.cancel()
        getSystemService(NotificationManager::class.java).cancel(ID)
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL = "audio_book_narration"
        const val ID = 4107
        const val ACTION_PLAY = "com.tasnimulhasan.featureaudiobook.PLAY"
        const val ACTION_PAUSE = "com.tasnimulhasan.featureaudiobook.PAUSE"
        const val ACTION_STOP = "com.tasnimulhasan.featureaudiobook.STOP"
    }
}
