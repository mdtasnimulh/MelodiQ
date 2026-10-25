package com.tasnimulhasan.radio.domain

/**
 * The two ways the app can produce radio audio. Exactly one engine is ever active:
 *  - [Internet]  streams over the network with Media3 ExoPlayer.
 *  - [Hardware]  drives the phone's FM tuner chip through android.hardware.radio.
 */
sealed interface RadioEngine {
    /** An Icecast / Shoutcast / HLS stream at [url]. */
    data class Internet(val url: String) : RadioEngine

    /** The FM tuner chip tuned to [frequencyKhz] (e.g. 98_100 for 98.1 MHz). */
    data class Hardware(val frequencyKhz: Int) : RadioEngine
}
