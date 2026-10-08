package com.tasnimulhasan.featureplayer.lyrics

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.lyrics.ObserveLyricsUseCase
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.lyrics.LyricsTranslator
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.lyrics.LyricLine
import com.tasnimulhasan.entity.lyrics.LyricsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** The language lyrics were detected to be in, when it isn't English. */
data class DetectedLanguage(
    val tag: String,
    val displayName: String,
    val canTranslate: Boolean,
)

sealed interface TranslationState {
    data object Off : TranslationState
    /** Downloading the language pack (first time only) and/or translating. */
    data object Working : TranslationState
    /** [lines] lines up one-to-one with the original lyric lines. */
    data class Showing(val lines: List<String>) : TranslationState
    data class Failed(val message: String) : TranslationState
}

/**
 * Highlight a hair EARLY. A lyric line should read as "current" right as the singer starts
 * it; with the highlight, recomposition and the scroll animation all landing a frame or two
 * late, using the exact timestamp consistently feels behind. 150ms is a common LRC-player
 * lead and is imperceptible as "early" while removing the "late" feeling.
 */
const val LYRIC_LEAD_MS = 150L

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val observeLyricsUseCase: ObserveLyricsUseCase,
    private val playerUseCases: PlayerUseCases,
    private val lyricsTranslator: LyricsTranslator,
) : ViewModel() {

    private val dummyAudio = MusicEntity(
        contentUri = "".toUri(),
        songId = 0L,
        cover = null,
        songTitle = "",
        artist = "",
        duration = "",
        albumId = 0L,
        album = ""
    )

    val currentSong: StateFlow<MusicEntity> = playerUseCases.observeCurrentSelectedAudio()
        .map { it ?: dummyAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dummyAudio)

    // Re-resolves lyrics whenever the playing song actually changes (not on every emission -
    // observeCurrentSelectedAudio can re-emit the same song). Loading a new song's lyrics
    // never blocks or interrupts playback: this is a plain reactive read on Dispatchers.IO,
    // entirely separate from the playback pipeline.
    val lyricsState: StateFlow<LyricsUiState> = playerUseCases.observeCurrentSelectedAudio()
        .filter { it != null && it.songId != 0L }
        .distinctUntilChangedBy { it!!.songId }
        .flatMapLatest { song -> observeLyricsUseCase(song!!) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsUiState.Loading)

    /**
     * Polled straight from the player every 100ms. The shared progress state only ticks every
     * 500ms (fine for a seek bar), which made the highlight trail the singer by up to half a
     * second. Only runs while something is collecting this (the lyrics screen is open);
     * identical values (paused) are dropped so nothing recomposes while paused.
     */
    private val positionMs: StateFlow<Long> = flow {
        while (true) {
            emit(playerUseCases.getPlaybackSnapshot().position)
            delay(100)
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Index of the line being sung right now (-1 if unsynced / before the first line). The
     * screen observes only this - it changes a few times a minute, not 10x a second - so the
     * list recomposes only when the highlight actually moves. */
    val currentLineIndex: StateFlow<Int> = combine(lyricsState, positionMs) { state, position ->
        if (state is LyricsUiState.Found && state.isSynced) {
            currentLyricLineIndex(state.lines, position + LYRIC_LEAD_MS)
        } else {
            -1
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    // ---- Translation to English -----------------------------------------------------------

    private val _detectedLanguage = MutableStateFlow<DetectedLanguage?>(null)
    val detectedLanguage: StateFlow<DetectedLanguage?> = _detectedLanguage.asStateFlow()

    private val _translation = MutableStateFlow<TranslationState>(TranslationState.Off)
    val translation: StateFlow<TranslationState> = _translation.asStateFlow()

    private var cachedTranslation: List<String>? = null
    private var translateJob: Job? = null

    init {
        // Whenever a new song's lyrics arrive, reset translation and work out whether the
        // lyrics are in a language other than English (only then is the option offered).
        viewModelScope.launch {
            lyricsState.collect { state ->
                translateJob?.cancel()
                cachedTranslation = null
                _translation.value = TranslationState.Off
                _detectedLanguage.value = null
                if (state is LyricsUiState.Found) {
                    val sample = state.lines.map { it.text }.filter { it.isNotBlank() }.take(40).joinToString("\n")
                    val tag = runCatching { lyricsTranslator.detectLanguage(sample) }.getOrNull()
                    if (tag != null && !tag.startsWith("en", ignoreCase = true)) {
                        val name = Locale.forLanguageTag(tag).getDisplayLanguage(Locale.ENGLISH).ifBlank { tag }
                        _detectedLanguage.value = DetectedLanguage(
                            tag = tag,
                            displayName = if (tag.contains("-Latn", ignoreCase = true)) "$name (romanized)" else name,
                            canTranslate = lyricsTranslator.canTranslate(tag),
                        )
                    }
                }
            }
        }
    }

    fun toggleTranslation() {
        when (_translation.value) {
            is TranslationState.Showing -> _translation.value = TranslationState.Off
            TranslationState.Working -> Unit
            else -> {
                val detected = _detectedLanguage.value?.takeIf { it.canTranslate } ?: return
                val found = lyricsState.value as? LyricsUiState.Found ?: return
                cachedTranslation?.let {
                    _translation.value = TranslationState.Showing(it)
                    return
                }
                translateJob = viewModelScope.launch {
                    _translation.value = TranslationState.Working
                    try {
                        val result = lyricsTranslator.translateToEnglish(detected.tag, found.lines.map { it.text })
                        cachedTranslation = result
                        _translation.value = TranslationState.Showing(result)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _translation.value = TranslationState.Failed(
                            "Couldn't translate. The first translation needs a connection to download the language pack."
                        )
                    }
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch { playerUseCases.seekTo(positionMs) }
    }
}

/** Index of the last line whose timestamp has already passed - i.e. the line that should be
 * highlighted right now. Returns -1 for unsynchronized lyrics or before the first line. */
fun currentLyricLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty() || lines.first().timestampMs == null) return -1
    var result = -1
    for (i in lines.indices) {
        val ts = lines[i].timestampMs ?: continue
        if (ts <= positionMs) result = i else break
    }
    return result
}
