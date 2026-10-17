package com.tasnimulhasan.featureaudiobook

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.languageid.LanguageIdentification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/** What the listener and the reader see. Positions are a zero-based page plus thousandths of that page. */
data class SpeechState(
    val bookId: String? = null,
    val title: String = "",
    val pageCount: Int = 0,
    val playing: Boolean = false,
    val page: Int = 0,
    val permille: Int = 0,
    /** Character range of the sentence being read on [page], or -1 when nothing is being read. */
    val chunkStart: Int = -1,
    val chunkEnd: Int = -1,
    /** Character range of the single word being spoken right now (a sub-range of the sentence), or -1. */
    val wordStart: Int = -1,
    val wordEnd: Int = -1,
    /** Bumped whenever the position is moved on purpose (skip, restart) so the reader scrolls to it. */
    val seekNonce: Int = 0,
)

/** One of the four narrator choices. [voiceName] is the engine voice behind it, null for the engine default. */
data class VoicePreset(val label: String, val voiceName: String?, val pitch: Float)

/**
 * Reads a book aloud with the phone's text-to-speech engine and remembers where it stopped.
 * One book at a time; it lives for the whole process so listening carries on when the reader
 * screen is closed (the notification service keeps the process alive).
 */
@Singleton
class BookSpeaker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AudioBookRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs = context.getSharedPreferences("audio_book_voice", Context.MODE_PRIVATE)
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _presets = MutableStateFlow(defaultPresets())
    val presets: StateFlow<List<VoicePreset>> = _presets.asStateFlow()

    private val _presetIndex = MutableStateFlow(0)
    val presetIndex: StateFlow<Int> = _presetIndex.asStateFlow()

    /** Show every installed voice, not just those in the book's language. */
    private val _allLanguages = MutableStateFlow(prefs.getBoolean("all_languages", false))
    val allLanguages: StateFlow<Boolean> = _allLanguages.asStateFlow()

    /** The chosen voice by its engine name, so the choice survives the list changing. */
    private var savedVoice: String? = prefs.getString("voice_name", null)

    private val _rate = MutableStateFlow(prefs.getFloat("rate", 1f).coerceIn(0.5f, 2f))
    val rate: StateFlow<Float> = _rate.asStateFlow()

    /** ISO language of the open book as detected by ML Kit ("en", "bn"...), null until known. */
    @Volatile private var language: String? = null
    private var languageWarned = false

    internal var source: BookSource? = null
        private set
    private var book: Book? = null
    private val loadLock = Mutex()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingStart: (() -> Unit)? = null
    private var voices: List<Voice> = emptyList()

    private val gen = AtomicInteger(0)
    @Volatile private var queuedPage = -1
    @Volatile private var endQueued = false
    private var lastSaveAt = 0L
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false
    private var serviceStarted = false

    init {
        // Leftovers from a previous run (the temporary copy a book is read from while open).
        runCatching { booksTempDir(context).listFiles()?.forEach { it.delete() } }
    }

    /** Opens [book] for reading/listening, restoring where it was left. Keeps an already-open book as it is. */
    suspend fun load(book: Book): Boolean = loadLock.withLock {
        if (this.book?.id == book.id && source != null) return@withLock true
        stop()
        val src = openBookSource(context, book) ?: return@withLock false
        if (src.pageCount <= 0) {
            src.close()
            return@withLock false
        }
        source = src
        this.book = book
        val p = repository.progressOf(book.id)
        _state.value = SpeechState(
            bookId = book.id,
            title = book.title,
            pageCount = src.pageCount,
            page = p.page.coerceIn(0, src.pageCount - 1),
            permille = p.permille.coerceIn(0, 1000),
        )
        language = null
        languageWarned = false
        scope.launch { detectLanguage(src, book.id) }
        true
    }

    /** Asks ML Kit which language the book is in, so the narrator uses a voice of that language. */
    private suspend fun detectLanguage(src: BookSource, bookId: String) {
        val start = _state.value.page
        var sample = ""
        for (p in listOf(start, start + 1, 0, 1, 2)) {
            if (p !in 0 until src.pageCount) continue
            val t = runCatching { src.text(p) }.getOrDefault("")
            if (t.length >= 80) {
                sample = t.take(1500)
                break
            }
        }
        if (sample.isEmpty()) return
        val tag = runCatching {
            suspendCancellableCoroutine<String> { cont ->
                LanguageIdentification.getClient().identifyLanguage(sample)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume("und") }
            }
        }.getOrDefault("und")
        if (_state.value.bookId != bookId || tag == "und") return
        language = tag.substringBefore('-')
        if (ttsReady) {
            applyPresets(buildPresets(voices, language, _allLanguages.value))
            applyVoiceSettings()
        }
    }

    // ---- controls ----------------------------------------------------------------------------

    fun play() {
        if (source == null) return
        if (_state.value.playing) return
        ensureTts {
            val s = _state.value
            // Atom of resume: the exact sentence we paused on, else the saved position.
            startFrom(s.page, s.permille, s.chunkStart.takeIf { it >= 0 })
        }
    }

    fun pause() {
        if (!_state.value.playing) return
        gen.incrementAndGet()
        runCatching { tts?.stop() }
        resumeOnFocusGain = false
        abandonFocus()
        _state.update { it.copy(playing = false) }
        persistNow()
    }

    /** Starts reading at character [charStart] of [page] (e.g. from a selected word). */
    fun playFrom(page: Int, charStart: Int) {
        val s = _state.value
        if (source == null) return
        val p = page.coerceIn(0, s.pageCount - 1)
        gen.incrementAndGet()
        runCatching { tts?.stop() }
        _state.update {
            it.copy(playing = false, page = p, chunkStart = -1, chunkEnd = -1, wordStart = -1, wordEnd = -1, seekNonce = it.seekNonce + 1)
        }
        ensureTts { startFrom(p, 0, charStart.coerceAtLeast(0), snapToSentence = false) }
    }

    fun togglePlay() = if (_state.value.playing) pause() else play()

    /** Moves to the start of another page, keeping playback going if it was on. */
    fun skipPage(delta: Int) {
        val s = _state.value
        if (source == null) return
        val target = (s.page + delta).coerceIn(0, s.pageCount - 1)
        moveTo(target, 0)
    }

    /** Jumps to a position chosen by the reader (e.g. the user scrolled while paused). */
    fun seek(page: Int, permille: Int) {
        val s = _state.value
        if (source == null || s.playing) return
        val p = page.coerceIn(0, s.pageCount - 1)
        val m = permille.coerceIn(0, 1000)
        if (p == s.page && m == s.permille) return
        _state.update { it.copy(page = p, permille = m, chunkStart = -1, chunkEnd = -1, wordStart = -1, wordEnd = -1) }
        persistNow()
    }

    private fun moveTo(page: Int, permille: Int) {
        val wasPlaying = _state.value.playing
        _state.update { it.copy(page = page, permille = permille, chunkStart = -1, chunkEnd = -1, wordStart = -1, wordEnd = -1, seekNonce = it.seekNonce + 1) }
        persistNow()
        if (wasPlaying) {
            gen.incrementAndGet()
            runCatching { tts?.stop() }
            _state.update { it.copy(playing = false) }
            ensureTts { startFrom(page, permille, null) }
        }
    }

    /** Ends the session: stops speech, saves the position, closes the book and removes the notification. */
    fun stop() {
        gen.incrementAndGet()
        runCatching { tts?.stop() }
        abandonFocus()
        resumeOnFocusGain = false
        pendingStart = null
        if (book != null) persistNow()
        source?.close()
        source = null
        book = null
        _state.value = SpeechState()
        if (serviceStarted) {
            serviceStarted = false
            runCatching { context.stopService(Intent(context, BookSpeechService::class.java)) }
        }
    }

    fun onBookRemoved(id: String) {
        if (book?.id == id) stop()
    }

    fun selectPreset(index: Int) {
        val list = _presets.value
        if (list.isEmpty()) return
        val i = index.coerceIn(0, list.lastIndex)
        if (i == _presetIndex.value && list[i].voiceName == savedVoice) return
        _presetIndex.value = i
        savedVoice = list[i].voiceName
        prefs.edit().putString("voice_name", savedVoice).apply()
        applyVoiceSettings()
        restartIfPlaying()
    }

    fun setAllLanguages(all: Boolean) {
        if (all == _allLanguages.value) return
        _allLanguages.value = all
        prefs.edit().putBoolean("all_languages", all).apply()
        if (ttsReady) applyPresets(buildPresets(voices, language, all))
    }

    /** Publishes the voice list and keeps the previously chosen voice selected if it is still there. */
    private fun applyPresets(list: List<VoicePreset>) {
        _presets.value = list
        val i = list.indexOfFirst { it.voiceName != null && it.voiceName == savedVoice }
        _presetIndex.value = if (i >= 0) i else 0
    }

    fun setRate(rate: Float) {
        val r = rate.coerceIn(0.5f, 2f)
        _rate.value = r
        prefs.edit().putFloat("rate", r).apply()
        applyVoiceSettings()
        restartIfPlaying()
    }

    /** Speaks a short sample in the given voice without touching the book position. */
    fun preview(index: Int) {
        if (_state.value.playing) pause()
        ensureTts {
            val preset = _presets.value.getOrNull(index) ?: return@ensureTts
            val engine = tts ?: return@ensureTts
            gen.incrementAndGet()
            engine.stop()
            applyPreset(engine, preset)
            engine.speak("Hello, this is how I will read your book.", TextToSpeech.QUEUE_FLUSH, null, "preview")
        }
    }

    private fun restartIfPlaying() {
        val s = _state.value
        if (!s.playing) return
        gen.incrementAndGet()
        runCatching { tts?.stop() }
        _state.update { it.copy(playing = false) }
        ensureTts { startFrom(s.page, s.permille, s.chunkStart.takeIf { it >= 0 }) }
    }

    // ---- speech ------------------------------------------------------------------------------

    private fun ensureTts(block: () -> Unit) {
        if (ttsReady) {
            block()
            return
        }
        pendingStart = block
        if (tts == null) tts = TextToSpeech(context, ::onTtsInit)
    }

    private fun onTtsInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            runCatching { engine?.shutdown() }
            tts = null
            pendingStart = null
            _messages.tryEmit("Text-to-speech isn't available on this device")
            return
        }
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        engine.setOnUtteranceProgressListener(listener)
        voices = runCatching { engine.voices?.toList() }.getOrNull().orEmpty()
        applyPresets(buildPresets(voices, language, _allLanguages.value))
        ttsReady = true
        applyVoiceSettings()
        pendingStart?.invoke()
        pendingStart = null
    }

    /** Re-reads the engine's installed voices (after the user downloaded one) and rebuilds the four narrators. */
    fun refreshVoices() {
        val engine = tts ?: return
        if (!ttsReady) return
        val fresh = runCatching { engine.voices?.toList() }.getOrNull().orEmpty()
        if (fresh.size == voices.size && fresh.map { it.name }.toSet() == voices.map { it.name }.toSet()) return
        voices = fresh
        languageWarned = false
        applyPresets(buildPresets(voices, language, _allLanguages.value))
        applyVoiceSettings()
    }

    private fun applyVoiceSettings() {
        val engine = tts ?: return
        if (!ttsReady) return
        _presets.value.getOrNull(_presetIndex.value)?.let { applyPreset(engine, it) }
    }

    private fun applyPreset(engine: TextToSpeech, preset: VoicePreset) {
        val voice = preset.voiceName?.let { name -> voices.firstOrNull { it.name == name } }
        if (voice != null) {
            engine.setVoice(voice)
        } else {
            // No named voice: at least speak in the book's language.
            val lang = language ?: Locale.getDefault().language
            val result = engine.setLanguage(Locale.forLanguageTag(lang))
            if ((result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) && !languageWarned) {
                languageWarned = true
                val name = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.ENGLISH)
                _messages.tryEmit("No $name voice is installed. Add one in Settings > Text-to-speech so this book is read correctly")
            }
        }
        engine.setPitch(preset.pitch)
        engine.setSpeechRate(_rate.value)
    }

    /**
     * Starts reading at a page, [permille] through it. [exactChunkStart] (a character offset on
     * that page) wins when given, so resuming after a pause repeats nothing.
     */
    private fun startFrom(page: Int, permille: Int, exactChunkStart: Int?, snapToSentence: Boolean = true) {
        val src = source ?: return
        val engine = tts ?: return
        if (!requestFocus()) {
            _messages.tryEmit("Couldn't get audio focus")
            return
        }
        val myGen = gen.incrementAndGet()
        engine.stop()
        applyVoiceSettings()
        _state.update { it.copy(playing = true, page = page, permille = permille) }
        queuedPage = -1
        endQueued = false
        if (!serviceStarted) {
            serviceStarted = true
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, BookSpeechService::class.java))
            }
        }
        scope.launch {
            var p = page
            var text = src.text(p)
            var from = exactChunkStart ?: (permille * text.length + 999) / 1000
            while (text.isBlank() && p < src.pageCount - 1) {
                p++
                text = src.text(p)
                from = 0
            }
            if (gen.get() != myGen) return@launch
            if (text.isBlank()) {
                _messages.tryEmit("This book has no readable text (it may be scanned images)")
                pause()
                return@launch
            }
            if (p != page) _state.update { it.copy(page = p, permille = 0, seekNonce = it.seekNonce + 1) }
            enqueue(myGen, p, text, from, snapToSentence && p == page)
        }
    }

    private fun enqueue(myGen: Int, page: Int, text: String, from: Int, snap: Boolean = true) {
        val engine = tts ?: return
        val chunks = SpeechChunker.split(text, from, snap)
        if (chunks.isEmpty()) return
        queuedPage = page
        chunks.forEachIndexed { i, (s, e) ->
            if (gen.get() != myGen) return
            val last = if (i == chunks.lastIndex) 1 else 0
            engine.speak(text.substring(s, e).trim(), TextToSpeech.QUEUE_ADD, null, "$myGen|$page|$s|$e|$last|${text.length}")
        }
    }

    /** Queues the next page that has text while the current one's last sentence is still being read. */
    private fun prefetchNext(myGen: Int, page: Int) {
        val src = source ?: return
        scope.launch {
            var p = page + 1
            while (p < src.pageCount) {
                val text = src.text(p)
                if (gen.get() != myGen) return@launch
                if (text.isNotBlank()) {
                    enqueue(myGen, p, text, 0)
                    return@launch
                }
                p++
            }
            endQueued = true
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val id = parse(utteranceId) ?: return
            if (id.gen != gen.get()) return
            _state.update {
                it.copy(
                    playing = true,
                    page = id.page,
                    permille = ceilPermille(id.start, id.length),
                    chunkStart = id.start,
                    chunkEnd = id.end,
                    wordStart = -1,
                    wordEnd = -1,
                )
            }
            val now = System.currentTimeMillis()
            if (now - lastSaveAt > 1500) persistNow()
            if (id.last && id.page == queuedPage && !endQueued) prefetchNext(id.gen, id.page)
        }

        // The engine tells which part of the sentence it is saying right now: that is the highlighted word.
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            val id = parse(utteranceId) ?: return
            if (id.gen != gen.get()) return
            _state.update {
                if (it.page != id.page) it
                else it.copy(wordStart = id.start + start, wordEnd = id.start + end)
            }
        }

        override fun onDone(utteranceId: String?) {
            val id = parse(utteranceId) ?: return
            if (id.gen != gen.get()) return
            if (id.last && endQueued && id.page == queuedPage) finishBook()
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            val id = parse(utteranceId) ?: return
            if (id.gen != gen.get()) return
            _messages.tryEmit("Narration stopped because of a speech engine error")
            pause()
        }
    }

    private fun finishBook() {
        val s = _state.value
        gen.incrementAndGet()
        abandonFocus()
        // Rewind so the next Play starts from the beginning of the book.
        repository.saveProgress(s.bookId ?: return, 0, 0)
        _state.update { it.copy(playing = false, page = 0, permille = 0, chunkStart = -1, chunkEnd = -1, wordStart = -1, wordEnd = -1, seekNonce = it.seekNonce + 1) }
        _messages.tryEmit("Finished the book")
    }

    private fun persistNow() {
        val s = _state.value
        val id = s.bookId ?: return
        lastSaveAt = System.currentTimeMillis()
        repository.saveProgress(id, s.page, s.permille)
    }

    // ---- audio focus -------------------------------------------------------------------------

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> scope.launch { pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.playing) {
                    resumeOnFocusGain = true
                    scope.launch { pause() }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocusGain) {
                resumeOnFocusGain = false
                scope.launch { play() }
            }
            else -> Unit
        }
    }

    private fun requestFocus(): Boolean {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { runCatching { audio.abandonAudioFocusRequest(it) } }
    }

    // ---- helpers -----------------------------------------------------------------------------

    private class UtteranceId(val gen: Int, val page: Int, val start: Int, val end: Int, val last: Boolean, val length: Int)

    private fun parse(id: String?): UtteranceId? {
        val parts = id?.split('|') ?: return null
        if (parts.size != 6) return null
        return runCatching {
            UtteranceId(parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4] == "1", parts[5].toInt())
        }.getOrNull()
    }

    private fun ceilPermille(start: Int, length: Int): Int =
        if (length <= 0) 0 else min(1000, (start * 1000 + length - 1) / length)

    private fun defaultPresets() = listOf(
        VoicePreset("Female 1", null, 1.15f),
        VoicePreset("Male 1", null, 0.8f),
        VoicePreset("Female 2", null, 1.3f),
        VoicePreset("Male 2", null, 0.92f),
    )

    /**
     * Every installed voice of the book's language (or of all languages when asked), best quality first.
     * Android doesn't report a voice's gender, so it is only shown when the voice's name says so.
     */
    private fun buildPresets(all: List<Voice>, lang: String?, allLanguages: Boolean): List<VoicePreset> {
        val wanted = lang ?: Locale.getDefault().language
        val matching = if (allLanguages) all else all.filter { it.locale.language == wanted }
        val pool = (if (matching.isEmpty()) all else matching)
            .distinctBy { it.name }
            .sortedWith(
                compareBy<Voice> { it.locale.getDisplayName(Locale.ENGLISH) }
                    .thenByDescending { it.quality }
                    .thenBy { it.name }
            )
        if (pool.isEmpty()) return defaultPresets()

        val counters = HashMap<String, Int>()
        return pool.map { v ->
            val place = v.locale.getDisplayName(Locale.ENGLISH).ifBlank { v.locale.toLanguageTag() }
            val n = (counters[place] ?: 0) + 1
            counters[place] = n
            val gender = when {
                v.name.contains("female", ignoreCase = true) -> " · Female"
                v.name.contains("male", ignoreCase = true) -> " · Male"
                else -> ""
            }
            val online = if (v.isNetworkConnectionRequired) " · Online" else ""
            VoicePreset("$place $n$gender$online", v.name, 1f)
        }
    }
}

