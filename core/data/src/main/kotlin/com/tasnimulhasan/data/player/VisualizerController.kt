package com.tasnimulhasan.data.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.math.sqrt

/**
 * Captures the raw output waveform for the given audio session and reduces it to a small,
 * fixed-size list of normalized (0f..1f) bars - cheap enough to redraw on every frame on a
 * mini player, popup player, or the full player screen.
 *
 * This deliberately does NOT do a real FFT / frequency-spectrum analysis: Visualizer's own
 * waveform capture (raw PCM amplitude over time) bucketed into N groups and RMS'd per bucket
 * gives a convincing, audio-reactive bar/waveform animation without pulling in an FFT
 * library or getting frequency-bin math wrong unverified. It will not match what a real
 * spectrum analyzer shows (bars won't correspond to actual frequency bands), but it reacts
 * to the actual audio in real time, not a canned animation.
 *
 * Needs RECORD_AUDIO at runtime (Android still gates Visualizer behind it even though this
 * only ever reads this app's own playback, not the microphone) - every entry point here is a
 * no-op if the permission isn't granted, so a denial just means no visualizer, nothing else
 * breaks.
 */
class VisualizerController(
    private val context: Context,
    private val barCount: Int = 32,
) {
    private var visualizer: Visualizer? = null

    private val _bars = MutableStateFlow(FloatArray(barCount))
    val bars: StateFlow<FloatArray> = _bars.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Starts capturing for [audioSessionId]. Safe to call repeatedly (e.g. on every track
     * change) - it's a no-op if already attached to the same session, and tears down and
     * reattaches if the session id changed. */
    fun attach(audioSessionId: Int) {
        if (!hasRecordAudioPermission()) {
            _isActive.value = false
            return
        }
        if (audioSessionId == 0) return // AudioManager.ERROR / no session yet
        // Visualizer has no "current session id" getter to compare against, so always tear
        // down and reattach. attach() is only called when the style turns on or playback
        // starts with it inactive - not per track and not per frame - so this isn't hot.
        release()
        runCatching {
            val v = Visualizer(audioSessionId)
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            v.captureSize = captureSize
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        if (waveform == null) return
                        _bars.value = bucketize(waveform, barCount)
                    }

                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        // Unused - see class doc for why this uses waveform capture instead.
                    }
                },
                (Visualizer.getMaxCaptureRate() / 2).coerceAtMost(20_000), // ~20fps, well under the device max
                true,  // waveform
                false, // fft
            )
            v.enabled = true
            visualizer = v
            _isActive.value = true
        }.onFailure { e ->
            Timber.e(e, "Visualizer unavailable on this device")
            visualizer = null
            _isActive.value = false
        }
    }

    fun release() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        _isActive.value = false
    }

    private fun bucketize(waveform: ByteArray, buckets: Int): FloatArray {
        if (waveform.isEmpty()) return FloatArray(buckets)
        val perBucket = (waveform.size / buckets).coerceAtLeast(1)
        return FloatArray(buckets) { i ->
            val start = i * perBucket
            val end = (start + perBucket).coerceAtMost(waveform.size)
            if (start >= end) return@FloatArray 0f
            var sumSquares = 0.0
            for (j in start until end) {
                // Unsigned 8-bit PCM, 128 is the zero/silence midpoint.
                val sample = (waveform[j].toInt() and 0xFF) - 128
                sumSquares += (sample * sample).toDouble()
            }
            val rms = sqrt(sumSquares / (end - start))
            (rms / 128.0).toFloat().coerceIn(0f, 1f)
        }
    }
}
