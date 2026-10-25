package com.tasnimulhasan.radio.domain

import java.util.Locale

/** Which engine the user has selected with the tabs. */
enum class RadioMode { INTERNET, HARDWARE }

/** What the active engine is doing, in terms both engines can report. */
enum class RadioStatus { IDLE, BUFFERING, PLAYING, PAUSED }

/** Everything that can go wrong, in terms the UI can explain to the user. */
sealed interface RadioError {
    /**
     * android.hardware.radio is unusable: RadioManager is null (no FM hardware / no broadcast radio
     * service), the app lacks the privileged ACCESS_BROADCAST_RADIO permission, or the tuner could
     * not be opened.
     */
    data class HardwareUnavailable(val reason: String) : RadioError

    /** Hardware FM uses the wired headphone cable as its antenna; it is not plugged in (or was pulled out). */
    data object HeadphonesUnplugged : RadioError

    /** The tuner accepted the request but could not lock onto the frequency. */
    data class TuneFailed(val reason: String) : RadioError

    /** The internet stream could not be opened or broke while playing. */
    data class StreamFailed(val reason: String) : RadioError

    /** Another app is holding audio focus (for example a phone call). */
    data object FocusDenied : RadioError
}

/** The sections the station list is split into. */
enum class StationGroup(val label: String) {
    BANGLADESHI("Bangladeshi"),
    NEWS("News"),
    MUSIC("Music"),
    PODCASTS("Podcasts"),
    EDUCATION("Education"),
    TECHNOLOGY("Technology"),
    OTHERS("Others")
}

/** An internet radio station the user can pick. */
data class RadioStation(
    val name: String,
    val genre: String,
    val url: String,
    val group: StationGroup = StationGroup.OTHERS,
)

/** The FM broadcast band the tuner dial covers. All values are in kilohertz, which is what the API wants. */
object FmBand {
    const val MIN_KHZ = 87_500
    const val MAX_KHZ = 108_000
    const val STEP_KHZ = 100
    const val STEP_COUNT = (MAX_KHZ - MIN_KHZ) / STEP_KHZ
    const val DEFAULT_KHZ = 98_100

    /** Clamps to the band and rounds to the nearest 100 kHz channel. */
    fun snap(khz: Int): Int {
        val inBand = khz.coerceIn(MIN_KHZ, MAX_KHZ)
        return ((inBand - MIN_KHZ + STEP_KHZ / 2) / STEP_KHZ) * STEP_KHZ + MIN_KHZ
    }

    fun indexOf(khz: Int): Int = (snap(khz) - MIN_KHZ) / STEP_KHZ

    fun khzAt(index: Int): Int = MIN_KHZ + index.coerceIn(0, STEP_COUNT) * STEP_KHZ

    /** "98.1" */
    fun mhzLabel(khz: Int): String = String.format(Locale.US, "%.1f", khz / 1000f)
}

/**
 * The one shared snapshot of the radio, owned by RadioController. The notification, the media
 * session and the screen all read this, so they can never disagree.
 */
data class RadioState(
    val mode: RadioMode = RadioMode.INTERNET,
    val status: RadioStatus = RadioStatus.IDLE,
    val stationName: String? = null,
    val stationUrl: String? = null,
    val frequencyKhz: Int = FmBand.DEFAULT_KHZ,
    /** Stream title from ICY metadata, when the station sends it. */
    val nowPlaying: String? = null,
    val headphonesPlugged: Boolean = false,
    val error: RadioError? = null,
    /** Stations the user saved, shown in their chosen group. */
    val savedStations: List<RadioStation> = emptyList(),
)