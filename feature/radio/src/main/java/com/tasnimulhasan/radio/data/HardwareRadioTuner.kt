package com.tasnimulhasan.radio.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.hardware.radio.ProgramSelector
import android.hardware.radio.RadioManager
import android.hardware.radio.RadioTuner
import com.tasnimulhasan.radio.domain.FmBand
import com.tasnimulhasan.radio.domain.RadioError
import com.tasnimulhasan.radio.domain.RadioStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware FM engine: the phone's FM tuner chip through android.hardware.radio.
 *
 * HARDWARE CONSTRAINTS (read before changing anything here)
 *  1. RadioManager, RadioTuner and ProgramSelector are @SystemApi. They are not in the public SDK
 *     and opening a tuner needs android.permission.ACCESS_BROADCAST_RADIO, a signature|privileged
 *     permission that only system / privileged apps (or a custom ROM) are granted. On an ordinary
 *     Play Store install the service is null or openTuner throws SecurityException. That is an
 *     expected outcome, not a bug: it is reported as [RadioError.HardwareUnavailable] and the UI
 *     explains it.
 *  2. Because the classes are hidden, every call can fail with a SecurityException, NoSuchMethodError
 *     or other LinkageError on some builds. All access is therefore inside try/catch(Throwable).
 *  3. FM needs the wired headphone cable as its antenna. This class does not check for it; the
 *     RadioController refuses to start the tuner (and pauses it) when [WiredHeadsetMonitor] says
 *     nothing is plugged in. [RadioTuner.Callback.onAntennaState] is also honoured.
 *  4. The compile-time types come from the :feature:radio-stubs module (compileOnly). The real
 *     framework classes are used on the device.
 *
 * AUDIO FOCUS: the tuner HAL plays straight to the audio output, so Android does not manage focus
 * for it. This class asks for AUDIOFOCUS_GAIN itself and obeys the answer by muting the tuner:
 * permanent loss -> pause; transient loss -> mute and resume on gain. Ducking is ignored because
 * the tuner has no volume control (a ducking request keeps playing at full level).
 */
