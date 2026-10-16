package com.tasnimulhasan.featureaudiobook

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.LruCache
import com.google.gson.Gson
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class AddResult { ADDED, DUPLICATE, FAILED, UNSUPPORTED }

/** Where the reader stopped: a zero-based [page] and how far through it, in thousandths. */
data class Progress(val page: Int = 0, val permille: Int = 0)

/**
 * The audio-book library. It keeps a list of books the user added through the document picker
 * (nothing is copied into app storage) and reads each PDF straight from its place on the phone.
 */
@Singleton
class AudioBookRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("audio_books", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val progressPrefs = context.getSharedPreferences("audio_book_progress", Context.MODE_PRIVATE)

    private val _books = MutableStateFlow(load())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _progress = MutableStateFlow(loadProgress())

    /** Reading/listening position of every book that has one. */
    val progress: StateFlow<Map<String, Progress>> = _progress.asStateFlow()

    private fun loadProgress(): Map<String, Progress> = runCatching {
        progressPrefs.all.keys.filter { it.endsWith(".page") }.associate { key ->
            val id = key.removeSuffix(".page")
            id to Progress(progressPrefs.getInt(key, 0), progressPrefs.getInt("$id.permille", 0))
        }
    }.getOrDefault(emptyMap())

    fun progressOf(id: String): Progress = _progress.value[id] ?: Progress()

    fun saveProgress(id: String, page: Int, permille: Int) {
        progressPrefs.edit().putInt("$id.page", page).putInt("$id.permille", permille).apply()
        _progress.update { it + (id to Progress(page, permille)) }
    }

    private fun load(): List<Book> = runCatching {
        gson.fromJson(prefs.getString(KEY, "[]"), Array<Book>::class.java).toList()
    }.getOrDefault(emptyList())

    private fun save() {
        prefs.edit().putString(KEY, gson.toJson(_books.value)).apply()
    }

    fun find(id: String): Book? = _books.value.firstOrNull { it.id == id }

    suspend fun add(uri: Uri): AddResult = withContext(Dispatchers.IO) {
        val id = UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString()
        if (find(id) != null) return@withContext AddResult.DUPLICATE

        // Keep read access across restarts without copying the file.
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        var name = "Book"
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0) name = c.getString(n) ?: name
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
        }
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        val lower = name.lowercase()
        val epub = lower.endsWith(".epub") || mime == "application/epub+zip"
        val pdf = lower.endsWith(".pdf") || mime == "application/pdf"
        if (!epub && !pdf) return@withContext AddResult.UNSUPPORTED

        val pages = runCatching {
            if (epub) {
                Epub.pageCount(context, uri)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                    PdfRenderer(fd).use { it.pageCount }
                } ?: 0
            }
        }.getOrElse { return@withContext AddResult.FAILED }
        if (pages <= 0) return@withContext AddResult.FAILED

        val title = name.replace(Regex("(?i)\\.(pdf|epub)$"), "").replace('_', ' ').replace('-', ' ').trim().ifEmpty { "Book" }
        val format = if (epub) "epub" else "pdf"
        _books.update { listOf(Book(id, uri.toString(), title, size, pages, false, System.currentTimeMillis(), format)) + it }
        save()
        AddResult.ADDED
    }

    fun toggleFavourite(id: String) {
        _books.update { list -> list.map { if (it.id == id) it.copy(favourite = !it.favourite) else it } }
        save()
    }

    /** Removes the book from the library only; the PDF on the phone is untouched. */
    fun remove(id: String) {
        val book = find(id) ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(book.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        _books.update { list -> list.filterNot { it.id == id } }
        save()
        progressPrefs.edit().remove("$id.page").remove("$id.permille").apply()
        _progress.update { it - id }
    }

    private companion object {
        const val KEY = "books"
    }
}

/** A book's cover: the artwork embedded in its PDF, else the rendered first page. Small in-memory cache. */
@Singleton
class BookCovers @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val gate = Semaphore(2)

    suspend fun load(book: Book, widthPx: Int = 420): Bitmap? {
        cache.get(book.id)?.let { return it }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                // The book's real cover art first; the rendered first page only when there is none.
                val bmp = if (book.isEpub) {
                    runCatching { Epub.cover(context, Uri.parse(book.uri), widthPx) }.getOrNull()
                } else {
                    embeddedCover(book, widthPx) ?: firstPage(book, widthPx)
                }
                bmp?.also { cache.put(book.id, it) }
            }
        }
    }

    /**
     * Most PDF books carry their cover as a large portrait image on page one. Pull that image out
     * directly (full artwork, no page margins or text); returns null when page one has no such image.
     */
    private fun embeddedCover(book: Book, widthPx: Int): Bitmap? {
        if (book.sizeBytes > MAX_SCAN_BYTES) return null
        return runCatching {
            PDFBoxResourceLoader.init(context)
            context.contentResolver.openInputStream(Uri.parse(book.uri))?.use { input ->
                PDDocument.load(input).use { doc ->
                    val resources = doc.getPage(0).resources ?: return@use null
                    var best: PDImageXObject? = null
                    for (name in resources.xObjectNames) {
                        val image = resources.getXObject(name) as? PDImageXObject ?: continue
                        val w = image.width
                        val h = image.height
                        val ratio = w.toFloat() / h
                        if (w < 300 || h < 400 || w.toLong() * h > MAX_COVER_PIXELS) continue
                        if (ratio !in 0.4f..1.0f) continue // not a portrait cover
                        if (best == null || w.toLong() * h > best.width.toLong() * best.height) best = image
                    }
                    best?.image?.let { src ->
                        val h = (widthPx.toFloat() * src.height / src.width).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(src, widthPx, h, true).also { if (it !== src) src.recycle() }
                    }
                }
            }
        }.getOrNull()
    }

    private fun firstPage(book: Book, widthPx: Int): Bitmap? =
        runCatching {
                    var fd: ParcelFileDescriptor? = null
                    try {
                        fd = context.contentResolver.openFileDescriptor(Uri.parse(book.uri), "r") ?: return@runCatching null
                        PdfRenderer(fd).use { renderer ->
                            renderer.openPage(0).use { page ->
                                val h = (widthPx.toFloat() * page.height / page.width).toInt().coerceIn(widthPx / 2, widthPx * 3)
                                val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bmp
                            }
                        }
                    } finally {
                        runCatching { fd?.close() }
                    }
        }.getOrNull()

    private companion object {
        const val MAX_SCAN_BYTES = 80L * 1024 * 1024
        const val MAX_COVER_PIXELS = 16_000_000L
    }
}
