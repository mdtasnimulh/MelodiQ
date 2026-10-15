package com.tasnimulhasan.melodiq.ipod

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberUpdatedState
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.app.Activity
import android.os.Process
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.AlbumSummary
import com.tasnimulhasan.entity.room.library.ArtistSummary
import com.tasnimulhasan.entity.room.library.GenreSummary
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import kotlin.math.sign

/**
 * Player B: the whole app as an iPod.
 *
 * It opens on a Cover Flow of every song. MENU slides the menu in from the left (MENU again
 * closes it; hold MENU to open it from any depth). Songs, albums, artists, genres, favourites
 * and the songs inside them are all Cover Flow; playlists are a list. Turn the wheel (or drag
 * across the display) to move, press the centre (or tap) to open.
 */
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
    val bodyColor by viewModel.color.collectAsStateWithLifecycle()
    val theme = remember(bodyColor) { ipodTheme(bodyColor) }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val minimize = remember(activity) { { activity?.moveTaskToBack(true); Unit } }
    val exitApp = remember(activity, viewModel) {
        {
            viewModel.stopEverything {
                activity?.finishAndRemoveTask()
                Process.killProcess(Process.myPid())
            }
        }
    }

    // null = dialog closed; otherwise whether the playing song should go into the new playlist.
    var newPlaylistFor by remember { mutableStateOf<Boolean?>(null) }
    val onNewPlaylist = remember { { addCurrent: Boolean -> newPlaylistFor = addCurrent } }

    val data = IpodData(songs, artists, albums, genres, playlists, favouriteIds, current, clickFeedback, bodyColor, minimize, exitApp, onNewPlaylist)
    val latestData by rememberUpdatedState(data)
    val latestSwitch by rememberUpdatedState(onSwitchToClassic)

    // Status / navigation bar icons follow the body colour (light icons on a dark body).
    DisposableEffect(activity, theme.darkBody) {
        val a = activity as? ComponentActivity
        if (a != null) {
            val bar = if (theme.darkBody) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            a.enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    newPlaylistFor?.let { addCurrent ->
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newPlaylistFor = null },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Playlist name") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.createPlaylist(name, addCurrent)
                        if (addCurrent) viewModel.closeMenu()
                        newPlaylistFor = null
                    },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { newPlaylistFor = null }) { Text("Cancel") } },
        )
    }

    val contentTop = viewModel.content.last()
    val menuTop = viewModel.menu.lastOrNull()
    val active = menuTop ?: contentTop

    LaunchedEffect(songs.isNotEmpty()) { viewModel.syncHomeSelection() }

    // Opened from the notification / a launcher shortcut: jump straight to Now Playing.
    LaunchedEffect(openNowPlayingRequested, current?.songId) {
        if (openNowPlayingRequested && current != null) {
            viewModel.pushContent(IpodPage.NOW_PLAYING, "Now Playing")
            onOpenNowPlayingHandled()
        }
    }

    BackHandler(enabled = viewModel.menu.isNotEmpty() || viewModel.content.size > 1) { viewModel.back() }

    val view = LocalView.current
    val feedback: () -> Unit = {
        if (clickFeedback) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    // The wheel callbacks always read the LIVE top-of-stack (menu first, else content), never a
    // value captured at an earlier composition - otherwise a menu opened later would be ignored
    // and the wheel / OK button would keep driving the screen behind it.
    fun liveEntry(): IpodEntry = viewModel.menu.lastOrNull() ?: viewModel.content.last()

    fun rotate(steps: Int) {
        val e = liveEntry()
        if (e.page == IpodPage.NOW_PLAYING) {
            viewModel.nowPlayingRotate(steps)
            return
        }
        val count = if (e.page.isMenuPage) buildRows(e, latestData, viewModel, latestSwitch).size else rowCount(e, latestData)
        // Fast turns skip several items, but only in long lists - a short menu moves one row at a time.
        val moved = if (count < 30) steps.sign else steps
        e.selected = (e.selected + moved).coerceIn(0, (count - 1).coerceAtLeast(0))
    }

    fun centerPressed() {
        val e = liveEntry()
        if (e.page == IpodPage.NOW_PLAYING) viewModel.toggleScrub()
        else buildRows(e, latestData, viewModel, latestSwitch).getOrNull(e.selected)?.onSelect?.invoke()
    }

    // Keeps the last menu level around while the panel is sliding out.
    val lastMenu = remember { arrayOfNulls<IpodEntry>(1) }
    if (menuTop != null) lastMenu[0] = menuTop

    CompositionLocalProvider(LocalIpodTheme provides theme) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.body)
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
                title = active.title,
                isPlaying = isPlaying,
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .height(displayHeight),
            ) {
                DisplayBody(
                    contentTop = contentTop,
                    menuTop = menuTop,
                    lastMenu = lastMenu[0],
                    data = data,
                    viewModel = viewModel,
                    onSwitchToClassic = onSwitchToClassic,
                )
            }

            Spacer(Modifier.weight(1f))

            ClickWheel(
                modifier = Modifier.size(wheelSize),
                onRotate = ::rotate,
                onCenter = ::centerPressed,
                onMenu = viewModel::menuButton,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onPlayPause = viewModel::playPause,
                onHoldSeek = viewModel::seekHold,
                onMenuHold = viewModel::menuHold,
                onFeedback = feedback,
                isPlaying = isPlaying,
            )

            Spacer(Modifier.weight(1f))
        }
    }
    }
}

