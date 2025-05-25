package com.tasnimulhasan.albums

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
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
class AlbumViewModel @Inject constructor(
    private val setEqTypeUseCase: SetEqTypeUseCase,
    private val getEqTypeUseCase: GetEqTypeUseCase,
    private val setEqualizerEnabledUseCase: SetEqualizerEnabledUseCase,
    private val exoPlayer: ExoPlayer,
    private val context: Context
) : BaseViewModel() {
    val audioEffects = MutableStateFlow<AudioEffects?>(null)
    val enableEqualizer = MutableStateFlow(false)
    val enableTenBand = MutableStateFlow(false)
    val frequencyLabels = MutableStateFlow<List<String>>(emptyList())
    val isTenBandSupported = MutableStateFlow(true) // Tracks if device can support 10 bands
    val equalizerError = MutableStateFlow<String?>(null) // Tracks initialization errors
    private var equalizer: Equalizer? = null
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
        equalizer?.release()
        try {
            equalizer = Equalizer(0, audioSessionId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Equalizer")
            equalizer = null
            equalizerError.tryEmit("Failed to initialize equalizer. Try disabling system equalizer.")
            enableEqualizer.tryEmit(false)
            enableTenBand.tryEmit(false)
            return
        }

        val numberOfBands = equalizer?.numberOfBands?.toInt() ?: 5
        val activeBands = if (enableTenBand.value) 10 else 5

        // Warn if 10-band is requested but unsupported, but don't disable switch
        if (enableTenBand.value && numberOfBands < 10) {
            Timber.w("Device supports only $numberOfBands bands")
            equalizerError.tryEmit("Device supports only $numberOfBands bands. Using available bands.")
            isTenBandSupported.tryEmit(false)
        } else {
            isTenBandSupported.tryEmit(true)
        }

        equalizer?.enabled = enableEqualizer.value || enableTenBand.value

        val frequencies = (0 until minOf(numberOfBands, activeBands)).map { band ->
            val freq = try {
                equalizer?.getCenterFreq(band.toShort())?.div(1000)
                    ?: (31 * Math.pow(2.0, band.toDouble())).toInt()
            } catch (e: Exception) {
                Timber.e(e, "Failed to get center frequency for band $band")
                (31 * Math.pow(2.0, band.toDouble())).toInt()
            }
            if (freq >= 1000) "${(freq / 1000.0).toString().take(3)}kHz" else "${freq}Hz"
        }
        frequencyLabels.tryEmit(frequencies)

        val bandLevelRange = try {
            equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get band level range")
            shortArrayOf(-1500, 1500)
        }
        audioEffects.value?.gainValues?.take(minOf(numberOfBands, activeBands))?.forEachIndexed { index, value ->
            try {
                val bandLevel = (value * 1000).toInt().coerceIn(
                    bandLevelRange[0].toInt(),
                    bandLevelRange[1].toInt()
                ).toShort()
                equalizer?.setBandLevel(index.toShort(), bandLevel)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Failed to set band level for band $index")
                equalizer?.setBandLevel(index.toShort(), 0)
            } catch (e: UnsupportedOperationException) {
                Timber.e(e, "Unsupported operation for band $index")
                equalizerError.tryEmit("Equalizer operation not supported. Try disabling system equalizer.")
                equalizer?.release()
                equalizer = null
                enableTenBand.tryEmit(false)
                enableEqualizer.tryEmit(false)
                return
            }
        }

        Timber.d("CheckAudioEffects -> \nId: $audioSessionId\nNo. Bands: $numberOfBands\nLower Band: ${bandLevelRange[0]}\nUpper Band: ${bandLevelRange[1]}\nFrequencies: $frequencies")
    }

    fun onSelectPreset(presetPosition: Int) {
        if (audioEffects.value == null || equalizer == null) return

        val numberOfBands = equalizer?.numberOfBands?.toInt() ?: 5
        val activeBands = if (enableTenBand.value) 10 else 5
        val effectiveBands = minOf(numberOfBands, activeBands)
        val gain = if (presetPosition == AppConstants.PRESET_CUSTOM) {
            ArrayList(audioEffects.value!!.gainValues.take(effectiveBands))
        } else {
            ArrayList(getPresetGainValue(presetPosition, activeBands).take(effectiveBands))
        }

        val newAudioEffects = AudioEffects(presetPosition, gain)
        audioEffects.tryEmit(newAudioEffects)
        viewModelScope.launch {
            setEqTypeUseCase.invoke(newAudioEffects)
        }

        val bandLevelRange = try {
            equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get band level range")
            shortArrayOf(-1500, 1500)
        }
        gain.forEachIndexed { index, value ->
            try {
                val bandLevel = (value * 1000).toInt().coerceIn(
                    bandLevelRange[0].toInt(),
                    bandLevelRange[1].toInt()
                ).toShort()
                equalizer?.setBandLevel(index.toShort(), bandLevel)
            } catch (e: Exception) {
                Timber.e(e, "Failed to set band level for preset, band $index")
            }
        }
    }

    fun onBandLevelChanged(changedBand: Int, newGainValue: Int) {
        if (equalizer == null) return
        val numberOfBands = equalizer?.numberOfBands?.toInt() ?: 5
        val activeBands = if (enableTenBand.value) 10 else 5
        if (changedBand >= minOf(numberOfBands, activeBands)) return

        val bandLevelRange = try {
            equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get band level range")
            shortArrayOf(-1500, 1500)
        }
        val bandLevel = newGainValue.coerceIn(
            bandLevelRange[0].toInt(),
            bandLevelRange[1].toInt()
        )
        try {
            equalizer?.setBandLevel(changedBand.toShort(), bandLevel.toShort())
            val list = ArrayList(audioEffects.value?.gainValues ?: List(activeBands) { 0.0 })
            list[changedBand] = bandLevel.toDouble() / 1000
            val newAudioEffects = AudioEffects(AppConstants.PRESET_CUSTOM, list)
            audioEffects.tryEmit(newAudioEffects)
            viewModelScope.launch {
                setEqTypeUseCase.invoke(newAudioEffects)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set band level for band $changedBand")
        }
    }

    fun toggleEqualizer() {
        val newState = !enableEqualizer.value
        enableEqualizer.tryEmit(newState)
        if (newState) {
            enableTenBand.tryEmit(false)
            equalizerError.tryEmit(null) // Clear error when switching modes
        }
        equalizer?.enabled = newState || enableTenBand.value
        viewModelScope.launch {
            setEqualizerEnabledUseCase.invoke(newState || enableTenBand.value)
            if (!newState && !enableTenBand.value) {
                val newAudioEffects = AudioEffects(AppConstants.PRESET_FLAT, AppConstants.FLAT)
                audioEffects.tryEmit(newAudioEffects)
                setEqTypeUseCase.invoke(newAudioEffects)
            } else if (newState) {
                val newAudioEffects = AudioEffects(AppConstants.PRESET_FLAT, AppConstants.FLAT)
                audioEffects.tryEmit(newAudioEffects)
                setEqTypeUseCase.invoke(newAudioEffects)
                onStart()
            }
        }
    }

    fun toggleTenBand() {
        val newState = !enableTenBand.value
        enableTenBand.tryEmit(newState)
        if (newState) {
            enableEqualizer.tryEmit(false)
            equalizerError.tryEmit(null) // Clear error when switching modes
        }
        equalizer?.enabled = newState || enableEqualizer.value
        viewModelScope.launch {
            setEqualizerEnabledUseCase.invoke(newState || enableTenBand.value)
            if (!newState && !enableEqualizer.value) {
                val newAudioEffects = AudioEffects(AppConstants.PRESET_FLAT, AppConstants.FLAT)
                audioEffects.tryEmit(newAudioEffects)
                setEqTypeUseCase.invoke(newAudioEffects)
            } else if (newState) {
                val newAudioEffects = AudioEffects(AppConstants.PRESET_FLAT, AppConstants.FLAT_10)
                audioEffects.tryEmit(newAudioEffects)
                setEqTypeUseCase.invoke(newAudioEffects)
                onStart()
            }
        }
    }

    fun retryEqualizer() {
        equalizerError.tryEmit(null)
        isTenBandSupported.tryEmit(true) // Allow retry
        if (enableTenBand.value) {
            onStart()
        } else if (enableEqualizer.value) {
            onStart()
        }
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
        equalizer?.release()
        equalizer = null
        unbindSystemEqualizer()
        super.onCleared()
    }
}