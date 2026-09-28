package com.tasnimulhasan.data.library

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fired whenever a song's underlying file changed outside the normal MediaStore-observer
 * path this app already reacts to - specifically, right after a rename/move/delete/tag-edit
 * this app itself performed. Those changes go through a content:// Uri and often complete
 * fast enough, or in a way, that the debounced ContentObserver in MediaStoreLibraryScanner
 * doesn't reliably win the race before the user navigates back to a list showing the old
 * data (or, worse, taps a now-deleted song and hits a playback error).
 *
 * MusicRepoImpl's raw in-memory cache had no invalidation path at all before this - once
 * populated, it never changed for the lifetime of the process, so a deleted song stayed
 * "playable" (pointing at a Uri MediaStore no longer had a row for) until the app restarted.
 */
@Singleton
class LibraryChangeNotifier @Inject constructor() {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    /** Bumped BEFORE the signal is emitted. Consumers that cache (MusicRepoImpl) compare
     * this against the version they cached at, so invalidation is deterministic rather than
     * depending on which collector happens to run first. */
    @Volatile
    var version: Long = 0L
        private set

    fun notifyChanged() {
        version += 1
        _changes.tryEmit(Unit)
    }
}
