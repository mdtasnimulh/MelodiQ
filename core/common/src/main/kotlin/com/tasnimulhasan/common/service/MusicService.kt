package com.tasnimulhasan.common.service

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tasnimulhasan.common.constant.AppConstants
import com.tasnimulhasan.common.constant.AppConstants.CHANNEL_ID
import com.tasnimulhasan.common.constant.AppConstants.NEXT
import com.tasnimulhasan.common.constant.AppConstants.PLAY_PAUSE
import com.tasnimulhasan.common.constant.AppConstants.PREV
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import com.tasnimulhasan.designsystem.R as Res

class MusicService: Service() {

    private val binder = MusicBinder()

    inner class MusicBinder: Binder() {
        fun getService() = this@MusicService

        fun setMusicList(list: List<MusicEntity>) {
            this@MusicService.musicList = list.toMutableList()
        }

        fun currentDuration() = this@MusicService.currentDuration

        fun maxDuration() = this@MusicService.maxDuration

        fun isPlaying() = this@MusicService.isPlaying

        fun currentTrack() = this@MusicService.currentTrack
    }

    private var mediaPlayer = MediaPlayer()
    private val currentTrack = MutableStateFlow<MusicEntity?>(null)
    private val maxDuration = MutableStateFlow(0f)
    private val currentDuration = MutableStateFlow(0f)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var musicList = mutableListOf<MusicEntity>()
    private var isPlaying = MutableStateFlow(false)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (intent.action) {
                PREV -> {
                    prev()
                }
                PLAY_PAUSE -> {
                    playPause()
                }
                NEXT -> {
                    next()
                }
                else -> {
                    currentTrack.update { musicList.first() }
                    currentTrack.value?.let { track -> play(track) }

                    /*musicList.firstOrNull()?.let { track ->
                        currentTrack.update { track }
                        play(track)
                    } ?: Timber.e("Music list is empty, unable to play current track!")*/
                }
            }
        }

        return START_STICKY
    }

    private fun play(track: MusicEntity) {
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this, track.contentUri)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(track)
            updateDuration()
        }
    }

    fun playPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
        currentTrack.value?.let { sendNotification(it) }
    }

    fun next() {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = currentTrack.value?.let { musicList.indexOf(it) }
        val nextIndex = index?.plus(1)?.mod(musicList.size)
        val nextItem = nextIndex?.let { musicList[it] }
        currentTrack.update { nextItem }
        nextItem?.contentUri?.let { mediaPlayer.setDataSource(this, it) }
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            currentTrack.value?.let { sendNotification(it) }
            updateDuration()
        }
    }

    fun prev() {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = currentTrack.value?.let { musicList.indexOf(it) }
        val prevIndex = if (index?.let { it < 0 } == true) musicList.size.minus(1) else index?.minus(1)
        val prevItem = prevIndex?.let { musicList[it] }
        currentTrack.update { prevItem }
        prevItem?.contentUri?.let { mediaPlayer.setDataSource(this, it) }
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            currentTrack.value?.let { sendNotification(it) }
            updateDuration()
        }
    }

    private fun updateDuration() {
        job = scope.launch {
            if (mediaPlayer.isPlaying.not()) return@launch
            maxDuration.update { mediaPlayer.duration.toFloat() }
            while (true) {
                currentDuration.update { mediaPlayer.currentPosition.toFloat() }
                delay(1000)
            }
        }
    }

    private fun sendNotification(music: MusicEntity) {
        val session = MediaSessionCompat(this, "music")
        isPlaying.update { mediaPlayer.isPlaying }
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0,1,2)
            .setMediaSession(session.sessionToken)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(style)
            .setContentTitle(music.songTitle)
            .setContentText(music.artist)
            .addAction(Res.drawable.ic_backward, PREV, createPrevPendingIntent())
            .addAction(
                if (mediaPlayer.isPlaying) Res.drawable.ic_pause_circle else Res.drawable.ic_play_circle,
                PLAY_PAUSE,
                createPlayPausePendingIntent()
            )
            .addAction(Res.drawable.ic_next, NEXT, createNextPendingIntent())
            .setSmallIcon(Res.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, Res.drawable.ic_launcher_background))
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startForeground(AppConstants.FOREGROUND_ID, notification)
            }
        } else {
            startForeground(AppConstants.FOREGROUND_ID, notification)
        }
    }

    private fun createPrevPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = PREV
        }
        return PendingIntent.getService(
            this, AppConstants.NOTIFICATION_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createPlayPausePendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = PLAY_PAUSE
        }
        return PendingIntent.getService(
            this, AppConstants.NOTIFICATION_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNextPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = NEXT
        }
        return PendingIntent.getService(
            this, AppConstants.NOTIFICATION_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

}