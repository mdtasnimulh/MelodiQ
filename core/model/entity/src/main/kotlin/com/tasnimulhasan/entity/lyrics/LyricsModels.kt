package com.tasnimulhasan.entity.lyrics

/** One line of lyrics. [timestampMs] is null for unsynchronized lyrics (plain text with no
 * per-line timing) - the UI falls back to a static scrollable view in that case. */
data class LyricLine(
    val timestampMs: Long?,
    val text: String,
)

enum class LyricsSource {
    EMBEDDED_TAG,
    LOCAL_LRC_FILE,
    CACHED_ONLINE,
    ONLINE,
}

/** Mirrors the resolution priority chain: embedded tag -> local .lrc -> cache -> online ->
 * unavailable. Every state the UI needs to render is represented explicitly rather than
 * inferring it from nulls/booleans. */
sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data class Found(val lines: List<LyricLine>, val isSynced: Boolean, val source: LyricsSource) : LyricsUiState
    /** No source had lyrics, and either there was no connectivity to try online, or trying
     * online was skipped/failed. [offline] distinguishes "we didn't even try" from "we tried
     * and truly found nothing", since the message the user should see differs. */
    data class Unavailable(val offline: Boolean) : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}
