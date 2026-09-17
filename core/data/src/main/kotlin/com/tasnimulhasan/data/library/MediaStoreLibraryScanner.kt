package com.tasnimulhasan.data.library

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.tasnimulhasan.database.dao.LibrarySongDao
import com.tasnimulhasan.entity.room.library.LibrarySongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps [LibrarySongDao]'s table in sync with the device's MediaStore audio collection.
 *
 * Two things make this "incremental" rather than a wipe-and-reload on every launch:
 *  1. [scanIfNeeded] diffs the fresh MediaStore query against what Room already has (by id
 *     + dateModified) and only touches rows that are new, changed, or gone - it never
 *     rewrites the whole table for a library that hasn't changed.
 *  2. A [ContentObserver] on the audio collection triggers a rescan automatically when the
 *     user adds/removes/edits a file, debounced so a batch of changes (e.g. a sync app
 *     copying 200 files) collapses into one rescan instead of hundreds.
 */
@Singleton
class MediaStoreLibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val librarySongDao: LibrarySongDao,
) {
    private val scannerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanMutex = Mutex()
    private var observerRegistered = false
    private var debounceJob: Job? = null

    /**
     * Runs a scan if the library table is empty (first launch, or a fresh install) OR
     * unconditionally when [force] is set (e.g. a user-triggered "rescan library" action).
     * Safe to call from multiple places - overlapping calls are serialized, not duplicated.
     */
    suspend fun scanIfNeeded(force: Boolean = false) {
        scanMutex.withLock {
            if (!force && librarySongDao.getTotalCount() > 0) return
            performScan()
        }
        startObservingChanges()
    }

    /** Registers the ContentObserver once per process. Safe to call before permission is
     * granted - registering is harmless; only the resulting query needs the permission. */
    private fun startObservingChanges() {
        if (observerRegistered) return
        observerRegistered = true
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // Debounce: MediaStore can fire many rapid notifications for one bulk
                // change (a sync app copying a folder of files). Collapse them into a
                // single rescan a couple of seconds after the last one, instead of
                // re-scanning the whole library dozens of times in a row.
                debounceJob?.cancel()
                debounceJob = scannerScope.launch {
                    delay(2_000)
                    scanMutex.withLock { performScan() }
                }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
    }

    private suspend fun performScan() = withContext(Dispatchers.IO) {
        val fresh = queryMediaStore()
        if (fresh.isEmpty()) {
            // Almost always means "no permission yet" rather than "no music" - never let an
            // empty/failed query wipe out a library that was already scanned successfully.
            return@withContext
        }

        val existing = librarySongDao.getAllIdsAndModified().associate { it.songId to it.dateModified }
        val freshIds = fresh.map { it.songId }.toHashSet()

        val toUpsert = fresh.filter { song -> existing[song.songId] != song.dateModified }
        val toDelete = existing.keys.filterNot { it in freshIds }

        if (toUpsert.isNotEmpty()) librarySongDao.upsertAll(toUpsert)
        if (toDelete.isNotEmpty()) librarySongDao.deleteByIds(toDelete)
    }

    private fun queryMediaStore(): List<LibrarySongEntity> {
        val songs = mutableListOf<LibrarySongEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val genreBySongId = queryGenres()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val relPathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: cursor.getString(nameCol) ?: "Unknown"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val relativePath = cursor.getString(relPathCol) ?: ""
                // RELATIVE_PATH looks like "Music/English/Rock/" - trim the trailing slash
                // so it composes cleanly with folderPath || '/%' prefix queries.
                val folderPath = relativePath.trimEnd(File.separatorChar).ifBlank { "Music" }

                songs.add(
                    LibrarySongEntity(
                        songId = id,
                        title = title,
                        titleKey = title.lowercase(),
                        artist = artist,
                        artistKey = artist.lowercase(),
                        album = album,
                        albumKey = album.lowercase(),
                        albumId = cursor.getLong(albumIdCol),
                        genre = genreBySongId[id],
                        durationMs = cursor.getLong(durationCol),
                        dateAdded = cursor.getLong(dateAddedCol),
                        dateModified = cursor.getLong(dateModifiedCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol),
                        folderPath = folderPath,
                        contentUriString = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        ).toString(),
                    )
                )
            }
        }

        return songs
    }

    /** Best-effort id->genre map. MediaStore models genres as a separate table with a
     * members join table, rather than a column on the song itself - one query per genre
     * (typically a handful) is cheap and avoids an expensive per-song lookup. */
    private fun queryGenres(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        try {
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null, null, null
            )?.use { genreCursor ->
                val genreIdCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val genreNameCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genreCursor.moveToNext()) {
                    val genreId = genreCursor.getLong(genreIdCol)
                    val genreName = genreCursor.getString(genreNameCol) ?: continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members._ID),
                        null, null, null
                    )?.use { memberCursor ->
                        val memberIdCol = memberCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members._ID)
                        while (memberCursor.moveToNext()) {
                            result[memberCursor.getLong(memberIdCol)] = genreName
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Some OEM firmwares don't implement the genres table correctly - genre is a
            // "nice to have" for browsing, never worth crashing the whole scan over.
        }
        return result
    }
}
