package com.tasnimulhasan.ui.image

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stable, cache-friendly model for a song's album art.
 * Pass THIS into AsyncImage — never a decoded Bitmap — so Coil can decode
 * off the main thread and cache the result (memory + disk) instead of the
 * ViewModel holding bitmaps and re-emitting the whole list on every load.
 */
data class AlbumArt(
    val songId: Long,
    val contentUri: Uri,
    val albumId: Long,
)

class AlbumArtKeyer : Keyer<AlbumArt> {
    override fun key(data: AlbumArt, options: Options): String = "album_art_${data.songId}"
}

class AlbumArtFetcher(
    private val data: AlbumArt,
    private val context: Context,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        loadArtwork()?.let { bitmap ->
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = true,
                dataSource = DataSource.DISK,
            )
        }
    }

    private fun loadArtwork(): Bitmap? {
        // Fast path: MediaStore's own thumbnail pipeline (min SDK 30, always available).
        // Already downsampled + cached by the system — this is what makes scrolling smooth.
        runCatching {
            return context.contentResolver.loadThumbnail(data.contentUri, THUMBNAIL_SIZE, null)
        }

        // Fallback: legacy per-album art table (some OEM ROMs / older files).
        runCatching {
            val albumArtUri = ContentUris.withAppendedId(LEGACY_ALBUM_ART_URI, data.albumId)
            context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                decodeSampled(stream.readBytes())?.let { return it }
            }
        }

        // Last resort: picture embedded in the file itself. Slowest path,
        // only reached if the two MediaStore-backed lookups above both fail.
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, data.contentUri)
                retriever.embeddedPicture?.let { decodeSampled(it) }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private fun decodeSampled(bytes: ByteArray, reqSize: Int = 512): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqSize && bounds.outHeight / (sample * 2) >= reqSize) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    class Factory : Fetcher.Factory<AlbumArt> {
        override fun create(data: AlbumArt, options: Options, imageLoader: ImageLoader): Fetcher =
            AlbumArtFetcher(data, options.context)
    }

    private companion object {
        val THUMBNAIL_SIZE = Size(512, 512)
        val LEGACY_ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}