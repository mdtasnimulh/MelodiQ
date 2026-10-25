package com.tasnimulhasan.radio.data

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tasnimulhasan.domain.localusecase.player.PauseUseCase
import com.tasnimulhasan.radio.domain.FmBand
import com.tasnimulhasan.radio.domain.RadioEngine
import com.tasnimulhasan.radio.domain.RadioError
import com.tasnimulhasan.radio.domain.RadioMode
import com.tasnimulhasan.radio.domain.RadioState
import com.tasnimulhasan.radio.domain.RadioStation
import com.tasnimulhasan.radio.domain.RadioStations
import com.tasnimulhasan.radio.domain.RadioStatus
import com.tasnimulhasan.radio.service.RadioPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single owner of radio playback. It decides which engine ([InternetRadioPlayer] or
 * [HardwareRadioTuner]) is allowed to make sound and keeps one [RadioState] that the screen, the
 * media session and the notification all read.
 *
 * Rules it enforces:
 *  - Only one engine is ever active. Starting the hardware tuner first stops and RELEASES the
 *    internet player (connection closed, buffers dropped); starting the stream first stops the
 *    tuner and closes it.
 *  - The app's music player is paused before either engine starts, so they never overlap. If the
 *    music player (or another app) takes audio focus later, each engine's own focus handling pauses
 *    the radio.
 *  - Hardware FM refuses to start without wired headphones (the antenna), and pauses with an error
 *    the moment they are unplugged.
 *  - While radio is audible a foreground [RadioPlaybackService] keeps the process alive and shows
 *    the media notification.
 *
 * All commands are fire-and-forget and serialised by a mutex, so rapid taps cannot interleave an
 * engine switch. Everything runs on the main thread, which both engines require.
 */
