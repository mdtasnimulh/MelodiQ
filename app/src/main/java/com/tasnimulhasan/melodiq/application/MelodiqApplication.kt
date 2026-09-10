package com.tasnimulhasan.melodiq.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tasnimulhasan.common.constant.AppConstants.CHANNEL_ID
import com.tasnimulhasan.common.constant.AppConstants.CHANNEL_NAME
import com.tasnimulhasan.melodiq.BuildConfig
import com.tasnimulhasan.ui.image.AlbumArtFetcher
import com.tasnimulhasan.ui.image.AlbumArtKeyer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MelodiQApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        setupNotification()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(AlbumArtFetcher.Factory())
                add(AlbumArtKeyer())
            }
            .crossfade(true)
            .build()

    private fun setupNotification() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}