private fun android.content.Context.findActivity(): Activity? {
    var c = this
    while (c is android.content.ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
private fun DisplayBody(
    contentTop: IpodEntry,
    menuTop: IpodEntry?,
    lastMenu: IpodEntry?,
    data: IpodData,
    viewModel: IpodViewModel,
    onSwitchToClassic: () -> Unit,
) {
    // Own scope: BoxScope is the only implicit layout receiver here.
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = contentTop,
            transitionSpec = { slideSpec(forward = targetState.depth > initialState.depth) },
            label = "ipodContent",
        ) { entry ->
            ContentPage(entry, data, viewModel, onSwitchToClassic) { steps ->
                entry.selected = (entry.selected + steps).coerceIn(0, (rowCount(entry, data) - 1).coerceAtLeast(0))
            }
        }

        // Tapping the exposed part of the display closes the menu.
        if (menuTop != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x14000000))
                    .pointerInput(Unit) { detectTapGestures { viewModel.closeMenu() } }
            )
        }

        AnimatedVisibility(
            visible = menuTop != null,
            enter = slideInHorizontally(tween(260)) { -it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            val shown = menuTop ?: lastMenu
            if (shown != null) {
                AnimatedContent(
                    targetState = shown,
                    transitionSpec = { slideSpec(forward = targetState.depth > initialState.depth) },
                    label = "ipodMenu",
                ) { entry ->
                    val rows = rememberRows(entry, data, viewModel, onSwitchToClassic)
                    IpodMenuPanel(
                        entry = entry,
                        rows = rows,
                        onRowTap = { index ->
                            entry.selected = index
                            rows.getOrNull(index)?.onSelect?.invoke()
                        },
                    )
                }
            }
        }
    }
}

private fun slideSpec(forward: Boolean): ContentTransform {
    val dir = if (forward) 1 else -1
    return ContentTransform(
        targetContentEnter = slideInHorizontally(animationSpec = tween(260)) { fullWidth -> fullWidth * dir },
        initialContentExit = slideOutHorizontally(animationSpec = tween(260)) { fullWidth -> -fullWidth * dir },
        sizeTransform = SizeTransform(clip = true),
    )
}

@Composable
private fun ContentPage(
    entry: IpodEntry,
    data: IpodData,
    vm: IpodViewModel,
    onSwitchToClassic: () -> Unit,
    onStep: (Int) -> Unit,
) {
    when {
        entry.page == IpodPage.NOW_PLAYING -> NowPlayingScreen(vm)
        entry.page.isCoverFlow -> {
            val rows = rememberRows(entry, data, vm, onSwitchToClassic)
            CoverFlowView(
                items = rows,
                selected = entry.selected,
                emptyText = emptyTextFor(entry.page),
                onStep = onStep,
                onActivate = { rows.getOrNull(entry.selected)?.onSelect?.invoke() },
            )
        }
    }
}