@Singleton
class HardwareRadioTuner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class State(
        val status: RadioStatus = RadioStatus.IDLE,
        val frequencyKhz: Int = FmBand.DEFAULT_KHZ,
        val error: RadioError? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var tuner: RadioTuner? = null
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false

    // ---- public API (main thread) -------------------------------------------------------------

    /** Opens the tuner if needed, takes audio focus and tunes to [frequencyKhz]. Returns true on success. */
    fun start(frequencyKhz: Int): Boolean {
        val khz = FmBand.snap(frequencyKhz)
        _state.value = State(status = RadioStatus.BUFFERING, frequencyKhz = khz)
        if (!requestFocus()) {
            return fail(RadioError.FocusDenied)
        }
        val open = tuner ?: openTuner() ?: run {
            abandonFocus()
            return false
        }
        return tuneOn(open, khz) && unmute(open)
    }

    /** Retunes without closing the tuner. Used while the dial moves. */
    fun tune(frequencyKhz: Int): Boolean {
        val khz = FmBand.snap(frequencyKhz)
        val open = tuner ?: return false
        _state.value = _state.value.copy(frequencyKhz = khz)
        return tuneOn(open, khz)
    }

    /** Mutes the tuner and releases audio focus, keeping the tuner open and tuned. */
    fun pause() {
        val open = tuner ?: return
        resumeOnFocusGain = false
        runCatching { open.setMute(true) }
        abandonFocus()
        _state.value = _state.value.copy(status = RadioStatus.PAUSED)
    }

    /** Takes focus again and unmutes. */
    fun resume(): Boolean {
        val open = tuner ?: return false
        if (!requestFocus()) return fail(RadioError.FocusDenied)
        return unmute(open)
    }

    /** Mutes, closes the tuner and gives audio focus back. Safe to call at any time. */
    fun stop() {
        resumeOnFocusGain = false
        tuner?.let {
            runCatching { it.setMute(true) }
            runCatching { it.close() }
        }
        tuner = null
        abandonFocus()
        _state.value = State(frequencyKhz = _state.value.frequencyKhz)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ---- tuner access -------------------------------------------------------------------------

    private fun openTuner(): RadioTuner? {
        try {
            // Context.BROADCAST_RADIO_SERVICE is hidden too, so its value is used directly.
            val manager = context.getSystemService(BROADCAST_RADIO_SERVICE) as? RadioManager
            if (manager == null) {
                unavailable("RadioManager is null. This device has no FM tuner exposed to apps, or the broadcast radio service is restricted.")
                return null
            }
            val modules = ArrayList<RadioManager.ModuleProperties>()
            val status = manager.listModules(modules)
            if (status != RadioManager.STATUS_OK || modules.isEmpty()) {
                unavailable("No FM tuner module is reported by the system (status $status).")
                return null
            }
            val module = modules.firstOrNull { m -> m.bands.any { it.type == RadioManager.BAND_FM } }
                ?: modules.first()
            val opened = manager.openTuner(
                module.id,
                /* config = */ null,
                /* withAudio = */ true,
                callback,
                Handler(Looper.getMainLooper()),
            )
            if (opened == null) {
                unavailable("The FM tuner could not be opened. It may be in use by another app.")
                return null
            }
            tuner = opened
            return opened
        } catch (e: SecurityException) {
            Timber.w(e, "ACCESS_BROADCAST_RADIO not granted")
            unavailable("Permission denied. Opening the FM tuner needs the privileged ACCESS_BROADCAST_RADIO permission, which only system apps can hold.")
        } catch (e: Throwable) {
            Timber.w(e, "Hardware FM tuner failed to open")
            unavailable("The hidden FM radio API is not available on this build (${e.javaClass.simpleName}).")
        }
        return null
    }

    private fun tuneOn(open: RadioTuner, khz: Int): Boolean = try {
        // BAND_INVALID lets the framework infer AM/FM from the frequency. FM ids are in kHz.
        val result = open.tune(ProgramSelector.createAmFmSelector(RadioManager.BAND_INVALID, khz))
        if (result == RadioManager.STATUS_OK) {
            _state.value = _state.value.copy(frequencyKhz = khz, error = null)
            true
        } else {
            fail(RadioError.TuneFailed("The tuner refused $khz kHz (status $result)."))
        }
    } catch (e: Throwable) {
        Timber.w(e, "tune failed")
        fail(RadioError.TuneFailed("Tuning failed (${e.javaClass.simpleName})."))
    }

    private fun unmute(open: RadioTuner): Boolean = try {
        open.setMute(false)
        _state.value = _state.value.copy(status = RadioStatus.PLAYING, error = null)
        true
    } catch (e: Throwable) {
        Timber.w(e, "unmute failed")
        fail(RadioError.HardwareUnavailable("The tuner could not be unmuted (${e.javaClass.simpleName})."))
    }

    private val callback = object : RadioTuner.Callback() {
        override fun onError(status: Int) {
            fail(RadioError.HardwareUnavailable("The tuner reported error $status."))
        }

        override fun onTuneFailed(status: Int, selector: ProgramSelector?) {
            fail(RadioError.TuneFailed("Could not lock onto the station (status $status)."))
        }

        override fun onProgramInfoChanged(info: RadioManager.ProgramInfo) {
            // The tuner confirms the frequency it really locked on to (kHz for FM).
            runCatching { info.selector.primaryId.value.toInt() }
                .getOrNull()
                ?.takeIf { it in FmBand.MIN_KHZ..FmBand.MAX_KHZ }
                ?.let { _state.value = _state.value.copy(frequencyKhz = it) }
        }

        override fun onAntennaState(connected: Boolean) {
            // No antenna means the headphone cable was pulled out.
            if (!connected && _state.value.status == RadioStatus.PLAYING) {
                pause()
                _state.value = _state.value.copy(error = RadioError.HeadphonesUnplugged)
            }
        }
    }

    // ---- audio focus --------------------------------------------------------------------------

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        val open = tuner ?: return@OnAudioFocusChangeListener
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.status == RadioStatus.PLAYING) {
                    resumeOnFocusGain = true
                    runCatching { open.setMute(true) }
                    _state.value = _state.value.copy(status = RadioStatus.PAUSED)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocusGain) {
                resumeOnFocusGain = false
                unmute(open)
            }
            // AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: the tuner has no volume control, keep playing.
        }
    }

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return false
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { request -> runCatching { audioManager?.abandonAudioFocusRequest(request) } }
    }

    // ---- state helpers ------------------------------------------------------------------------

    private fun unavailable(reason: String) {
        fail(RadioError.HardwareUnavailable(reason))
    }

    private fun fail(error: RadioError): Boolean {
        _state.value = _state.value.copy(status = RadioStatus.IDLE, error = error)
        return false
    }

    private companion object {
        const val BROADCAST_RADIO_SERVICE = "broadcastradio"
    }
}
