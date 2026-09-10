package com.tasnimulhasan.featureplayer

import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
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
 * Owns the sleep timer independently of any single screen's lifecycle. See prior doc comment
 * for the "why a Singleton" rationale - unchanged. Only the playback source changed: this now
 * reads live position/duration through [PlayerUseCases.getPlaybackSnapshot] (repository/use-case
 * layer) instead of injecting MelodiqServiceHandler directly.
 */
@Singleton
class SleepTimerController @Inject constructor(
    private val playerUseCases: PlayerUseCases,
) {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickingJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

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

    fun startEndOfSong(onFinished: () -> Unit) {
        tickingJob?.cancel()
        _isRunning.value = true
        tickingJob = controllerScope.launch {
            val initialTrackIndex = playerUseCases.getPlaybackSnapshot().currentIndex
            while (_isRunning.value) {
                val snapshot = playerUseCases.getPlaybackSnapshot()
                val remaining = (snapshot.duration - snapshot.position).coerceAtLeast(0L)

                val trackHasChanged = snapshot.currentIndex != initialTrackIndex
                val trackHasEnded = snapshot.duration > 0L && remaining <= 500L

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