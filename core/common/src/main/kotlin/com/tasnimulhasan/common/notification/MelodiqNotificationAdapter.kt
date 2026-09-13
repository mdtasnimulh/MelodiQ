package com.tasnimulhasan.common.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class MelodiqNotificationAdapter(
    private val context: Context,
    private val pendingIntent: PendingIntent?
) : PlayerNotificationManager.MediaDescriptionAdapter {

    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastLoadedUri: Uri? = null
    private var lastLoadedBitmap: Bitmap? = null

    override fun getCurrentContentTitle(player: Player): CharSequence =
        player.mediaMetadata.displayTitle ?: "Unknown"

    override fun createCurrentContentIntent(player: Player): PendingIntent? = pendingIntent

    override fun getCurrentContentText(player: Player): CharSequence =
        player.mediaMetadata.albumArtist ?: "Unknown"

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        val contentUri = player.currentMediaItem?.localConfiguration?.uri ?: return null

        // Avoid re-decoding on every notification refresh tick for the same track.
        if (contentUri == lastLoadedUri && lastLoadedBitmap != null) {
            return lastLoadedBitmap
        }

        adapterScope.launch {
            val bitmap = loadArtwork(contentUri)
            lastLoadedUri = contentUri
            lastLoadedBitmap = bitmap
            if (bitmap != null) callback.onBitmap(bitmap)
        }
        return null
    }

    private fun loadArtwork(contentUri: Uri): Bitmap? {
        runCatching {
            return context.contentResolver.loadThumbnail(contentUri, Size(256, 256), null)
        }

        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, contentUri)
                retriever.embeddedPicture?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }
}