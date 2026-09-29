package com.tasnimulhasan.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Derives an accent [Color] from a song's cover art for the "auto from album art" theme
 * option. Reuses [AlbumArtFetcher]'s own artwork-loading fallbacks (MediaStore thumbnail ->
 * legacy album art table -> embedded picture) so this never duplicates or drifts from how
 * artwork is loaded everywhere else in the app.
 */
object AlbumArtAccentExtractor {

    // A small bitmap is plenty for a dominant-color read and keeps this fast enough to run
    // on every track change without it being felt as a stutter.
    private const val SAMPLE_SIZE_PX = 96

    suspend fun extractAccentColor(context: Context, contentUri: Uri, albumId: Long): Color? =
        withContext(Dispatchers.IO) {
            val bitmap = loadSmallArtwork(context, contentUri, albumId) ?: return@withContext null
            val palette = runCatching { Palette.from(bitmap).generate() }.getOrNull() ?: return@withContext null
            val swatch = palette.vibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.dominantSwatch
                ?: return@withContext null
            Color(swatch.rgb)
        }

    private fun loadSmallArtwork(context: Context, contentUri: Uri, albumId: Long): Bitmap? {
        runCatching {
            return context.contentResolver.loadThumbnail(
                contentUri,
                Size(SAMPLE_SIZE_PX, SAMPLE_SIZE_PX),
                null,
            )
        }
        runCatching {
            val albumArtUri = android.content.ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId,
            )
            context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)?.let { return it }
            }
        }
        return runCatching {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, contentUri)
                retriever.embeddedPicture?.let { bytes ->
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }
}
