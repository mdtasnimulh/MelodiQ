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
    NEWS("News"),
    BANGLADESHI("Bangladeshi"),
    OTHERS("Others"),
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

/**
 * Built-in stations, all https so Android's cleartext block does not stop them. Stream addresses
 * change without notice, so any of these can stop working; users can add their own and save them
 * under any group (see SavedStationStore).
 *
 * The Bangladeshi group has no built-in entries: no current, verified https stream addresses were
 * available when this was written. Saved stations the user puts in that group appear there.
 */
object RadioStations {
    val defaults = listOf(
        RadioStation("BBC World Service", "World news", "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service", StationGroup.NEWS),
        RadioStation("NPR News", "US news and talk", "https://npr-ice.streamguys1.com/live.mp3", StationGroup.NEWS),
        RadioStation("LBC", "UK news and talk", "https://media-ssl.musicradio.com/LBC", StationGroup.NEWS),
        RadioStation("KQED", "Public radio news", "https://streams.kqed.org/kqedradio", StationGroup.NEWS),

        RadioStation("SomaFM Groove Salad", "Ambient / downtempo", "https://ice1.somafm.com/groovesalad-128-mp3", StationGroup.OTHERS),
        RadioStation("SomaFM Defcon", "Electronic", "https://ice1.somafm.com/defcon-128-mp3", StationGroup.OTHERS),
        RadioStation("Radio Paradise", "Eclectic rock", "https://stream.radioparadise.com/aac-320", StationGroup.OTHERS),
        RadioStation("KEXP Seattle", "Indie / alternative", "https://kexp.streamguys1.com/kexp160.aac", StationGroup.OTHERS),
    )
}
