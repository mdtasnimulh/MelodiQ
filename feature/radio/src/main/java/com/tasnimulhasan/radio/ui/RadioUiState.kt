package com.tasnimulhasan.radio.ui

import com.tasnimulhasan.radio.domain.FmBand
import com.tasnimulhasan.radio.domain.RadioError
import com.tasnimulhasan.radio.domain.RadioMode
import com.tasnimulhasan.radio.domain.RadioStation
import com.tasnimulhasan.radio.domain.RadioStations
import com.tasnimulhasan.radio.domain.RadioStatus

/**
 * The one state object the Radio screen renders. Everything the screen shows or enables is derived
 * here, so the composables stay free of logic.
 */
data class RadioUiState(
    val mode: RadioMode = RadioMode.INTERNET,
    val status: RadioStatus = RadioStatus.IDLE,
    val stations: List<RadioStation> = RadioStations.defaults,
    val selectedStationUrl: String? = null,
    val stationName: String? = null,
    val customUrl: String = "",
    val frequencyKhz: Int = FmBand.DEFAULT_KHZ,
    val nowPlaying: String? = null,
    val headphonesPlugged: Boolean = false,
    val error: RadioError? = null,
) {
    val isActive: Boolean get() = status == RadioStatus.PLAYING || status == RadioStatus.BUFFERING
    val isBuffering: Boolean get() = status == RadioStatus.BUFFERING

    /** Hardware FM cannot start without the headphone cable (antenna); the screen disables play then. */
    val canPlay: Boolean
        get() = when (mode) {
            RadioMode.INTERNET -> selectedStationUrl != null
            RadioMode.HARDWARE -> headphonesPlugged && error !is RadioError.HardwareUnavailable
        }

    val canStop: Boolean get() = status != RadioStatus.IDLE
}
