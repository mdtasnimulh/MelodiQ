package com.tasnimulhasan.data.repoimpl

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.tasnimulhasan.domain.repository.MusicRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepoImpl @Inject constructor() : MusicRepository {

    // The MediaStore query itself (walking the cursor, string allocation per row) is the
    // expensive part - not the sort. Previously every sort-type change (and every screen
    // that independently asked for the library) re-ran the full ContentResolver query.
    // Now we run it once, cache the unsorted rows, and re-sort in memory - instant on every
    // subsequent call, and immune to MediaStore returning subtly different orderings across
    // separate queries.
    private val cacheMutex = Mutex()
    private var cachedRawList: List<MusicEntity>? = null

    override suspend fun fetchMusic(context: Context, sortType: SortType): List<MusicEntity> {
        val raw = cacheMutex.withLock {
            cachedRawList ?: queryMediaStore(context).also { cachedRawList = it }
        }
        return withContext(Dispatchers.Default) { sortInMemory(raw, sortType) }
    }

    /** Call after a library change (e.g. a scan) if this process should pick it up. */
    suspend fun invalidateCache() {
        cacheMutex.withLock { cachedRawList = null }
    }

    private fun sortInMemory(list: List<MusicEntity>, sortType: SortType): List<MusicEntity> = when (sortType) {
        SortType.DATE_MODIFIED_ASC -> list.sortedBy { it.dateAdded }
        SortType.DATE_MODIFIED_DESC -> list.sortedByDescending { it.dateAdded }
        SortType.NAME_ASC -> list.sortedBy { it.songTitle.lowercase() }
        SortType.NAME_DESC -> list.sortedByDescending { it.songTitle.lowercase() }
        SortType.ARTIST_ASC -> list.sortedBy { it.artist?.lowercase() }
        SortType.ARTIST_DESC -> list.sortedByDescending { it.artist?.lowercase() }
        SortType.DURATION_ASC -> list.sortedBy { it.duration.toLongOrNull() ?: 0L }
        SortType.DURATION_DESC -> list.sortedByDescending { it.duration.toLongOrNull() ?: 0L }
    }

    private suspend fun queryMediaStore(context: Context): List<MusicEntity> = withContext(Dispatchers.IO) {
        val musics = mutableListOf<MusicEntity>()

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATE_ADDED,
        )

        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"

        // Order doesn't matter here - we sort in memory afterward - but requesting it
        // pre-sorted by the DB's own index avoids a separate in-DB sort step.
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val duration = cursor.getInt(durationColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val album = cursor.getString(albumColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                musics.add(
                    MusicEntity(
                        contentUri = contentUri,
                        songId = id,
                        cover = null,
                        songTitle = title,
                        artist = artist,
                        album = album,
                        duration = duration.toString(),
                        albumId = albumId,
                        dateAdded = dateAdded,
                    )
                )
            }
        }

        musics
    }
}