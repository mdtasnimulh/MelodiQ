package com.tasnimulhasan.radio.data

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tasnimulhasan.radio.domain.RadioStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Internet radio engine: a Media3 [ExoPlayer] used only for the live stream.
 *
 * Threading: ExoPlayer must be used from the thread it was built on. Every method here is called
 * from the main thread (RadioController runs on Dispatchers.Main), and the player is built lazily
 * on that thread.
 *
 * Audio focus: handled by ExoPlayer itself (`setAudioAttributes(attrs, handleAudioFocus = true)`).
 * It pauses when another app takes focus, ducks for short interruptions, resumes after transient
 * losses, and also pauses when headphones are pulled (`setHandleAudioBecomingNoisy`).
 *
 * Network: [stop] releases the whole player, which closes the HTTP connection and drops all
 * buffered data, so switching to the hardware tuner leaves no streaming traffic behind. This is the
 * player the radio owns; it is deliberately NOT the music player's ExoPlayer.
 */
@Singleton
class InternetRadioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class State(
        val status: RadioStatus = RadioStatus.IDLE,
        val nowPlaying: String? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var currentUrl: String? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = publish()
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = publish()
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // ICY stream titles arrive as the media title ("Artist - Song").
            val title = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
                ?: mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() }
            _state.value = _state.value.copy(nowPlaying = title)
        }

        override fun onPlayerError(error: PlaybackException) {
            val message = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                    "This station uses plain http, which Android blocks. Use an https:// stream."
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "No connection to the station. Check your internet."
                else -> "The stream could not be played (${error.errorCodeName})."
            }
            _state.value = State(status = RadioStatus.IDLE, nowPlaying = null, error = message)
        }
    }

    /** Starts (or resumes) [url]. A different URL replaces the current stream. */
    fun play(url: String) {
        val exo = player ?: buildPlayer().also { player = it }
        if (currentUrl != url || exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED) {
            currentUrl = url
            _state.value = State(status = RadioStatus.BUFFERING)
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
        } else {
            _state.value = _state.value.copy(error = null)
        }
        exo.playWhenReady = true
        publish()
    }

    fun pause() {
        player?.playWhenReady = false
        publish()
    }

    /** Resumes the loaded stream; the caller falls back to [play] when nothing is loaded. */
    fun resume(): Boolean {
        val exo = player ?: return false
        if (exo.playbackState == Player.STATE_IDLE) return false
        exo.playWhenReady = true
        publish()
        return true
    }

    /** Stops, closes the connection and frees the player completely. */
    fun stop() {
        player?.let {
            it.removeListener(listener)
            it.stop()
            it.clearMediaItems()
            it.release()
        }
        player = null
        currentUrl = null
        _state.value = State()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun buildPlayer(): ExoPlayer =
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .also { it.addListener(listener) }

    private fun publish() {
        val exo = player
        val status = when {
            exo == null -> RadioStatus.IDLE
            exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED -> RadioStatus.IDLE
            !exo.playWhenReady -> RadioStatus.PAUSED
            exo.playbackState == Player.STATE_BUFFERING -> RadioStatus.BUFFERING
            else -> RadioStatus.PLAYING
        }
        val previous = _state.value
        _state.value = previous.copy(
            status = status,
            // A stream that plays again has recovered.
            error = if (status == RadioStatus.PLAYING) null else previous.error,
        )
    }
}
