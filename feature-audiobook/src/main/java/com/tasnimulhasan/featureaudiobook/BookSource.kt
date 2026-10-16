package com.tasnimulhasan.featureaudiobook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Html
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/** The readable text of a book, page by page. Pages are PDF pages, or ~1,800-character EPUB sections. */
internal interface BookSource {
    val pageCount: Int
    suspend fun text(page: Int): String

    /** Text that is already in memory (EPUB), so the UI can draw it without waiting. */
    fun cachedText(page: Int): String? = null
    fun close()
}

internal suspend fun openBookSource(context: Context, book: Book): BookSource? =
    withContext(Dispatchers.IO) {
        runCatching {
            if (book.isEpub) {
                val pages = Epub.readPages(context, Uri.parse(book.uri))
                if (pages.isEmpty()) null else EpubSource(pages)
            } else {
                PdfSource.open(context, book)
            }
        }.getOrNull()
    }

internal fun booksTempDir(context: Context): File = File(context.cacheDir, "booktmp").apply { mkdirs() }

/** PDF text through PDFBox. The file is copied to the cache while open (low memory) and deleted on close. */
internal class PdfSource private constructor(
    private val doc: PDDocument,
    private val tmp: File,
) : BookSource {
    private val lock = Mutex()
    override val pageCount: Int = doc.numberOfPages

    override suspend fun text(page: Int): String = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching {
                val stripper = PDFTextStripper()
                stripper.setStartPage(page + 1)
                stripper.setEndPage(page + 1)
                cleanPdfText(stripper.getText(doc))
            }.getOrDefault("")
        }
    }

    override fun close() {
        runCatching { doc.close() }
        runCatching { tmp.delete() }
    }

    companion object {
        fun open(context: Context, book: Book): PdfSource? {
            PDFBoxResourceLoader.init(context)
            val tmp = File(booksTempDir(context), "${book.id}.pdf")
            context.contentResolver.openInputStream(Uri.parse(book.uri))?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return null
            return runCatching { PdfSource(PDDocument.load(tmp), tmp) }
                .onFailure { tmp.delete() }
                .getOrNull()
        }
    }
}

internal class EpubSource(private val pages: List<String>) : BookSource {
    override val pageCount: Int = pages.size
    override suspend fun text(page: Int): String = pages.getOrElse(page) { "" }
    override fun cachedText(page: Int): String? = pages.getOrNull(page)
    override fun close() = Unit
}

