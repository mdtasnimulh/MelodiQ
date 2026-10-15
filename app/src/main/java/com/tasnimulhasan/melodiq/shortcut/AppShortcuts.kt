package com.tasnimulhasan.melodiq.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.melodiq.MainActivity
import com.tasnimulhasan.melodiq.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Launcher shortcuts (long-press the app icon): Shuffle all, Favourites, Continue listening.
 *
 * They are published as DYNAMIC shortcuts on purpose. Static shortcuts must be declared on the
 * launcher activity, and MelodiQ's launcher entries are the eight icon `activity-alias`es
 * (Settings > App icon) that get enabled/disabled - a dynamic shortcut follows whichever one
 * is currently enabled with no manifest changes.
 */
object AppShortcuts {
    const val ACTION_SHUFFLE_ALL = "com.tasnimulhasan.melodiq.action.SHUFFLE_ALL"
    const val ACTION_FAVOURITES = "com.tasnimulhasan.melodiq.action.FAVOURITES"
    const val ACTION_CONTINUE = "com.tasnimulhasan.melodiq.action.CONTINUE"

    fun publish(context: Context) {
        runCatching {
            val manager = context.getSystemService(ShortcutManager::class.java) ?: return
            manager.dynamicShortcuts = listOf(
                build(context, "continue", "Continue listening", ACTION_CONTINUE, R.drawable.ic_shortcut_play, 0),
                build(context, "shuffle", "Shuffle all", ACTION_SHUFFLE_ALL, R.drawable.ic_shortcut_shuffle, 1),
                build(context, "favourites", "Favourites", ACTION_FAVOURITES, R.drawable.ic_shortcut_favourite, 2),
            )
        }
    }

    private fun build(context: Context, id: String, label: String, action: String, iconRes: Int, rank: Int): ShortcutInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        // Bitmap icon: renders the same on every launcher, unlike a vector resource reference.
        val bitmap = ContextCompat.getDrawable(context, iconRes)!!.toBitmap(192, 192)
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(Icon.createWithBitmap(bitmap))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }

    fun isShortcutAction(action: String?): Boolean =
        action == ACTION_SHUFFLE_ALL || action == ACTION_FAVOURITES || action == ACTION_CONTINUE

    /**
     * Runs a shortcut. Must be called on the main thread (the player lives there).
     * Returns true when something started playing, so the caller can open the player screen.
     */
    suspend fun execute(
        action: String,
        player: PlayerUseCases,
        observeFavouriteIds: ObserveFavouriteIdsUseCase,
    ): Boolean {
        runCatching { player.ensurePlaybackServiceStarted() }
        return when (action) {
            ACTION_SHUFFLE_ALL -> {
                val library = awaitLibrary(player)
                if (library.isEmpty()) false else {
                    player.playCuratedQueue(library.shuffled(), 0)
                    true
                }
            }
            ACTION_FAVOURITES -> {
                val ids = observeFavouriteIds().first()
                val favourites = awaitLibrary(player).filter { it.songId in ids }
                if (favourites.isEmpty()) false else {
                    player.playCuratedQueue(favourites.shuffled(), 0)
                    true
                }
            }
            ACTION_CONTINUE -> {
                // After a cold start the last played track is restored asynchronously.
                val current = withTimeoutOrNull(3_000.milliseconds) {
                    player.observeCurrentSelectedAudio().first { it != null }
                }
                if (current != null) {
                    player.play()
                    true
                } else {
                    // Nothing played before: fall back to shuffling the library.
                    execute(ACTION_SHUFFLE_ALL, player, observeFavouriteIds)
                }
            }
            else -> false
        }
    }

    private suspend fun awaitLibrary(player: PlayerUseCases): List<MusicEntity> {
        if (player.observeAudioList().value.isEmpty()) {
            withTimeoutOrNull(3_000.milliseconds) { player.observeAudioList().first { it.isNotEmpty() } }
        }
        return player.observeAudioList().value
    }
}
