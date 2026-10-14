package com.tasnimulhasan.melodiq.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.tasnimulhasan.common.constant.AppConstants.CHANNEL_ID
import com.tasnimulhasan.common.constant.AppConstants.CHANNEL_NAME
import com.tasnimulhasan.melodiq.BuildConfig
import com.tasnimulhasan.melodiq.widget.WidgetUpdater
import com.tasnimulhasan.ui.image.AlbumArtFetcher
import com.tasnimulhasan.ui.image.AlbumArtKeyer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MelodiQApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var widgetUpdater: dagger.Lazy<WidgetUpdater>

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        setupNotification()
        // Keep placed home screen widgets in sync with the player. Lazy + only when a widget
        // actually exists, so users without widgets pay nothing at cold start.
        if (WidgetUpdater.hasPlacedWidgets(this)) widgetUpdater.get().start()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(AlbumArtFetcher.Factory())
                add(AlbumArtKeyer())
            }
            .crossfade(true)
            // Album art is small and re-shown constantly (list scrolling, mini player, full
            // player, album grid all request the same songId/albumId keys) - give the
            // in-memory cache more headroom than Coil's default so scrolling back up or
            // switching screens doesn't have to re-decode art that was already loaded once
            // this session.
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .build()

    private fun setupNotification() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}