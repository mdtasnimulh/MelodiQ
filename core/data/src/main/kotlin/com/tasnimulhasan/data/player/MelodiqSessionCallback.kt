package com.tasnimulhasan.data.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.tasnimulhasan.designsystem.R
import com.tasnimulhasan.domain.repository.local.FavouriteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Adds the Repeat and Favourite buttons to the media notification / lock screen via the
 * session's media button preferences, and handles taps on them.
 */
@UnstableApi
class MelodiqSessionCallback(
    private val player: Player,
    private val favouriteRepository: FavouriteRepository,
) : MediaSession.Callback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaSession? = null
    private var favouriteIds: Set<Long> = emptySet()

    private val repeatCommand = SessionCommand(ACTION_REPEAT, Bundle.EMPTY)
    private val favouriteCommand = SessionCommand(ACTION_FAVOURITE, Bundle.EMPTY)

    fun attach(mediaSession: MediaSession) {
        session = mediaSession
        refresh()
        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) = refresh()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh()
        })
        scope.launch {
            favouriteRepository.observeFavouriteIds().collect {
                favouriteIds = it
                refresh()
            }
        }
    }

    fun refresh() {
        session?.setMediaButtonPreferences(ImmutableList.of(repeatButton(), favouriteButton()))
    }

    private fun repeatButton(): CommandButton {
        val (icon, name) = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> R.drawable.ic_notif_repeat_all to "Repeat all"
            Player.REPEAT_MODE_ONE -> R.drawable.ic_notif_repeat_one to "Repeat one"
            else -> R.drawable.ic_notif_repeat_off to "Repeat off"
        }
        return CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(icon)
            .setDisplayName(name)
            .setSessionCommand(repeatCommand)
            .build()
    }

    private fun favouriteButton(): CommandButton {
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val isFavourite = songId != null && songId in favouriteIds
        return CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(if (isFavourite) R.drawable.ic_notif_fav_on else R.drawable.ic_notif_fav_off)
            .setDisplayName(if (isFavourite) "Remove from favourites" else "Add to favourites")
            .setSessionCommand(favouriteCommand)
            .build()
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(repeatCommand)
            .add(favouriteCommand)
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(commands)
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            ACTION_REPEAT -> player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            ACTION_FAVOURITE -> {
                val songId = player.currentMediaItem?.mediaId?.toLongOrNull()
                if (songId != null) scope.launch { favouriteRepository.toggleFavourite(songId) }
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    private companion object {
        const val ACTION_REPEAT = "com.tasnimulhasan.melodiq.REPEAT"
        const val ACTION_FAVOURITE = "com.tasnimulhasan.melodiq.FAVOURITE"
    }
}
