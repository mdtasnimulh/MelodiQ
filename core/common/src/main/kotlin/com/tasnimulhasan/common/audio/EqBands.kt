package com.tasnimulhasan.common.audio

import kotlin.math.ln

/**
 * The equalizer UI is its own band model (5 or 10 bands) and is independent of how many bands the
 * device's hardware Equalizer exposes. Gains are mapped between band layouts by interpolating on a
 * logarithmic frequency axis, so a 10-band curve works on every device.
 */
object EqBands {
    val TEN_FREQS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val FIVE_FREQS = intArrayOf(60, 230, 910, 3600, 14000)

    fun freqsFor(bandCount: Int): IntArray = if (bandCount >= 10) TEN_FREQS else FIVE_FREQS

    fun label(hz: Int): String =
        if (hz >= 1000) {
            val k = hz / 1000f
            if (k == k.toInt().toFloat()) "${k.toInt()}kHz" else "${k}kHz"
        } else "${hz}Hz"

    fun labelsFor(bandCount: Int): List<String> = freqsFor(bandCount).map { label(it) }

    /** Value of the curve [values] (laid out on [freqs]) at [hz], interpolated on a log axis. */
    fun interpolate(values: List<Double>, freqs: IntArray, hz: Int): Double {
        if (values.isEmpty()) return 0.0
        val n = minOf(values.size, freqs.size)
        if (n == 1 || hz <= freqs[0]) return values[0]
        if (hz >= freqs[n - 1]) return values[n - 1]
        var i = 0
        while (i < n - 2 && hz >= freqs[i + 1]) i++
        val a = ln(freqs[i].toDouble())
        val b = ln(freqs[i + 1].toDouble())
        val t = ((ln(hz.toDouble()) - a) / (b - a)).coerceIn(0.0, 1.0)
        return values[i] + (values[i + 1] - values[i]) * t
    }

    /** Re-samples [values] from the layout of its own size onto [targetBands] (5 or 10) bands. */
    fun resample(values: List<Double>, targetBands: Int): List<Double> {
        val target = freqsFor(targetBands)
        if (values.size == target.size) return values
        val source = freqsFor(values.size)
        return target.map { interpolate(values, source, it) }
    }
}
