package com.tasnimulhasan.domain.lyrics

/** Result of a successful online lookup - kept provider-agnostic so callers never need to
 * know which service answered. */
data class OnlineLyricsResult(
    val rawLyrics: String,
    val isSynced: Boolean,
    val providerName: String,
)

/**
 * Abstraction over "some online lyrics service". LyricsRepository depends on this interface,
 * not a concrete implementation, so the provider can be swapped (or a fallback chain of
 * several added) without touching the resolution-priority logic.
 */
interface LyricsProvider {
    val name: String

    /** Returns null on "not found" as well as on any network/parse failure - the repository
     * treats both the same way (nothing to show, try nothing further). */
    suspend fun fetch(title: String, artist: String, album: String?, durationMs: Long): OnlineLyricsResult?
}
