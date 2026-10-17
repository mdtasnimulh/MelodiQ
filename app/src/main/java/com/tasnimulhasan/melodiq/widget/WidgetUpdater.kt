package com.tasnimulhasan.melodiq.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.tasnimulhasan.domain.localusecase.library.LibraryUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.repository.local.FavouriteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the home screen widgets in step with the player. Started once from the Application;
 * it observes the same StateFlows the mini player uses and re-renders the widgets whenever the
 * song, play/pause state or the song's favourite flag changes. Only those fields are compared
 * (not the whole MusicEntity) and updates are debounced, so a burst of events - e.g. a skip,
 * which changes song and play state together - costs a single widget redraw.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerUseCases: PlayerUseCases,
    private val favouriteRepository: FavouriteRepository,
    private val libraryUseCases: LibraryUseCases,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    private data class Snapshot(
        val songId: Long?,
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean,
        val isFavourite: Boolean,
        // Whole set, so the Favourites widget also refreshes when a non-current song is hearted.
        val favouriteIds: Set<Long>,
        // The play is written to history a moment AFTER the song changes, so the Recently
        // Played widget must also react to the history itself, not just to the song.
        val recentTop: List<Long>,
        val libraryCount: Int,
    )

    @OptIn(FlowPreview::class)
    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(
                playerUseCases.observeCurrentSelectedAudio(),
                playerUseCases.observeIsPlaying(),
                favouriteRepository.observeFavouriteIds(),
                libraryUseCases.observeRecentlyPlayedWithTimestamp(5),
                playerUseCases.observeAudioList(),
            ) { song, playing, favourites, recent, library ->
                Snapshot(
                    songId = song?.songId,
                    title = song?.songTitle,
                    artist = song?.artist,
                    isPlaying = playing,
                    isFavourite = song != null && favourites.contains(song.songId),
                    favouriteIds = favourites,
                    recentTop = recent.map { it.first.songId },
                    libraryCount = library.size,
                )
            }
                .distinctUntilChanged()
                .debounce(250)
                .collect { updateAll(context) }
        }
    }

    companion object {
        /** Cheap synchronous check used at app start. */
        fun hasPlacedWidgets(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            return listOf(
                MelodiqPlayerWidgetReceiver::class.java,
                MelodiqCompactWidgetReceiver::class.java,
                MelodiqCoverWidgetReceiver::class.java,
                MelodiqRecentWidgetReceiver::class.java,
                MelodiqFavouritesWidgetReceiver::class.java,
                MelodiqIpodDarkWidgetReceiver::class.java,
                MelodiqIpodSilverWidgetReceiver::class.java,
            )
                .any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
        }

        /** Redraws every placed MelodiQ widget (no-op when none are on the home screen). */
        suspend fun updateAll(context: Context) {
            safely { MelodiqPlayerWidget().updateAll(context) }
            safely { MelodiqCompactWidget().updateAll(context) }
            safely { MelodiqCoverWidget().updateAll(context) }
            safely { MelodiqRecentWidget().updateAll(context) }
            safely { MelodiqFavouritesWidget().updateAll(context) }
            safely { MelodiqIpodDarkWidget().updateAll(context) }
            safely { MelodiqIpodSilverWidget().updateAll(context) }
        }

        private suspend inline fun safely(block: () -> Unit) {
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                timber.log.Timber.w(e, "Widget update failed")
            }
        }
    }
}
