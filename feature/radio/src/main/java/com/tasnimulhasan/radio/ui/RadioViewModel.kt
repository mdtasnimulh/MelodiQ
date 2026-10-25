package com.tasnimulhasan.radio.ui

import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.radio.data.RadioController
import com.tasnimulhasan.radio.domain.RadioMode
import com.tasnimulhasan.radio.domain.RadioState
import com.tasnimulhasan.radio.domain.RadioStation
import com.tasnimulhasan.radio.domain.StationGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Presentation layer for the Radio screen. The screen only reads [uiState] and calls the functions
 * below; every decision (engine switching, headphone checks, audio focus) is made by the
 * [RadioController], which is shared with the foreground service so the notification and the screen
 * always show the same thing.
 */
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val controller: RadioController,
) : BaseViewModel() {

    /** The text in the "custom stream" box: the only state that belongs to the screen alone. */
    private val customUrl = MutableStateFlow("")
    private val customName = MutableStateFlow("")
    private val customGroup = MutableStateFlow(StationGroup.OTHERS)

    val uiState: StateFlow<RadioUiState> =
        combine(controller.state, customUrl, customName, customGroup) { radio, url, name, group ->
            radio.toUi(url, name, group)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = controller.state.value.toUi(customUrl.value, customName.value, customGroup.value),
            )

    fun onModeSelected(mode: RadioMode) = controller.selectMode(mode)

    fun onStationSelected(station: RadioStation) = controller.playStation(station)

    fun onCustomUrlChanged(text: String) = customUrl.update { text }

    fun onCustomNameChanged(text: String) = customName.update { text }

    fun onCustomGroupChanged(group: StationGroup) = customGroup.update { group }

    fun onPlayCustomUrl() = controller.playCustomUrl(customUrl.value)

    fun onSaveCustomStation() = controller.saveStation(customName.value, customUrl.value, customGroup.value)

    fun onRemoveStation(station: RadioStation) = controller.removeStation(station)

    /** Dial is moving: update the display without touching the chip. */
    fun onFrequencyChanged(khz: Int) = controller.setFrequency(khz)

    /** Dial was released or a step button was pressed: retune the chip. */
    fun onFrequencyCommitted() = controller.commitFrequency()

    fun onTogglePlayPause() = controller.togglePlayPause()

    fun onStop() = controller.stop()

    fun onDismissError() = controller.clearError()

    private fun RadioState.toUi(customUrlText: String, customNameText: String, group: StationGroup) = RadioUiState(
        mode = mode,
        status = status,
        selectedStationUrl = stationUrl,
        stationName = stationName,
        customUrl = customUrlText,
        customName = customNameText,
        customGroup = group,
        savedStations = savedStations,
        frequencyKhz = frequencyKhz,
        nowPlaying = nowPlaying,
        headphonesPlugged = headphonesPlugged,
        error = error,
    )
}
