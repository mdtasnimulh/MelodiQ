package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.data.lyrics.EmbeddedLyricsReader
import com.tasnimulhasan.data.lyrics.LrcFileLocator
import com.tasnimulhasan.data.lyrics.LrcParser
import com.tasnimulhasan.data.lyrics.NetworkConnectivityChecker
import com.tasnimulhasan.database.dao.LyricsCacheDao
import com.tasnimulhasan.domain.lyrics.LyricsProvider
import com.tasnimulhasan.domain.repository.local.LyricsRepository
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.lyrics.LyricsSource
import com.tasnimulhasan.entity.lyrics.LyricsUiState
import com.tasnimulhasan.entity.room.lyrics.LyricsCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Implements the resolution chain exactly as specified:
 *
 * ```
 * Embedded lyrics? -> yes: show
 *                  -> no: local .lrc? -> yes: parse & show
 *                                     -> no: cached online? -> yes: show
 *                                                            -> no: internet?
 *                                                                -> yes: search online -> found: show + cache
 *                                                                                       -> not found: unavailable
 *                                                                -> no: unavailable (offline)
 * ```
 *
 * Every local step (embedded tag read, .lrc lookup, cache read) runs unconditionally and
 * cheaply before ever considering the network, so the common case - lyrics already on disk
 * or already cached - never touches the network at all.
 */
class LyricsRepoImpl @Inject constructor(
    private val embeddedLyricsReader: EmbeddedLyricsReader,
    private val lrcFileLocator: LrcFileLocator,
    private val lyricsCacheDao: LyricsCacheDao,
    private val connectivityChecker: NetworkConnectivityChecker,
    private val onlineProvider: LyricsProvider,
) : LyricsRepository {

    override fun observeLyrics(song: MusicEntity) = flow {
        emit(LyricsUiState.Loading)

        // 1. Embedded tag (ID3 USLT for mp3, Vorbis Comment for FLAC).
        embeddedLyricsReader.read(song.contentUri, song.mimeType)?.let { raw ->
            emit(toFoundState(raw, LyricsSource.EMBEDDED_TAG))
            return@flow
        }

        // 2. Local .lrc file next to the song.
        lrcFileLocator.findAndRead(song.songId)?.let { raw ->
            emit(toFoundState(raw, LyricsSource.LOCAL_LRC_FILE))
            return@flow
        }

        // 3. Previously fetched and cached online lyrics.
        lyricsCacheDao.get(song.songId)?.let { cached ->
            emit(toFoundState(cached.rawLyrics, LyricsSource.CACHED_ONLINE, forceSynced = cached.isSynced))
            return@flow
        }

        // 4. Nothing local - only now do we even consider the network, and only if one is
        // actually available. This is the one point in the whole chain that can make a
        // network request, and it's reached only after three local misses.
        if (!connectivityChecker.isOnline()) {
            emit(LyricsUiState.Unavailable(offline = true))
            return@flow
        }

        val durationMs = song.duration.toLongOrNull() ?: 0L
        val online = onlineProvider.fetch(
            title = song.songTitle,
            artist = song.artist,
            album = song.album,
            durationMs = durationMs,
        )

        if (online == null) {
            emit(LyricsUiState.Unavailable(offline = false))
            return@flow
        }

        // Cache the result so this exact song never needs a second network request, even
        // offline from now on.
        lyricsCacheDao.upsert(
            LyricsCacheEntity(
                songId = song.songId,
                rawLyrics = online.rawLyrics,
                isSynced = online.isSynced,
                providerName = online.providerName,
                fetchedAt = System.currentTimeMillis(),
            )
        )
        emit(toFoundState(online.rawLyrics, LyricsSource.ONLINE, forceSynced = online.isSynced))
    }.flowOn(Dispatchers.IO)

    private fun toFoundState(raw: String, source: LyricsSource, forceSynced: Boolean? = null): LyricsUiState {
        val lines = LrcParser.parse(raw)
        if (lines.isEmpty()) return LyricsUiState.Unavailable(offline = false)
        val isSynced = forceSynced ?: LrcParser.isSynced(lines)
        return LyricsUiState.Found(lines = lines, isSynced = isSynced, source = source)
    }
}
