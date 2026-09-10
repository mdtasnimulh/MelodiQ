package com.tasnimulhasan.featureplayer

import com.tasnimulhasan.common.service.MelodiqServiceHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Owns the sleep timer independently of any single screen's lifecycle.
 *
 * `PlayerScreen` is a standard nav-graph destination, so `PlayerViewModel` (obtained via the
 * default `hiltViewModel()`) is scoped to that destination's back-stack entry: navigating away
 * from the Player screen pops that entry, clears the ViewModel, and cancels its
 * `viewModelScope` - which is exactly what was resetting the sleep timer every time the user
 * left and came back to the Player screen.
 *
 * This class is a Hilt `@Singleton` instead: the same instance (and the coroutine driving the
 * countdown) is shared by every `PlayerViewModel` for as long as the app process is alive, so
 * the countdown keeps running - and still fires its end action - no matter which screen is on
 * top when it finishes. Reopening the Player screen just re-reads whatever this controller's
 * current state is.
 *
 * Two independent modes:
 * - [start]: a fixed wall-clock countdown (the preset/custom durations). Unaffected by seeking
 *   within the current track, which is correct - "30 minutes from now" shouldn't change just
 *   because the user skipped around in a song.
 * - [startEndOfSong]: instead of freezing a duration at selection time (which went stale the
 *   moment the user seeked/skipped), this re-reads the live playback position and track
 *   duration from [audioServiceHandler] on every tick, so seeking forward or back immediately
 *   changes how long is actually left - matching what "end of song" should mean.
 */
@Singleton
class SleepTimerController @Inject constructor(
    private val audioServiceHandler: MelodiqServiceHandler,
) {

    // Deliberately not tied to any ViewModel's viewModelScope - this scope must outlive
    // individual PlayerViewModel instances. Main.immediate keeps ticks (and the finish
    // callback, which touches playback) on the thread the player/ExoPlayer expects.
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickingJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    /**
     * Starts (or restarts) a fixed countdown for [totalDurationMillis]. [onFinished] runs
     * exactly once, right when the countdown reaches zero.
     */
    fun start(totalDurationMillis: Long, onFinished: () -> Unit) {
        if (totalDurationMillis <= 0L) return
        tickingJob?.cancel()
        val endAtMillis = System.currentTimeMillis() + totalDurationMillis
        _remainingMillis.value = totalDurationMillis
        _isRunning.value = true
        tickingJob = controllerScope.launch {
            while (_isRunning.value) {
                val remaining = endAtMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    finish(onFinished)
                } else {
                    _remainingMillis.value = remaining
                    delay(1000L.milliseconds)
                }
            }
        }
    }

    /**
     * Starts (or restarts) tracking the currently playing track to its end. Unlike [start],
     * there's no fixed target: every tick re-reads the actual track duration and playback
     * position, so seeking (forward or back) or pausing immediately changes the reported
     * remaining time instead of drifting out of sync with the real audio.
     *
     * Also detects the track itself changing (song ended naturally and auto-advanced, or the
     * user skipped to another track) as an end condition, since polling on a fixed interval
     * could otherwise miss the exact zero-crossing and start reading the *next* track's
     * (larger) remaining time instead of finishing.
     */
    fun startEndOfSong(onFinished: () -> Unit) {
        tickingJob?.cancel()
        _isRunning.value = true
        val initialTrackIndex = audioServiceHandler.getCurrentMediaItemIndex()
        tickingJob = controllerScope.launch {
            while (_isRunning.value) {
                val currentTrackIndex = audioServiceHandler.getCurrentMediaItemIndex()
                val totalDuration = audioServiceHandler.getDuration()
                val elapsed = audioServiceHandler.getCurrentDuration()
                val remaining = (totalDuration - elapsed).coerceAtLeast(0L)

                val trackHasChanged = currentTrackIndex != initialTrackIndex
                val trackHasEnded = totalDuration > 0L && remaining <= 500L

                if (trackHasChanged || trackHasEnded) {
                    finish(onFinished)
                } else {
                    _remainingMillis.value = remaining
                    delay(400L.milliseconds)
                }
            }
        }
    }

    fun cancel() {
        tickingJob?.cancel()
        tickingJob = null
        _isRunning.value = false
        _remainingMillis.value = 0L
    }

    private fun finish(onFinished: () -> Unit) {
        tickingJob?.cancel()
        tickingJob = null
        _remainingMillis.value = 0L
        _isRunning.value = false
        onFinished()
    }
}