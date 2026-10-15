package com.tasnimulhasan.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tasnimulhasan.domain.player.PlaybackTuning
import com.tasnimulhasan.domain.player.PlaybackTuningController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns playback speed and pitch.
 *
 * Two layers, both kept in a small SharedPreferences file:
 *  - a global default that applies to every song, and
 *  - an optional per-song override ("remember for this song").
 *
 * On every track change the right layer is applied: the song's own values if it has them,
 * otherwise the global default. So a podcast can stay at 1.5x while music plays at 1.0x.
 *
 * It listens to the ExoPlayer directly (like MelodiqServiceHandler does) and does not touch
 * the queue, session or notification. ExoPlayer callbacks arrive on the main thread, and the
 * setters are called from the UI, so every player access here is on the main thread.
 */
class PlaybackTuningManager(
    context: Context,
    private val player: ExoPlayer,
) : PlaybackTuningController, Player.Listener {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var songId: Long = 0L

    private val _tuning = MutableStateFlow(
        PlaybackTuning(
            speed = prefs.getFloat(KEY_SPEED, 1f).coerceIn(MIN_SPEED, MAX_SPEED),
            pitch = prefs.getFloat(KEY_PITCH, 1f).coerceIn(MIN_PITCH, MAX_PITCH),
        )
    )
    override val tuning: StateFlow<PlaybackTuning> = _tuning.asStateFlow()

    init {
        player.addListener(this)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        songId = mediaItem?.mediaId?.toLongOrNull() ?: 0L
        applyForCurrentSong()
    }

    private fun applyForCurrentSong() {
        val override = if (songId != 0L) readOverride(songId) else null
        val next = override?.copy(rememberedForSong = true)
            ?: PlaybackTuning(
                speed = prefs.getFloat(KEY_SPEED, 1f).coerceIn(MIN_SPEED, MAX_SPEED),
                pitch = prefs.getFloat(KEY_PITCH, 1f).coerceIn(MIN_PITCH, MAX_PITCH),
                rememberedForSong = false,
            )
        push(next)
    }

    override fun setTuning(speed: Float, pitch: Float) {
        val s = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        val p = pitch.coerceIn(MIN_PITCH, MAX_PITCH)
        val remembered = _tuning.value.rememberedForSong && songId != 0L
        if (remembered) {
            prefs.edit().putString(songKey(songId), "$s|$p").apply()
        } else {
            prefs.edit().putFloat(KEY_SPEED, s).putFloat(KEY_PITCH, p).apply()
        }
        push(PlaybackTuning(s, p, remembered))
    }

    override fun setRememberForSong(enabled: Boolean) {
        if (songId == 0L) return
        val current = _tuning.value
        if (enabled) {
            prefs.edit().putString(songKey(songId), "${current.speed}|${current.pitch}").apply()
        } else {
            prefs.edit().remove(songKey(songId)).apply()
        }
        _tuning.value = current.copy(rememberedForSong = enabled)
    }

    override fun reset() = setTuning(1f, 1f)

    private fun push(next: PlaybackTuning) {
        val params = player.playbackParameters
        if (params.speed != next.speed || params.pitch != next.pitch) {
            player.playbackParameters = PlaybackParameters(next.speed, next.pitch)
        }
        _tuning.value = next
    }

    private fun readOverride(id: Long): PlaybackTuning? {
        val raw = prefs.getString(songKey(id), null) ?: return null
        val parts = raw.split("|")
        val s = parts.getOrNull(0)?.toFloatOrNull() ?: return null
        val p = parts.getOrNull(1)?.toFloatOrNull() ?: return null
        return PlaybackTuning(s.coerceIn(MIN_SPEED, MAX_SPEED), p.coerceIn(MIN_PITCH, MAX_PITCH))
    }

    private fun songKey(id: Long) = "song_$id"

    companion object {
        private const val PREFS = "melodiq_playback_tuning"
        private const val KEY_SPEED = "default_speed"
        private const val KEY_PITCH = "default_pitch"
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2.0f
        const val MIN_PITCH = 0.5f   // an octave down
        const val MAX_PITCH = 2.0f   // an octave up
    }
}
