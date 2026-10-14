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
private suspend fun loadArt(context: Context, song: MusicEntity, sizePx: Int = ART_SIZE_PX): Bitmap? = runCatching {
    val request = ImageRequest.Builder(context)
        .data(AlbumArt(songId = song.songId, contentUri = song.contentUri, albumId = song.albumId))
        .size(sizePx)
        .allowHardware(false) // RemoteViews need a software bitmap
        .build()
    (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
}.getOrNull()

/** A song shown in one of the list-style widgets, with its (small) cover already decoded. */
internal data class WidgetSong(val song: MusicEntity, val art: Bitmap?)

private const val LIST_ART_SIZE_PX = 160

/** Most recently played songs that still exist in the current library. */
internal suspend fun loadRecentSongs(context: Context, limit: Int): List<WidgetSong> {
    val entry = context.widgetEntryPoint()
    val library = entry.playerUseCases().observeAudioList().value.mapTo(HashSet()) { it.songId }
    val recent = runCatching {
        entry.libraryUseCases().observeRecentlyPlayedWithTimestamp(limit * 2).first()
    }.getOrDefault(emptyList())
    return recent.map { it.first }
        .filter { library.isEmpty() || it.songId in library }
        .distinctBy { it.songId }
        .take(limit)
        .map { WidgetSong(it, loadArt(context, it, LIST_ART_SIZE_PX)) }
}

/** Favourite songs (resolved against the current library, like Home does). */
internal suspend fun loadFavouriteSongs(context: Context, limit: Int): List<WidgetSong> {
    val entry = context.widgetEntryPoint()
    val ids = runCatching { entry.favouriteRepository().observeFavouriteIds().first() }.getOrDefault(emptySet())
    return entry.playerUseCases().observeAudioList().value
        .filter { it.songId in ids }
        .take(limit)
        .map { WidgetSong(it, loadArt(context, it, LIST_ART_SIZE_PX)) }
}
