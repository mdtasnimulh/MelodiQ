package com.tasnimulhasan.eqalizer

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.annotation.OptIn
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tasnimulhasan.common.constant.AppConstants
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqualizerEnabledUseCase
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val setEqTypeUseCase: SetEqTypeUseCase,
    private val getEqTypeUseCase: GetEqTypeUseCase,
    private val setEqualizerEnabledUseCase: SetEqualizerEnabledUseCase,
    private val exoPlayer: ExoPlayer,
    private val controller: com.tasnimulhasan.common.audio.EqualizerController,
    private val context: Context
) : BaseViewModel() {
    val audioEffects = MutableStateFlow<AudioEffects?>(null)
    val enableEqualizer = MutableStateFlow(false)
    val enableTenBand = MutableStateFlow(false)
    val frequencyLabels = MutableStateFlow<List<String>>(emptyList())
    val isTenBandSupported = MutableStateFlow(true)
    val equalizerError = MutableStateFlow<String?>(null)
    private var audioSessionId = 0

    init {
        viewModelScope.launch {
            getEqTypeUseCase.invoke().collectLatest { appConfig ->
                audioEffects.tryEmit(appConfig.audioEffects)
                enableEqualizer.tryEmit(appConfig.enableEqualizer)
                enableTenBand.tryEmit(appConfig.audioEffects?.gainValues?.size == 10)
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun onStart(sessionId: Int = exoPlayer.audioSessionId) {
        audioSessionId = sessionId
        if (!controller.attach(sessionId)) {
            equalizerError.tryEmit("Failed to initialize equalizer. Try disabling system equalizer.")
            enableEqualizer.tryEmit(false)
            enableTenBand.tryEmit(false)
            isTenBandSupported.tryEmit(false)
            return
        }

        val numberOfBands = controller.numberOfBands.takeIf { it > 0 } ?: 5
        val activeBands = if (enableTenBand.value && numberOfBands >= 10) 10 else 5

        // Check if 10-band is supported
        if (enableTenBand.value && numberOfBands < 10) {
            Timber.w("Device supports only $numberOfBands bands")
            equalizerError.tryEmit("Device supports only $numberOfBands bands. Using 5-band equalizer.")
            isTenBandSupported.tryEmit(false)
        } else if (enableTenBand.value) {
            isTenBandSupported.tryEmit(true)
        }

        controller.setEnabled(enableEqualizer.value || enableTenBand.value)

        val frequencies = (0 until minOf(numberOfBands, activeBands)).map { band ->
            val freq = controller.centerFreqHz(band) ?: (31 * Math.pow(2.0, band.toDouble())).toInt()
            if (freq >= 1000) "${(freq / 1000.0).toString().take(3)}kHz" else "${freq}Hz"
        }
        frequencyLabels.tryEmit(frequencies)

        val currentGainValues = audioEffects.value?.gainValues ?: List(activeBands) { 0.0 }
        controller.applyGains(currentGainValues.take(minOf(numberOfBands, activeBands)))
    }

    fun onSelectPreset(presetPosition: Int) {
        if (audioEffects.value == null || !controller.isAttached) return

        val numberOfBands = controller.numberOfBands.takeIf { it > 0 } ?: 5
        val activeBands = if (enableTenBand.value && isTenBandSupported.value) 10 else 5
        val gain = if (presetPosition == AppConstants.PRESET_CUSTOM) {
            ArrayList(audioEffects.value!!.gainValues.take(minOf(numberOfBands, activeBands)))
        } else {
            ArrayList(getPresetGainValue(presetPosition, activeBands).take(minOf(numberOfBands, activeBands)))
        }

        val newAudioEffects = AudioEffects(presetPosition, gain)
        audioEffects.tryEmit(newAudioEffects)
        viewModelScope.launch {
            setEqTypeUseCase.invoke(newAudioEffects)
        }
        controller.applyGains(gain)
    }

    fun onBandLevelChanged(changedBand: Int, newGainValue: Int) {
        if (!controller.isAttached) return
        val numberOfBands = controller.numberOfBands.takeIf { it > 0 } ?: 5
        val activeBands = if (enableTenBand.value && isTenBandSupported.value) 10 else 5
        if (changedBand >= minOf(numberOfBands, activeBands)) return

        val range = controller.bandLevelRange()
        val bandLevel = newGainValue.coerceIn(range[0].toInt(), range[1].toInt())
        if (controller.setBandLevel(changedBand, bandLevel)) {
            val list = ArrayList(audioEffects.value?.gainValues ?: List(activeBands) { 0.0 })
            if (changedBand < list.size) list[changedBand] = bandLevel.toDouble() / 1000
            val newAudioEffects = AudioEffects(AppConstants.PRESET_CUSTOM, list)
            audioEffects.tryEmit(newAudioEffects)
            viewModelScope.launch {
                setEqTypeUseCase.invoke(newAudioEffects)
            }
        }
    }

    fun toggleEqualizer() {
        val newState = !enableEqualizer.value
        enableEqualizer.tryEmit(newState)
        controller.setEnabled(newState || enableTenBand.value)
        viewModelScope.launch {
            setEqualizerEnabledUseCase.invoke(newState || enableTenBand.value)
            if (!newState && !enableTenBand.value) {
                val newAudioEffects = AudioEffects(AppConstants.PRESET_FLAT, AppConstants.FLAT)
                audioEffects.tryEmit(newAudioEffects)
                setEqTypeUseCase.invoke(newAudioEffects)
            } else if (newState) {
                equalizerError.tryEmit(null) // Clear error when enabling
                onStart()
            }
        }
    }

    fun toggleTenBand() {
        val newState = !enableTenBand.value
        enableTenBand.tryEmit(newState)
        controller.setEnabled(newState || enableEqualizer.value)
        viewModelScope.launch {
            setEqualizerEnabledUseCase.invoke(newState || enableEqualizer.value)
            if (!newState && !enableEqualizer.value) {
                val newAudioEffects = AudioEffects(AppConstants.PRESET_FLAT, AppConstants.FLAT)
                audioEffects.tryEmit(newAudioEffects)
                setEqTypeUseCase.invoke(newAudioEffects)
            } else if (newState) {
                equalizerError.tryEmit(null) // Clear error when enabling
                onStart()
            }
        }
    }

    fun retryEqualizer() {
        equalizerError.tryEmit(null)
        isTenBandSupported.tryEmit(true) // Allow retry
        onStart()
    }

    private fun unbindSystemEqualizer() {
        val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(intent)
        Timber.d("Sent broadcast to close system equalizer for session $audioSessionId")
    }

    private fun getPresetGainValue(index: Int, bands: Int): List<Double> {
        return if (bands == 10) {
            when (index) {
                AppConstants.PRESET_FLAT -> AppConstants.FLAT_10
                AppConstants.PRESET_ACOUSTIC -> AppConstants.ACOUSTIC_10
                AppConstants.PRESET_DANCE_LOUNGE -> AppConstants.DANCE_10
                AppConstants.PRESET_HIP_HOP -> AppConstants.HIP_HOPE_10
                AppConstants.PRESET_JAZZ_BLUES -> AppConstants.JAZZ_10
                AppConstants.PRESET_POP -> AppConstants.POP_10
                AppConstants.PRESET_ROCK -> AppConstants.ROCK_10
                AppConstants.PRESET_PODCAST -> AppConstants.PODCAST_10
                else -> AppConstants.FLAT_10
            }
        } else {
            when (index) {
                AppConstants.PRESET_FLAT -> AppConstants.FLAT
                AppConstants.PRESET_ACOUSTIC -> AppConstants.ACOUSTIC
                AppConstants.PRESET_DANCE_LOUNGE -> AppConstants.DANCE
                AppConstants.PRESET_HIP_HOP -> AppConstants.HIP_HOPE
                AppConstants.PRESET_JAZZ_BLUES -> AppConstants.JAZZ
                AppConstants.PRESET_POP -> AppConstants.POP
                AppConstants.PRESET_ROCK -> AppConstants.ROCK
                AppConstants.PRESET_PODCAST -> AppConstants.PODCAST
                else -> AppConstants.FLAT
            }
        }
    }

    override fun onCleared() {
        // The Equalizer effect itself now lives in EqualizerController so the user's sound
        // keeps applying after this screen closes - only the system-EQ hand-off is cleaned up.
        unbindSystemEqualizer()
        super.onCleared()
    }
}