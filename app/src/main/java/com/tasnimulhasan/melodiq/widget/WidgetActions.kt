package com.tasnimulhasan.melodiq.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * A widget tap can arrive while the app process is dead. Starting the process rebuilds the
 * player and restores the last played track asynchronously, so wait (briefly) for a current
 * song before sending a command, otherwise play/next would hit an empty player and do nothing.
 */
private suspend fun PlayerUseCases.prepareForCommand() {
    runCatching { ensurePlaybackServiceStarted() }
    if (observeCurrentSelectedAudio().value == null) {
        withTimeoutOrNull(2_500.milliseconds) { observeCurrentSelectedAudio().first { it != null } }
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val useCases = context.widgetEntryPoint().playerUseCases()
        // The ExoPlayer lives on the main thread, same as every call from the UI.
        withContext(Dispatchers.Main) {
            useCases.prepareForCommand()
            if (useCases.observeIsPlaying().value) useCases.pause() else useCases.play()
        }
        WidgetUpdater.updateAll(context)
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val useCases = context.widgetEntryPoint().playerUseCases()
        withContext(Dispatchers.Main) {
            useCases.prepareForCommand()
            useCases.next()
        }
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val useCases = context.widgetEntryPoint().playerUseCases()
        withContext(Dispatchers.Main) {
            useCases.prepareForCommand()
            useCases.previous()
        }
    }
}

class ToggleFavouriteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entry = context.widgetEntryPoint()
        val songId = entry.playerUseCases().observeCurrentSelectedAudio().value?.songId ?: return
        entry.favouriteRepository().toggleFavourite(songId)
        WidgetUpdater.updateAll(context)
    }
}

internal val SongIdKey = ActionParameters.Key<Long>("songId")

/** Plays the tapped song from the full library (same as tapping it in Songs/Home). */
class PlaySongAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val songId = parameters[SongIdKey] ?: return
        val useCases = context.widgetEntryPoint().playerUseCases()
        withContext(Dispatchers.Main) {
            runCatching { useCases.ensurePlaybackServiceStarted() }
            // After a cold start the library list fills in asynchronously.
            if (useCases.observeAudioList().value.isEmpty()) {
                withTimeoutOrNull(3_000.milliseconds) { useCases.observeAudioList().first { it.isNotEmpty() } }
            }
            val index = useCases.observeAudioList().value.indexOfFirst { it.songId == songId }
            if (index >= 0) useCases.selectAudioChange(index)
        }
        WidgetUpdater.updateAll(context)
    }
}