/** Collected here, not in [IpodApp], so the half-second progress tick only redraws this screen. */
@Composable
private fun NowPlayingScreen(vm: IpodViewModel) {
    val song by vm.current.collectAsStateWithLifecycle()
    val positionMs by vm.positionMs.collectAsStateWithLifecycle()
    val durationMs by vm.durationMs.collectAsStateWithLifecycle()
    val scrubbing by vm.scrubbing.collectAsStateWithLifecycle()
    val showVolume by vm.showVolume.collectAsStateWithLifecycle()
    val volume by vm.volume.collectAsStateWithLifecycle()
    val favouriteIds by vm.favouriteIds.collectAsStateWithLifecycle()
    NowPlayingView(
        song = song,
        positionMs = positionMs,
        durationMs = durationMs,
        scrubbing = scrubbing,
        showVolume = showVolume,
        volume = volume,
        isFavourite = song?.songId in favouriteIds,
        onToggleFavourite = vm::toggleFavouriteCurrent,
        onAddToPlaylist = { vm.openMenuPage(IpodPage.ADD_TO_PLAYLIST, "Add to Playlist") },
    )
}

private fun emptyTextFor(page: IpodPage) = when (page) {
    IpodPage.HOME, IpodPage.SONG_LIST -> "No songs"
    IpodPage.ARTISTS -> "No artists"
    IpodPage.ALBUMS -> "No albums"
    IpodPage.GENRES -> "No genres"
    IpodPage.FAVOURITES -> "No favourites yet.\nHeart a song in Player A."
    IpodPage.PLAYLISTS -> ""
    else -> ""
}

/** Snapshot of everything the row builder needs. */
internal class IpodData(
    val songs: List<MusicEntity>,
    val artists: List<ArtistSummary>,
    val albums: List<AlbumSummary>,
    val genres: List<GenreSummary>,
    val playlists: List<PlaylistEntity>,
    val favouriteIds: Set<Long>,
    val current: MusicEntity?,
    val clickFeedback: Boolean,
    val color: IpodColor,
    val onMinimize: () -> Unit,
    val onExit: () -> Unit,
    val onNewPlaylist: (addCurrent: Boolean) -> Unit,
)

/** Rows are rebuilt only when what they are made from changes - not on every recomposition. */
@Composable
private fun rememberRows(
    entry: IpodEntry,
    d: IpodData,
    vm: IpodViewModel,
    onSwitchToClassic: () -> Unit,
): List<IpodRow> = remember(
    entry, d.songs, d.artists, d.albums, d.genres, d.playlists, d.favouriteIds, d.current, d.clickFeedback, d.color,
) { buildRows(entry, d, vm, onSwitchToClassic) }

private fun rowCount(entry: IpodEntry, d: IpodData): Int = when (entry.page) {
    IpodPage.HOME -> d.songs.size
    IpodPage.ALBUMS -> d.albums.size
    IpodPage.ARTISTS -> d.artists.size
    IpodPage.GENRES -> d.genres.size
    IpodPage.FAVOURITES -> d.songs.count { it.songId in d.favouriteIds }
    IpodPage.SONG_LIST -> entry.songs.size
    else -> 0
}

