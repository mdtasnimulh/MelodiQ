package com.tasnimulhasan.data.lyrics

import com.google.gson.Gson
import com.tasnimulhasan.domain.lyrics.LyricsProvider
import com.tasnimulhasan.domain.lyrics.OnlineLyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

/**
 * lrclib.net is a free, public, keyless lyrics API that returns synchronized (LRC) lyrics
 * when available and falls back to plain lyrics otherwise - a good fit for "prefer synced,
 * accept unsynced" without needing an API key to manage.
 *
 * Uses plain HttpURLConnection rather than adding an HTTP client dependency (Retrofit/OkHttp
 * aren't currently wired into this project), and Gson, which is already a dependency here.
 */
class LrcLibLyricsProvider @Inject constructor() : LyricsProvider {

    override val name: String = "lrclib.net"

    override suspend fun fetch(title: String, artist: String, album: String?, durationMs: Long): OnlineLyricsResult? =
        withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(title, artist, album, durationMs)
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "MelodiQ (Android music player)")
                }

                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    return@withContext null
                }

                val body = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                connection.disconnect()

                val response = Gson().fromJson(body, LrcLibResponse::class.java) ?: return@withContext null
                val synced = response.syncedLyrics?.trim()
                val plain = response.plainLyrics?.trim()

                when {
                    !synced.isNullOrBlank() -> OnlineLyricsResult(rawLyrics = synced, isSynced = true, providerName = name)
                    !plain.isNullOrBlank() -> OnlineLyricsResult(rawLyrics = plain, isSynced = false, providerName = name)
                    else -> null
                }
            } catch (_: Exception) {
                // Any network/parse failure is treated as "no lyrics available" - lyrics are
                // never allowed to surface an error that could look like a playback problem.
                null
            }
        }

    private fun buildUrl(title: String, artist: String, album: String?, durationMs: Long): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val base = "https://lrclib.net/api/get?track_name=${enc(title)}&artist_name=${enc(artist)}"
        val withAlbum = if (!album.isNullOrBlank()) "$base&album_name=${enc(album)}" else base
        val durationSec = durationMs / 1000
        return if (durationSec > 0) "$withAlbum&duration=$durationSec" else withAlbum
    }

    private data class LrcLibResponse(
        val syncedLyrics: String?,
        val plainLyrics: String?,
    )
}
