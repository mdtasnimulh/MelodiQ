package com.tasnimulhasan.featureplayer

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
 * Owns the sleep timer countdown independently of any single screen's lifecycle.
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
 */
@Singleton
class SleepTimerController @Inject constructor() {

    // Deliberately not tied to any ViewModel's viewModelScope - this scope must outlive
    // individual PlayerViewModel instances. Main.immediate keeps the finish callback (which
    // ends up touching playback) on the thread the player expects.
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickingJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    /**
     * Starts (or restarts) counting down [totalDurationMillis]. [onFinished] runs exactly once,
     * right when the countdown reaches zero.
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
                    _remainingMillis.value = 0L
                    _isRunning.value = false
                    onFinished()
                } else {
                    _remainingMillis.value = remaining
                    delay(1000L.milliseconds)
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
}
