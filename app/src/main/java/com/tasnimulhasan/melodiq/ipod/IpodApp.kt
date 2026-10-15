package com.tasnimulhasan.melodiq.ipod

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.home.MusicEntity

private val BodyBrush = Brush.horizontalGradient(
    0.00f to Color(0xFFB7B7B9),
    0.10f to Color(0xFFCFCFD1),
    0.90f to Color(0xFFCFCFD1),
    1.00f to Color(0xFFB7B7B9),
)

/**
 * Player B: the whole app as an iPod. A small display on top that shows menus, lists, Cover
 * Flow and Now Playing, and a click wheel underneath that does all the navigating - turn it to
 * move the highlight, centre button to select, MENU to go back.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun IpodApp(
    openNowPlayingRequested: Boolean,
    onOpenNowPlayingHandled: () -> Unit,
    onSwitchToClassic: () -> Unit,
    viewModel: IpodViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favouriteIds by viewModel.favouriteIds.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val clickFeedback by viewModel.clickFeedback.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val scrubbing by viewModel.scrubbing.collectAsStateWithLifecycle()
    val showVolume by viewModel.showVolume.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()

    val data = IpodData(songs, artists, albums, genres, playlists, favouriteIds, current, clickFeedback)
    val top = viewModel.stack.last()
    val topRows = buildRows(top, data, viewModel, onSwitchToClassic)

    // Opened from the notification / a launcher shortcut: jump straight to Now Playing.
    LaunchedEffect(openNowPlayingRequested, current?.songId) {
        if (openNowPlayingRequested && current != null) {
            viewModel.push(IpodPage.NOW_PLAYING, "Now Playing")
            onOpenNowPlayingHandled()
        }
    }

    BackHandler(enabled = viewModel.stack.size > 1) { viewModel.pop() }

    val view = LocalView.current
    val feedback: () -> Unit = {
        if (clickFeedback) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun rotate(steps: Int) {
        when (top.page) {
            IpodPage.NOW_PLAYING -> viewModel.nowPlayingRotate(steps)
            IpodPage.COVER_FLOW -> top.selected = (top.selected + steps).coerceIn(0, (albums.size - 1).coerceAtLeast(0))
            else -> top.selected = (top.selected + steps).coerceIn(0, (topRows.size - 1).coerceAtLeast(0))
        }
    }

    fun centerPressed() {
        when (top.page) {
            IpodPage.NOW_PLAYING -> viewModel.toggleScrub()
            IpodPage.COVER_FLOW -> albums.getOrNull(top.selected)?.let { viewModel.openAlbum(it.albumId, it.album) }
            else -> topRows.getOrNull(top.selected)?.onSelect?.invoke()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BodyBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val displayHeight = maxHeight * 0.46f
        val wheelSize = min(maxWidth * 0.84f, maxHeight - displayHeight - 40.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            IpodDisplay(
                title = top.title,
                isPlaying = isPlaying,
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .height(displayHeight),
            ) {
                AnimatedContent(
                    targetState = top,
                    transitionSpec = {
                        // Deeper screen slides in from the right, going back slides in from the left.
                        val forward = targetState.depth > initialState.depth
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(tween(260)) { it * dir } togetherWith
                            slideOutHorizontally(tween(260)) { -it * dir })
                            .using(SizeTransform(clip = true))
                    },
                    label = "ipodPage",
                ) { entry ->
                    when (entry.page) {
                        IpodPage.MAIN -> IpodMainMenu(entry, buildRows(entry, data, viewModel, onSwitchToClassic), current)
                        IpodPage.COVER_FLOW -> CoverFlowView(albums, entry.selected)
                        IpodPage.NOW_PLAYING -> NowPlayingView(
                            song = current,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            scrubbing = scrubbing,
                            showVolume = showVolume,
                            volume = volume,
                        )
                        else -> IpodList(
                            entry = entry,
                            rows = buildRows(entry, data, viewModel, onSwitchToClassic),
                            emptyText = emptyTextFor(entry.page),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            ClickWheel(
                modifier = Modifier.size(wheelSize),
                onRotate = { steps ->
                    rotate(steps)
                },
                onCenter = ::centerPressed,
                onMenu = { viewModel.pop() },
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onPlayPause = viewModel::playPause,
                onHoldSeek = viewModel::seekHold,
                onFeedback = feedback,
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

private fun emptyTextFor(page: IpodPage) = when (page) {
    IpodPage.SONGS, IpodPage.SONG_LIST -> "No songs"
    IpodPage.ARTISTS -> "No artists"
    IpodPage.ALBUMS -> "No albums"
    IpodPage.GENRES -> "No genres"
    IpodPage.PLAYLISTS -> "No playlists yet"
    IpodPage.FAVOURITES -> "No favourites yet"
    else -> ""
}

/** Snapshot of everything the row builder needs. */
internal class IpodData(
    val songs: List<MusicEntity>,
    val artists: List<com.tasnimulhasan.entity.room.library.ArtistSummary>,
    val albums: List<com.tasnimulhasan.entity.room.library.AlbumSummary>,
    val genres: List<com.tasnimulhasan.entity.room.library.GenreSummary>,
    val playlists: List<com.tasnimulhasan.entity.room.playlist.PlaylistEntity>,
    val favouriteIds: Set<Long>,
    val current: MusicEntity?,
    val clickFeedback: Boolean,
)

