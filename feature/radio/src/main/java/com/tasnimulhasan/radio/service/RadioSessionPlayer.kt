package com.tasnimulhasan.radio.service

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.tasnimulhasan.radio.data.RadioController
import com.tasnimulhasan.radio.domain.FmBand
import com.tasnimulhasan.radio.domain.RadioMode
import com.tasnimulhasan.radio.domain.RadioStatus

/**
 * A Media3 [Player] that is only a window onto [RadioController].
 *
 * The notification, lock screen, Bluetooth / headset buttons and Android Auto all talk to a
 * MediaSession, and a MediaSession needs a Player. The radio has two very different engines
 * (ExoPlayer and the FM chip), so this player owns no audio at all: it reports the controller's
 * state and forwards play / pause / stop to it. That way the same session works for both engines
 * and survives switching between them.
 */
@OptIn(UnstableApi::class)
internal class RadioSessionPlayer(
    private val controller: RadioController,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    /** Call after [RadioController.state] changes so the session and notification refresh. */
    fun refresh() = invalidateState()

    override fun getState(): State {
        val s = controller.state.value
        val hardware = s.mode == RadioMode.HARDWARE

        val title = if (hardware) "FM ${FmBand.mhzLabel(s.frequencyKhz)} MHz" else s.stationName ?: "Internet radio"
        val subtitle = s.nowPlaying ?: if (hardware) "Hardware FM tuner" else "Internet radio"
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setIsPlayable(true)
            .build()
        val item = MediaItemData.Builder(ITEM_ID)
            .setMediaItem(MediaItem.Builder().setMediaId(ITEM_ID).setMediaMetadata(metadata).build())
            .setMediaMetadata(metadata)
            .setIsDynamic(true)
            .setIsSeekable(false)
            .setDurationUs(C.TIME_UNSET)
            .build()

        val playbackState = when (s.status) {
            RadioStatus.IDLE -> Player.STATE_IDLE
            RadioStatus.BUFFERING -> Player.STATE_BUFFERING
            RadioStatus.PLAYING, RadioStatus.PAUSED -> Player.STATE_READY
        }
        val commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_RELEASE,
            )
            .build()

        return State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(
                s.status == RadioStatus.PLAYING || s.status == RadioStatus.BUFFERING,
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
            )
            .setPlaybackState(playbackState)
            .setPlaylist(listOf(item))
            .setCurrentMediaItemIndex(0)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) controller.play() else controller.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        controller.stop()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

    private companion object {
        const val ITEM_ID = "melodiq_radio_live"
    }
}
