package com.tasnimulhasan.melodiq.ipod

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.tasnimulhasan.entity.home.MusicEntity

/**
 * Pages come in two families. CONTENT pages fill the display: HOME (Cover Flow of every song),
 * ALBUMS / ARTISTS / GENRES / FAVOURITES / SONG_LIST (all Cover Flow too) and NOW_PLAYING.
 * MENU pages (MAIN, MUSIC, PLAYLISTS, SETTINGS, COLOUR, ADD_TO_PLAYLIST) are plain lists shown in the panel that slides
 * in from the left when MENU is pressed.
 */
internal enum class IpodPage {
    HOME, ALBUMS, ARTISTS, GENRES, FAVOURITES, SONG_LIST, NOW_PLAYING,
    MAIN, MUSIC, PLAYLISTS, SETTINGS, COLOUR, ADD_TO_PLAYLIST,
}

/** How the music pages (Songs, Albums, Artists, Genres, Favourites and their songs) are drawn. */
internal enum class IpodMusicView(val label: String) {
    COVER_FLOW("Cover Flow"),
    LIST("List"),
}

internal val IpodPage.isMenuPage: Boolean
    get() = this == IpodPage.MAIN || this == IpodPage.MUSIC || this == IpodPage.PLAYLISTS ||
        this == IpodPage.SETTINGS || this == IpodPage.COLOUR || this == IpodPage.ADD_TO_PLAYLIST

internal val IpodPage.isCoverFlow: Boolean
    get() = this == IpodPage.HOME || this == IpodPage.ALBUMS || this == IpodPage.ARTISTS ||
        this == IpodPage.GENRES || this == IpodPage.FAVOURITES || this == IpodPage.SONG_LIST

/**
 * One screen on the iPod's navigation stack. [selected] lives here (not in the composable) so
 * going back lands on the same highlighted row, like the real thing. [songs] is only used by
 * SONG_LIST pages (an artist's, album's, genre's or playlist's songs).
 */
@Stable
internal class IpodEntry(
    val page: IpodPage,
    val title: String,
    val depth: Int,
    val songs: List<MusicEntity> = emptyList(),
) {
    var selected by mutableIntStateOf(0)
}

/** Everything needed to load a cover through the app's Coil AlbumArt fetcher. */
internal data class IpodArt(val songId: Long, val albumId: Long, val uri: Uri)

internal fun songUri(songId: Long): Uri =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

internal fun MusicEntity.toArt() = IpodArt(songId = songId, albumId = albumId, uri = contentUri)

internal fun artFor(representativeSongId: Long, albumId: Long) =
    IpodArt(songId = representativeSongId, albumId = albumId, uri = songUri(representativeSongId))

/** One line in a menu or list. [art] non-null makes it a taller row with a thumbnail. */
internal data class IpodRow(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val art: IpodArt? = null,
    val chevron: Boolean = true,
    val onSelect: () -> Unit,
)
