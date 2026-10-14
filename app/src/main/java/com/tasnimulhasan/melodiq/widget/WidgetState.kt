package com.tasnimulhasan.melodiq.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.ui.image.AlbumArt
import kotlinx.coroutines.flow.first

/** Everything a widget needs to draw one frame. Built fresh on every update. */
internal data class WidgetState(
    val song: MusicEntity?,
    val isPlaying: Boolean,
    val isFavourite: Boolean,
    val art: Bitmap?,
)

// RemoteViews have a hard limit on total bitmap memory per widget update, so keep the cover
// small - it is only ever shown at ~60-130dp.
private const val ART_SIZE_PX = 320

internal suspend fun loadWidgetState(context: Context): WidgetState {
    val entry = context.widgetEntryPoint()
    val song = entry.playerUseCases().observeCurrentSelectedAudio().value
    val playing = entry.playerUseCases().observeIsPlaying().value
    val favourite = song != null &&
        runCatching { entry.favouriteRepository().observeFavouriteIds().first().contains(song.songId) }
            .getOrDefault(false)
    val art = song?.let { loadArt(context, it) }
    return WidgetState(song = song, isPlaying = playing, isFavourite = favourite, art = art)
}

// Goes through the app's own Coil ImageLoader so it reuses AlbumArtFetcher (MediaStore
// thumbnail -> embedded picture), the memory cache and the AlbumArtVersion cache key.
private suspend fun loadArt(context: Context, song: MusicEntity): Bitmap? = runCatching {
    val request = ImageRequest.Builder(context)
        .data(AlbumArt(songId = song.songId, contentUri = song.contentUri, albumId = song.albumId))
        .size(ART_SIZE_PX)
        .allowHardware(false) // RemoteViews need a software bitmap
        .build()
    (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
}.getOrNull()