/** Rows (or Cover Flow items) of every page except Now Playing, which draws itself. */
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
        onSelect = onSelect,
    )

    fun playList(list: List<MusicEntity>, index: Int) {
        vm.playQueue(list, index)
        vm.pushContent(IpodPage.NOW_PLAYING, "Now Playing")
    }

    return when (entry.page) {
        // ── Cover Flow pages ─────────────────────────────────────────────────────────────
        IpodPage.HOME -> d.songs.map { song ->
            songRow(song) {
                vm.playFromLibrary(song)
                vm.pushContent(IpodPage.NOW_PLAYING, "Now Playing")
            }
        }

        IpodPage.SONG_LIST -> entry.songs.mapIndexed { index, song -> songRow(song) { playList(entry.songs, index) } }

        IpodPage.FAVOURITES -> {
            val list = d.songs.filter { it.songId in d.favouriteIds }
            list.mapIndexed { index, song -> songRow(song) { playList(list, index) } }
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

        IpodPage.ARTISTS -> d.artists.map { a ->
            IpodRow(
                id = "artist_${a.artist}",
                title = a.artist,
                subtitle = if (a.songCount == 1) "1 song" else "${a.songCount} songs",
                art = artFor(a.representativeSongId, 0L),
                onSelect = { vm.openArtist(a.artist) },
            )
        }

        IpodPage.GENRES -> d.genres.map { g ->
            IpodRow(
                id = "genre_${g.genre}",
                title = g.genre,
                subtitle = if (g.songCount == 1) "1 song" else "${g.songCount} songs",
                art = artFor(g.representativeSongId, 0L),
                onSelect = { vm.openGenre(g.genre) },
            )
        }

        // ── Menu pages (the panel that slides in from the left) ──────────────────────────
        IpodPage.MAIN -> buildList {
            add(IpodRow("music", "Music") { vm.pushMenu(IpodPage.MUSIC, "Music") })
            add(IpodRow("shuffle", "Shuffle Songs", chevron = false) {
                vm.shuffleAll()
                vm.pushContent(IpodPage.NOW_PLAYING, "Now Playing")
            })
            if (d.current != null) {
                add(IpodRow("now", "Now Playing", chevron = false) { vm.pushContent(IpodPage.NOW_PLAYING, "Now Playing") })
            }
            add(IpodRow("colour", "Colour") { vm.pushMenu(IpodPage.COLOUR, "Colour") })
            add(IpodRow("settings", "Settings") { vm.pushMenu(IpodPage.SETTINGS, "Settings") })
            add(IpodRow("minimize", "Minimize", chevron = false) {
                vm.closeMenu()
                d.onMinimize()
            })
            add(IpodRow("exit", "Exit", chevron = false) { d.onExit() })
        }

        IpodPage.COLOUR -> IpodColor.entries.map { c ->
            IpodRow(
                id = "colour_${c.name}",
                title = c.label + if (c == d.color) "  ✓" else "",
                chevron = false,
                onSelect = { vm.setColor(c) },
            )
        }

        IpodPage.ADD_TO_PLAYLIST -> buildList {
            add(IpodRow("new_playlist_add", "New Playlist…", chevron = false) { d.onNewPlaylist(true) })
            d.playlists.forEach { p -> add(
                IpodRow(
                    id = "add_${p.id}",
                    title = p.playlistName,
                    chevron = false,
                    onSelect = {
                        vm.addCurrentToPlaylist(p)
                        vm.closeMenu()
                    },
                ))
            }
        }

        IpodPage.MUSIC -> listOf(
            IpodRow("songs", "Songs", chevron = false) { vm.showHome() },
            IpodRow("albums", "Albums", chevron = false) { vm.pushContent(IpodPage.ALBUMS, "Albums", fromRoot = true) },
            IpodRow("artists", "Artists", chevron = false) { vm.pushContent(IpodPage.ARTISTS, "Artists", fromRoot = true) },
            IpodRow("genres", "Genres", chevron = false) { vm.pushContent(IpodPage.GENRES, "Genres", fromRoot = true) },
            IpodRow("playlists", "Playlists") { vm.pushMenu(IpodPage.PLAYLISTS, "Playlists") },
            IpodRow("favourites", "Favourites", chevron = false) { vm.pushContent(IpodPage.FAVOURITES, "Favourites", fromRoot = true) },
        )

        IpodPage.PLAYLISTS -> buildList {
            add(IpodRow("new_playlist", "New Playlist…", chevron = false) { d.onNewPlaylist(false) })
            d.playlists.forEach { p ->
                add(IpodRow(id = "playlist_${p.id}", title = p.playlistName, onSelect = { vm.openPlaylist(p.id, p.playlistName) }))
            }
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
                chevron = false,
                onSelect = onSwitchToClassic,
            ),
        )

        IpodPage.NOW_PLAYING -> emptyList()
    }
}
