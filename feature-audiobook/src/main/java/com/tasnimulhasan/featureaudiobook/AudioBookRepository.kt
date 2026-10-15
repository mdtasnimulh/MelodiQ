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

enum class AddResult { ADDED, DUPLICATE, FAILED }

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

    private val _books = MutableStateFlow(load())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

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
        val pages = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                PdfRenderer(fd).use { it.pageCount }
            } ?: 0
        }.getOrElse { return@withContext AddResult.FAILED }

        val title = name.removeSuffix(".pdf").removeSuffix(".PDF").replace('_', ' ').replace('-', ' ').trim().ifEmpty { "Book" }
        _books.update { listOf(Book(id, uri.toString(), title, size, pages, false, System.currentTimeMillis())) + it }
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
    }

    private companion object {
        const val KEY = "books"
    }
}

/** Draws the first page of a book's PDF as its cover, with a small in-memory cache. */
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
                                cache.put(book.id, bmp)
                                bmp
                            }
                        }
                    } finally {
                        runCatching { fd?.close() }
                    }
                }.getOrNull()
            }
        }
    }
}
