package com.tasnimulhasan.domain.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Current tempo / pitch of the player.
 *
 * @property speed 1.0 = normal. Changing it speeds up or slows down WITHOUT changing the pitch.
 * @property pitch 1.0 = original key. A semitone is a factor of 2^(1/12).
 * @property rememberedForSong true when these values are saved for the song that is playing
 * and will be re-applied whenever it plays; false when they are the shared default for all songs.
 */
data class PlaybackTuning(
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val rememberedForSong: Boolean = false,
) {
    val isDefault: Boolean get() = speed == 1f && pitch == 1f
}

/**
 * Speed / pitch control with an optional per-song memory (for podcasts, audiobooks, practice).
 * Lives in the domain layer so feature modules can use it without depending on the data module.
 */
interface PlaybackTuningController {
    val tuning: StateFlow<PlaybackTuning>

    /** Applies [speed] / [pitch]. Saved for the current song if it is "remembered", otherwise
     * saved as the default for every song. */
    fun setTuning(speed: Float, pitch: Float)

    /** Turn per-song memory on (saves the current values for this song) or off (forgets them). */
    fun setRememberForSong(enabled: Boolean)

    /** Back to 1.0x, original key. */
    fun reset()
}