/** Splits page text into sentence-sized pieces so speech can start, pause and be followed precisely. */
internal object SpeechChunker {
    private const val MAX = 350
    private const val MIN = 60
    private const val SENTENCE_END = ".!?।\n"
    private const val CLOSERS = "\"'”’)]"

    /** Returns (start, end) character ranges covering [text] from the sentence that contains [from]. */
    fun split(text: String, from: Int, snapToSentence: Boolean = true): List<Pair<Int, Int>> {
        val len = text.length
        var s = from.coerceIn(0, len)
        if (snapToSentence && s > 0 && s < len) {
            val limit = maxOf(0, s - 400)
            var i = s
            while (i > limit && text[i - 1] !in ".!?\n") i--
            s = if (i > limit) i else (text.lastIndexOf(' ', s).takeIf { it >= limit }?.plus(1) ?: s)
        }
        while (s < len && text[s].isWhitespace()) s++

        val out = ArrayList<Pair<Int, Int>>()
        while (s < len) {
            val windowEnd = min(len, s + MAX)
            var cut = -1
            var i = s
            while (i < windowEnd) {
                if (text[i] in SENTENCE_END) {
                    var j = i + 1
                    while (j < len && text[j] in CLOSERS) j++
                    if ((j >= len || text[j].isWhitespace()) && (j - s >= MIN || j >= len)) {
                        cut = j
                        break
                    }
                    i = j
                } else {
                    i++
                }
            }
            if (cut == -1) {
                cut = if (windowEnd >= len) len else (text.lastIndexOf(' ', windowEnd).takeIf { it > s } ?: windowEnd)
            }
            if (text.substring(s, cut).isNotBlank()) out.add(s to cut)
            s = cut
            while (s < len && text[s].isWhitespace()) s++
        }
        return out
    }
}