@Singleton
class RadioController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val internet: InternetRadioPlayer,
    private val hardware: HardwareRadioTuner,
    private val headset: WiredHeadsetMonitor,
    private val pauseMusic: PauseUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()

    private val _state = MutableStateFlow(
        RadioState(
            stationName = RadioStations.defaults.first().name,
            stationUrl = RadioStations.defaults.first().url,
            headphonesPlugged = headset.isPlugged.value,
        )
    )
    val state: StateFlow<RadioState> = _state.asStateFlow()

    init {
        scope.launch {
            internet.state.collect { s ->
                _state.update { cur ->
                    if (cur.mode != RadioMode.INTERNET) cur
                    else cur.copy(
                        status = s.status,
                        nowPlaying = s.nowPlaying,
                        error = s.error?.let { RadioError.StreamFailed(it) }
                            ?: cur.error.takeUnless { it is RadioError.StreamFailed },
                    )
                }
            }
        }
        scope.launch {
            hardware.state.collect { s ->
                _state.update { cur ->
                    if (cur.mode != RadioMode.HARDWARE) cur
                    else cur.copy(
                        status = s.status,
                        frequencyKhz = s.frequencyKhz,
                        error = s.error ?: cur.error.takeIf { it is RadioError.HeadphonesUnplugged },
                    )
                }
            }
        }
        scope.launch {
            headset.isPlugged.collect { plugged -> onHeadsetChanged(plugged) }
        }
    }

    // ---- commands -----------------------------------------------------------------------------

    /** Switches engine. Audio that was playing carries on in the new engine. */
    fun selectMode(mode: RadioMode) {
        scope.launch { lock.withLock { switchTo(mode) } }
    }

    /** Picks [station] and plays it (Internet mode). */
    fun playStation(station: RadioStation) = playUrl(station.name, station.url)

    /** Plays any stream URL the user typed. */
    fun playCustomUrl(rawUrl: String) {
        val url = rawUrl.trim()
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
            _state.update { it.copy(error = RadioError.StreamFailed("Enter a full stream address starting with https://")) }
            return
        }
        playUrl("Custom stream", url)
    }

    /** Moves the dial. Updates the display at once; the chip is retuned by [commitFrequency]. */
    fun setFrequency(khz: Int) {
        _state.update { it.copy(frequencyKhz = FmBand.snap(khz)) }
    }

    /** Retunes the chip to the dial position when the hardware engine is active. */
    fun commitFrequency() {
        scope.launch {
            lock.withLock {
                val s = _state.value
                if (s.mode == RadioMode.HARDWARE && s.status != RadioStatus.IDLE) {
                    hardware.tune(s.frequencyKhz)
                }
            }
        }
    }

    fun play() {
        scope.launch { lock.withLock { startOrResume() } }
    }

    fun pause() {
        scope.launch { lock.withLock { pauseActive() } }
    }

    fun resume() = play()

    fun togglePlayPause() {
        scope.launch {
            lock.withLock {
                when (_state.value.status) {
                    RadioStatus.PLAYING, RadioStatus.BUFFERING -> pauseActive()
                    else -> startOrResume()
                }
            }
        }
    }

    fun stop() {
        scope.launch { lock.withLock { stopActive() } }
    }

    fun clearError() {
        internet.clearError()
        hardware.clearError()
        _state.update { cur ->
            // The unplugged warning is a live condition, not an event: it stays while nothing is plugged in.
            val keep = cur.mode == RadioMode.HARDWARE && !cur.headphonesPlugged
            cur.copy(error = if (keep) RadioError.HeadphonesUnplugged else null)
        }
    }

    // ---- internals (always under the lock) ----------------------------------------------------

    private fun playUrl(name: String, url: String) {
        scope.launch {
            lock.withLock {
                _state.update { it.copy(stationName = name, stationUrl = url, nowPlaying = null, error = null) }
                if (_state.value.mode != RadioMode.INTERNET) switchTo(RadioMode.INTERNET, autoStart = false)
                start(RadioEngine.Internet(url))
            }
        }
    }

    private suspend fun switchTo(mode: RadioMode, autoStart: Boolean = true) {
        val before = _state.value
        if (before.mode == mode) return
        val wasAudible = before.status == RadioStatus.PLAYING || before.status == RadioStatus.BUFFERING
        when (mode) {
            RadioMode.HARDWARE -> {
                // Enabling the tuner pauses ExoPlayer and releases its network resources first.
                internet.stop()
                headset.acquire()
                _state.update {
                    it.copy(
                        mode = RadioMode.HARDWARE, status = RadioStatus.IDLE, nowPlaying = null,
                        headphonesPlugged = headset.isPlugged.value,
                        error = if (headset.isPlugged.value) null else RadioError.HeadphonesUnplugged,
                    )
                }
            }
            RadioMode.INTERNET -> {
                hardware.stop()
                headset.release()
                _state.update { it.copy(mode = RadioMode.INTERNET, status = RadioStatus.IDLE, nowPlaying = null, error = null) }
            }
        }
        if (autoStart && wasAudible) startOrResume()
    }

    private suspend fun startOrResume() {
        val s = _state.value
        when (s.mode) {
            RadioMode.INTERNET -> {
                val url = s.stationUrl
                if (url == null) {
                    _state.update { it.copy(error = RadioError.StreamFailed("Choose a station first.")) }
                    return
                }
                runCatching { pauseMusic() }
                if (s.status == RadioStatus.PAUSED && internet.resume()) return
                start(RadioEngine.Internet(url))
            }
            RadioMode.HARDWARE -> {
                if (s.status == RadioStatus.PAUSED && headset.isPlugged.value) {
                    runCatching { pauseMusic() }
                    if (hardware.resume()) ensureService()
                    return
                }
                start(RadioEngine.Hardware(s.frequencyKhz))
            }
        }
    }

    private suspend fun start(engine: RadioEngine) {
        runCatching { pauseMusic() }
        when (engine) {
            is RadioEngine.Internet -> {
                hardware.stop()
                ensureService()
                internet.play(engine.url)
            }
            is RadioEngine.Hardware -> {
                internet.stop()
                if (!headset.isPlugged.value) {
                    _state.update { it.copy(status = RadioStatus.IDLE, error = RadioError.HeadphonesUnplugged) }
                    return
                }
                if (hardware.start(engine.frequencyKhz)) ensureService()
            }
        }
    }

    private fun pauseActive() {
        when (_state.value.mode) {
            RadioMode.INTERNET -> internet.pause()
            RadioMode.HARDWARE -> hardware.pause()
        }
    }

    private fun stopActive() {
        internet.stop()
        hardware.stop()
        _state.update { it.copy(status = RadioStatus.IDLE, nowPlaying = null) }
    }

    private fun onHeadsetChanged(plugged: Boolean) {
        _state.update { it.copy(headphonesPlugged = plugged) }
        if (_state.value.mode != RadioMode.HARDWARE) return
        if (!plugged) {
            // Antenna gone: silence the tuner now and tell the user.
            if (_state.value.status != RadioStatus.IDLE) hardware.pause()
            _state.update { it.copy(error = RadioError.HeadphonesUnplugged) }
        } else if (_state.value.error is RadioError.HeadphonesUnplugged) {
            // Plugged back in: the warning goes away; playback restarts only when the user presses play.
            _state.update { it.copy(error = null) }
        }
    }

    private fun ensureService() {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, RadioPlaybackService::class.java))
        }
    }
}