/** Joins wrapped lines back into running text and drops end-of-line hyphenation. */
internal fun cleanPdfText(raw: String): String =
    raw.replace("\r\n", "\n").replace('\r', '\n')
        .replace(Regex("(\\p{L})-\\n(\\p{L})"), "$1$2")
        .replace(Regex("(?<!\\n)\\n(?!\\n)"), " ")
        .replace(Regex("[ \\t\\u00A0]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

internal object Epub {
    private const val PAGE_CHARS = 1800

    private class Package(val spine: List<String>, val cover: String?)

    /** Every readable section of the book, split into pages of roughly [PAGE_CHARS] characters. */
    fun readPages(context: Context, uri: Uri): List<String> = withZip(context, uri) { zip ->
        val pkg = readPackage(zip) ?: return@withZip emptyList()
        val pages = ArrayList<String>()
        for (path in pkg.spine) {
            val entry = zip.getEntry(path) ?: continue
            val html = zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.UTF_8)
            paginate(htmlToText(html), pages)
        }
        pages
    } ?: emptyList()

    fun pageCount(context: Context, uri: Uri): Int = readPages(context, uri).size

    /** The cover image declared by the EPUB, decoded to about [widthPx] wide, or null. */
    fun cover(context: Context, uri: Uri, widthPx: Int): Bitmap? = withZip(context, uri) { zip ->
        val path = readPackage(zip)?.cover ?: return@withZip null
        val entry = zip.getEntry(path) ?: return@withZip null
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return@withZip null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= widthPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun <T> withZip(context: Context, uri: Uri, block: (ZipFile) -> T): T? {
        val tmp = File(booksTempDir(context), "epub-${System.nanoTime()}.zip")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return null
            ZipFile(tmp).use(block)
        } catch (_: Throwable) {
            null
        } finally {
            tmp.delete()
        }
    }

    private fun readPackage(zip: ZipFile): Package? {
        val container = zip.getEntry("META-INF/container.xml") ?: return null
        val opfPath = zip.getInputStream(container).use { parse(it) }
            .getElementsByTagName("rootfile").item(0)
            ?.let { (it as Element).getAttribute("full-path") }
            ?.takeIf { it.isNotBlank() } ?: return null
        val opf = zip.getEntry(opfPath)?.let { e -> zip.getInputStream(e).use { parse(it) } } ?: return null
        val base = opfPath.substringBeforeLast('/', "")

        class Item(val id: String, val href: String, val type: String, val props: String)
        val items = opf.getElementsByTagName("item").let { list ->
            (0 until list.length).map { i ->
                (list.item(i) as Element).let {
                    Item(it.getAttribute("id"), it.getAttribute("href"), it.getAttribute("media-type"), it.getAttribute("properties"))
                }
            }
        }
        val byId = items.associateBy { it.id }
        val spine = opf.getElementsByTagName("itemref").let { list ->
            (0 until list.length).mapNotNull { i ->
                byId[(list.item(i) as Element).getAttribute("idref")]
            }
        }.filter { it.type.contains("html") || it.type.contains("xml") }
            .map { resolve(base, it.href) }

        val coverMetaId = opf.getElementsByTagName("meta").let { list ->
            (0 until list.length).map { list.item(it) as Element }
                .firstOrNull { it.getAttribute("name") == "cover" }?.getAttribute("content")
        }
        val coverItem = items.firstOrNull { it.props.split(' ').contains("cover-image") }
            ?: coverMetaId?.let { byId[it] }
            ?: items.firstOrNull { it.type.startsWith("image/") && it.id.contains("cover", ignoreCase = true) }
        return Package(spine, coverItem?.let { resolve(base, it.href) })
    }

    private fun parse(input: java.io.InputStream) =
        DocumentBuilderFactory.newInstance().apply {
            // Never fetch a DTD over the network while reading a book.
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }.newDocumentBuilder().parse(input).also { it.documentElement.normalize() }

    /** Resolves a manifest href against the package folder: decodes %20 and collapses "../". */
    private fun resolve(base: String, href: String): String {
        val parts = ArrayList<String>()
        val all = (if (base.isEmpty()) href else "$base/$href").substringBefore('#')
        for (seg in Uri.decode(all).split('/')) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts.add(seg)
            }
        }
        return parts.joinToString("/")
    }

    private fun htmlToText(html: String): String {
        val body = html
            .replace(Regex("(?is)<head.*?</head>"), " ")
            .replace(Regex("(?is)<script.*?</script>"), " ")
            .replace(Regex("(?is)<style.*?</style>"), " ")
        return Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT).toString()
            .replace('￼', ' ')
            .replace(' ', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" ?\\n ?"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    /** Appends [text] as pages, breaking on paragraph (then sentence, then word) boundaries. */
    private fun paginate(text: String, out: MutableList<String>) {
        if (text.isBlank()) return
        val current = StringBuilder()
        fun flush() {
            if (current.isNotBlank()) out.add(current.toString().trim())
            current.setLength(0)
        }
        for (paragraph in text.split(Regex("\\n{2,}|\\n"))) {
            val p = paragraph.trim()
            if (p.isEmpty()) continue
            if (current.isNotEmpty() && current.length + p.length > PAGE_CHARS) flush()
            if (p.length > PAGE_CHARS) {
                var rest = p
                while (rest.length > PAGE_CHARS) {
                    var cut = rest.lastIndexOf(". ", PAGE_CHARS).let { if (it > PAGE_CHARS / 3) it + 1 else -1 }
                    if (cut < 0) cut = rest.lastIndexOf(' ', PAGE_CHARS).takeIf { it > 0 } ?: PAGE_CHARS
                    if (current.isNotEmpty()) flush()
                    out.add(rest.substring(0, cut).trim())
                    rest = rest.substring(cut).trim()
                }
                if (rest.isNotEmpty()) current.append(rest).append("\n\n")
            } else {
                current.append(p).append("\n\n")
            }
        }
        flush()
    }
}
