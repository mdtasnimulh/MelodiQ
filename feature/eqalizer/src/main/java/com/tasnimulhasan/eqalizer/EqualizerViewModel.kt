package com.tasnimulhasan.eqalizer

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.annotation.OptIn
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tasnimulhasan.common.audio.EqBands
import com.tasnimulhasan.common.constant.AppConstants
import com.tasnimulhasan.domain.base.BaseViewModel
import com.tasnimulhasan.domain.localusecase.datastore.GetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqTypeUseCase
import com.tasnimulhasan.domain.localusecase.datastore.SetEqualizerEnabledUseCase
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    /** true = 10 bands, false = 5 bands. Stored implicitly as the size of the saved gain list. */
    val enableTenBand = MutableStateFlow(false)
    val frequencyLabels = MutableStateFlow(EqBands.labelsFor(5))

    /** Only set when the system refuses the effect (e.g. another equalizer owns the session). */
    val equalizerError = MutableStateFlow<String?>(null)
    private var audioSessionId = 0
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            getEqTypeUseCase.invoke().collectLatest { appConfig ->
                audioEffects.tryEmit(appConfig.audioEffects)
                enableEqualizer.tryEmit(appConfig.enableEqualizer)
                val ten = appConfig.audioEffects?.gainValues?.size == 10
                enableTenBand.tryEmit(ten)
                frequencyLabels.tryEmit(EqBands.labelsFor(if (ten) 10 else 5))
            }
        }
    }

    private fun bandCount() = if (enableTenBand.value) 10 else 5

    private fun currentGains(): List<Double> =
        EqBands.resample(audioEffects.value?.gainValues ?: List(bandCount()) { 0.0 }, bandCount())

    @OptIn(UnstableApi::class)
    fun onStart(sessionId: Int = exoPlayer.audioSessionId) {
        audioSessionId = sessionId
        applyToHardware()
    }

    /** Pushes the current state to the device equalizer. Any band count works on any device. */
    @OptIn(UnstableApi::class)
    private fun applyToHardware() {
        val attached = controller.attach(exoPlayer.audioSessionId)
        if (!attached) {
            equalizerError.tryEmit("Couldn't reach the audio effect right now. It will apply when playback starts.")
            return
        }
        equalizerError.tryEmit(null)
        controller.setEnabled(enableEqualizer.value)
        controller.applyGains(currentGains())
    }

    private fun persist(effects: AudioEffects, debounceMs: Long = 0L) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            setEqTypeUseCase.invoke(effects)
        }
    }

    fun onSelectPreset(presetPosition: Int) {
        val bands = bandCount()
        val gain = if (presetPosition == AppConstants.PRESET_CUSTOM) {
            currentGains()
        } else {
            getPresetGainValue(presetPosition, bands)
        }
        val newAudioEffects = AudioEffects(presetPosition, gain)
        audioEffects.tryEmit(newAudioEffects)
        persist(newAudioEffects)
        applyToHardware()
    }

    /** [newGainValue] is in millibels. */
    fun onBandLevelChanged(changedBand: Int, newGainValue: Int) {
        val bands = bandCount()
        if (changedBand !in 0 until bands) return
        val range = controller.bandLevelRange()
        val bandLevel = newGainValue.coerceIn(range[0].toInt(), range[1].toInt())
        val list = ArrayList(currentGains())
        list[changedBand] = bandLevel.toDouble() / 1000
        val newAudioEffects = AudioEffects(AppConstants.PRESET_CUSTOM, list)
        audioEffects.tryEmit(newAudioEffects)
        persist(newAudioEffects, debounceMs = 150L)
        if (controller.attach(exoPlayer.audioSessionId)) {
            controller.setEnabled(enableEqualizer.value)
            controller.applyGains(list)
        }
    }

    fun toggleEqualizer() {
        val newState = !enableEqualizer.value
        enableEqualizer.tryEmit(newState)
        viewModelScope.launch { setEqualizerEnabledUseCase.invoke(newState) }
        if (newState) equalizerError.tryEmit(null)
        applyToHardware()
    }

    /** Switches between the 5- and 10-band layout, carrying the current curve (or preset) across. */
    fun toggleTenBand() = setTenBand(!enableTenBand.value)

    fun setTenBand(ten: Boolean) {
        if (enableTenBand.value == ten) return
        val bands = if (ten) 10 else 5
        val old = audioEffects.value
        val preset = old?.selectedEffectType ?: AppConstants.PRESET_FLAT
        val gains = if (preset != AppConstants.PRESET_CUSTOM) {
            getPresetGainValue(preset, bands)
        } else {
            EqBands.resample(old?.gainValues ?: List(5) { 0.0 }, bands)
        }
        val newAudioEffects = AudioEffects(preset, gains)
        enableTenBand.tryEmit(ten)
        frequencyLabels.tryEmit(EqBands.labelsFor(bands))
        audioEffects.tryEmit(newAudioEffects)
        persist(newAudioEffects)
        applyToHardware()
    }

    fun retryEqualizer() {
        equalizerError.tryEmit(null)
        applyToHardware()
    }

    private fun unbindSystemEqualizer() {
        val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(intent)
        Timber.d("Sent broadcast to close system equalizer for session $audioSessionId")
    }

    /** 10-band curves are the source of truth; 5-band uses the tuned arrays, or is sampled from the 10-band one. */
    private fun getPresetGainValue(index: Int, bands: Int): List<Double> {
        val ten = when (index) {
            AppConstants.PRESET_ACOUSTIC -> AppConstants.ACOUSTIC_10
            AppConstants.PRESET_DANCE_LOUNGE -> AppConstants.DANCE_10
            AppConstants.PRESET_HIP_HOP -> AppConstants.HIP_HOPE_10
            AppConstants.PRESET_JAZZ_BLUES -> AppConstants.JAZZ_10
            AppConstants.PRESET_POP -> AppConstants.POP_10
            AppConstants.PRESET_ROCK -> AppConstants.ROCK_10
            AppConstants.PRESET_PODCAST -> AppConstants.PODCAST_10
            AppConstants.PRESET_BASS_BOOST -> AppConstants.BASS_BOOST_10
            AppConstants.PRESET_TREBLE_BOOST -> AppConstants.TREBLE_BOOST_10
            AppConstants.PRESET_CLASSICAL -> AppConstants.CLASSICAL_10
            AppConstants.PRESET_VOCAL -> AppConstants.VOCAL_10
            AppConstants.PRESET_LOUDNESS -> AppConstants.LOUDNESS_10
            else -> AppConstants.FLAT_10
        }
        if (bands >= 10) return ten
        return when (index) {
            AppConstants.PRESET_ACOUSTIC -> AppConstants.ACOUSTIC
            AppConstants.PRESET_DANCE_LOUNGE -> AppConstants.DANCE
            AppConstants.PRESET_HIP_HOP -> AppConstants.HIP_HOPE
            AppConstants.PRESET_JAZZ_BLUES -> AppConstants.JAZZ
            AppConstants.PRESET_POP -> AppConstants.POP
            AppConstants.PRESET_ROCK -> AppConstants.ROCK
            AppConstants.PRESET_PODCAST -> AppConstants.PODCAST
            AppConstants.PRESET_FLAT -> AppConstants.FLAT
            else -> EqBands.resample(ten, 5)
        }
    }

    override fun onCleared() {
        // The Equalizer effect itself now lives in EqualizerController so the user's sound
        // keeps applying after this screen closes - only the system-EQ hand-off is cleaned up.
        unbindSystemEqualizer()
        super.onCleared()
    }
}
