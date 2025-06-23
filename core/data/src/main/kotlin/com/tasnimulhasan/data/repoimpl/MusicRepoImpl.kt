package com.tasnimulhasan.data.repoimpl

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.tasnimulhasan.domain.repository.MusicRepository
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.music.MelodiqEntity
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

class MusicRepoImpl @Inject constructor() : MusicRepository {

    @Inject lateinit var roomRepo: MelodiQRepository
    val musics = mutableStateListOf<MusicEntity>()

    override suspend fun fetchMusic(context: Context, sortType: SortType): List<MusicEntity> {

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
        )

        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"

        val sortOrder = when (sortType) {
            SortType.DATE_MODIFIED_ASC -> "${MediaStore.Audio.Media.DATE_ADDED} ASC"
            SortType.DATE_MODIFIED_DESC -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            SortType.NAME_ASC -> "${MediaStore.Audio.Media.TITLE} ASC"
            SortType.NAME_DESC -> "${MediaStore.Audio.Media.TITLE} DESC"
            SortType.ARTIST_ASC -> "${MediaStore.Audio.Media.ARTIST} ASC"
            SortType.ARTIST_DESC -> "${MediaStore.Audio.Media.ARTIST} DESC"
            SortType.DURATION_ASC -> "${MediaStore.Audio.Media.DURATION} ASC"
            SortType.DURATION_DESC -> "${MediaStore.Audio.Media.DURATION} DESC"
        }

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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val duration = cursor.getInt(durationColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val album = cursor.getString(albumColumn)
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
                        albumId = albumId
                    )
                )
            }
        }

        return musics
    }

    override suspend fun insertMusicToRoom(context: Context) {
        roomRepo.fetchAllMusic(SortType.DATE_MODIFIED_DESC).collectLatest {
            if (it.isEmpty()) {
                Log.e("RoomMusic", "Empty")
                val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                )

                val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"

                val sortOrder = "${MediaStore.Audio.Media.DURATION} DESC"

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

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val duration = cursor.getInt(durationColumn)
                        val title = cursor.getString(titleColumn)
                        val artist = cursor.getString(artistColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val album = cursor.getString(albumColumn)
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
                                albumId = albumId
                            )
                        )
                    }
                }

                val roomMusic = musics.map { music ->
                    MelodiqEntity(
                        musicId = music.songId,
                        musicTitle = music.songTitle,
                        musicArtist = music.artist,
                        musicPath = music.contentUri.toString(),
                        musicCover = music.cover,
                        musicDuration = music.duration,
                        album = music.album,
                        albumId = music.albumId
                    )
                }
                roomRepo.insertAllMusic(musicList = roomMusic)
            } else Log.e("RoomMusic", "Not Empty")
        }
    }
}