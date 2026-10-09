package com.tasnimulhasan.data.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Loads notification / lock-screen cover art for a local audio file. The MediaItem's artwork
 * URI is the audio file's own content URI, which the default loader cannot read as an image,
 * so this asks MediaStore for the album thumbnail and falls back to the embedded picture.
 */
@UnstableApi
class MelodiqBitmapLoader(private val context: Context) : BitmapLoader {

    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean =
        Util.isBitmapFactorySupportedMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = executor.submit(Callable {
        BitmapFactory.decodeByteArray(data, 0, data.size) ?: throw IllegalStateException("Could not decode artwork")
    })

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = executor.submit(Callable {
        loadArtwork(uri) ?: throw IllegalStateException("No artwork for $uri")
    })

    private fun loadArtwork(uri: Uri): Bitmap? {
        runCatching {
            return context.contentResolver.loadThumbnail(uri, Size(SIZE, SIZE), null)
        }
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private companion object {
        // Large enough to fill the expanded notification sharply, small enough to stay well
        // inside the system's notification bitmap memory limit.
        const val SIZE = 512
    }
}