/** Builds the rows of whichever menu/list page [entry] is. Now Playing and Cover Flow draw themselves. */
internal fun buildRows(
    entry: IpodEntry,
    d: IpodData,
    vm: IpodViewModel,
    onSwitchToClassic: () -> Unit,
): List<IpodRow> {
    fun songRow(song: MusicEntity, onSelect: () -> Unit) = IpodRow(
        id = "song_${song.songId}",
        title = song.songTitle,
        subtitle = song.artist,
        art = song.toArt(),
        chevron = true,
        onSelect = onSelect,
    )

    fun playList(list: List<MusicEntity>, index: Int) {
        vm.playQueue(list, index)
        vm.push(IpodPage.NOW_PLAYING, "Now Playing")
    }

    return when (entry.page) {
        IpodPage.MAIN -> buildList {
            add(IpodRow("music", "Music") { vm.push(IpodPage.MUSIC, "Music") })
            add(IpodRow("shuffle", "Shuffle Songs", chevron = false) {
                vm.shuffleAll()
                vm.push(IpodPage.NOW_PLAYING, "Now Playing")
            })
            if (d.current != null) add(IpodRow("now", "Now Playing") { vm.push(IpodPage.NOW_PLAYING, "Now Playing") })
            add(IpodRow("settings", "Settings") { vm.push(IpodPage.SETTINGS, "Settings") })
        }

        IpodPage.MUSIC -> listOf(
            IpodRow("coverflow", "Cover Flow") { vm.push(IpodPage.COVER_FLOW, "Cover Flow") },
            IpodRow("playlists", "Playlists") { vm.push(IpodPage.PLAYLISTS, "Playlists") },
            IpodRow("artists", "Artists") { vm.push(IpodPage.ARTISTS, "Artists") },
            IpodRow("albums", "Albums") { vm.push(IpodPage.ALBUMS, "Albums") },
            IpodRow("songs", "Songs") { vm.push(IpodPage.SONGS, "Songs") },
            IpodRow("genres", "Genres") { vm.push(IpodPage.GENRES, "Genres") },
            IpodRow("favourites", "Favourites") { vm.push(IpodPage.FAVOURITES, "Favourites") },
        )

        IpodPage.SONGS -> d.songs.map { song ->
            songRow(song) {
                vm.playFromLibrary(song)
                vm.push(IpodPage.NOW_PLAYING, "Now Playing")
            }
        }

        IpodPage.SONG_LIST -> entry.songs.mapIndexed { index, song -> songRow(song) { playList(entry.songs, index) } }

        IpodPage.FAVOURITES -> {
            val list = d.songs.filter { it.songId in d.favouriteIds }
            list.mapIndexed { index, song -> songRow(song) { playList(list, index) } }
        }

        IpodPage.ARTISTS -> d.artists.map { a ->
            IpodRow(
                id = "artist_${a.artist}",
                title = a.artist,
                subtitle = if (a.songCount == 1) "1 song" else "${a.songCount} songs",
                onSelect = { vm.openArtist(a.artist) },
            )
        }

        IpodPage.ALBUMS -> d.albums.map { a ->
            IpodRow(
                id = "album_${a.albumId}",
                title = a.album,
                subtitle = a.artist,
                art = artFor(a.representativeSongId, a.albumId),
                onSelect = { vm.openAlbum(a.albumId, a.album) },
            )
        }

        IpodPage.GENRES -> d.genres.map { g ->
            IpodRow(
                id = "genre_${g.genre}",
                title = g.genre,
                subtitle = if (g.songCount == 1) "1 song" else "${g.songCount} songs",
                onSelect = { vm.openGenre(g.genre) },
            )
        }

        IpodPage.PLAYLISTS -> d.playlists.map { p ->
            IpodRow(id = "playlist_${p.id}", title = p.playlistName, onSelect = { vm.openPlaylist(p.id, p.playlistName) })
        }

        IpodPage.SETTINGS -> listOf(
            IpodRow(
                id = "click",
                title = "Clicker: " + if (d.clickFeedback) "On" else "Off",
                chevron = false,
                onSelect = { vm.toggleClickFeedback() },
            ),
            IpodRow(
                id = "classic",
                title = "Switch to MelodiQ",
                subtitle = "Player A (default)",
                onSelect = onSwitchToClassic,
            ),
        )

        IpodPage.COVER_FLOW, IpodPage.NOW_PLAYING -> emptyList()
    }
}
