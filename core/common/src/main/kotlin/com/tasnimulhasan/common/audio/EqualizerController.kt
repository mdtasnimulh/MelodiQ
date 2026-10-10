package com.tasnimulhasan.common.audio

import android.media.audiofx.Equalizer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one Equalizer effect attached to the player's audio session.
 *
 * It used to be created and released by the Equalizer screen's ViewModel, so the sound only
 * changed while that screen was open: leaving it, or restarting the app, silently dropped the
 * user's preset. Keeping it in a singleton lets the player re-apply the saved settings
 * whenever playback starts, independent of any screen.
 */
@Singleton
class EqualizerController @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var sessionId = -1

    /** (Re)attaches to [session]. Returns false if the device refuses (e.g. a system EQ owns it). */
    @Synchronized
    fun attach(session: Int): Boolean {
        if (session <= 0) return false
        if (equalizer != null && sessionId == session) return true
        release()
        return try {
            equalizer = Equalizer(0, session)
            sessionId = session
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Equalizer")
            equalizer = null
            false
        }
    }

    val isAttached: Boolean get() = equalizer != null

    val numberOfBands: Int get() = runCatching { equalizer?.numberOfBands?.toInt() }.getOrNull() ?: 0

    fun bandLevelRange(): ShortArray =
        runCatching { equalizer?.bandLevelRange }.getOrNull() ?: shortArrayOf(-1500, 1500)

    /** Center frequency of [band] in Hz, or null if unavailable. */
    fun centerFreqHz(band: Int): Int? =
        runCatching { equalizer?.getCenterFreq(band.toShort())?.div(1000) }.getOrNull()

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
    }

    /** [level] is in millibels; clamped to what the device supports. Returns false on failure. */
    @Synchronized
    fun setBandLevel(band: Int, level: Int): Boolean {
        val eq = equalizer ?: return false
        return try {
            val range = bandLevelRange()
            eq.setBandLevel(band.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to set band level for band $band")
            false
        }
    }

    /** [gains] are the stored values (1.0 == 1000 millibels), one per band. */
    fun applyGains(gains: List<Double>) {
        gains.take(numberOfBands).forEachIndexed { band, value -> setBandLevel(band, (value * 1000).toInt()) }
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        sessionId = -1
    }
}
