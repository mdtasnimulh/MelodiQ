package com.tasnimulhasan.featureaudiobook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Html
import android.util.LruCache
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.xml.parsers.DocumentBuilderFactory

/** One word of a page: where it sits in the narration text, and (for PDFs) where it is drawn, as fractions 0..1 of the page. */
internal class PageWord(
    val text: String,
    val start: Int,
    val end: Int,
    val l: Float,
    val t: Float,
    val r: Float,
    val b: Float,
    /** What was put before this word in the narration text: "" (glued to the previous word), " " or a paragraph break. */
    val sepBefore: String,
    /** x of the boundary before each character, plus the end of the last one (page fractions); null = unknown. */
    val edges: FloatArray? = null,
) {
    /** x (page fraction) of the boundary before character [j] of this word; j == length is the word's right end. */
    fun edgeX(j: Int): Float {
        val n = (end - start).coerceAtLeast(1)
        val jj = j.coerceIn(0, n)
        val e = edges
        return if (e != null && jj < e.size) e[jj] else l + (r - l) * jj / n
    }
}

/** The text of one page as it is narrated, with a box for each word when the page has a layout. */
internal class PageContent(val text: String, val words: List<PageWord>) {
    /** The text of words [from]..[to] (inclusive, any order) as it would be copied. */
    fun textBetween(from: Int, to: Int): String {
        val a = minOf(from, to).coerceAtLeast(0)
        val b = maxOf(from, to).coerceAtMost(words.lastIndex)
        if (a > b) return ""
        val sb = StringBuilder()
        for (i in a..b) {
            if (i > a) sb.append(words[i].sepBefore)
            sb.append(words[i].text)
        }
        return sb.toString()
    }

    /** The text of the characters [from]..[to] (inclusive, any order) of the page text, as it would be copied. */
    fun textInRange(from: Int, to: Int): String {
        val a = minOf(from, to).coerceAtLeast(0)
        val b = (maxOf(from, to) + 1).coerceAtMost(text.length)
        return if (a >= b) "" else text.substring(a, b)
    }

    /** Index of the word that contains character [offset] of the narration text, or the nearest one after it. */
    fun wordAt(offset: Int): Int {
        if (words.isEmpty() || offset < 0) return -1
        var lo = 0
        var hi = words.lastIndex
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (words[mid].end <= offset) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        val Empty = PageContent("", emptyList())
    }
}

/** A word as found by PDFBox or OCR, in page fractions, before the narration text is put together. */
internal class RawWord(
    val text: String, val l: Float, val t: Float, val r: Float, val b: Float, val baseline: Float,
    /** Boundary before each character plus the last one's end (page fractions), when the source knows it. */
    val edges: FloatArray? = null,
)

/**
 * Turns the words of a page into narration text. Lines are joined into running text, a gap bigger
 * than a line becomes a paragraph break, and a word hyphenated at the end of a line is rejoined
 * with its second half.
 */
internal fun buildPageContent(raw: List<RawWord>): PageContent {
    val sb = StringBuilder()
    val words = ArrayList<PageWord>(raw.size)
    var prev: RawWord? = null
    var glue = false
    for (i in raw.indices) {
        val w = raw[i]
        var text = w.text.trim()
        if (text.isEmpty()) continue
        val p = prev
        var sep = ""
        var newLine = false
        if (p != null) {
            val lineH = maxOf(p.b - p.t, w.b - w.t).coerceAtLeast(0.004f)
            val down = w.baseline - p.baseline
            newLine = kotlin.math.abs(down) > lineH * 0.45f
            sep = when {
                glue -> ""
                newLine && (down > lineH * 1.9f || down < 0f) -> "\n\n"
                else -> " "
            }
        }
        // "inter-" at the end of a line + "national" on the next: read and copy as one word.
        val next = raw.getOrNull(i + 1)
        glue = false
        if (next != null && text.length > 2 && text.endsWith("-") && text[text.length - 2].isLetter() &&
            next.text.firstOrNull()?.isLowerCase() == true &&
            next.baseline - w.baseline > maxOf(w.b - w.t, 0.004f) * 0.45f
        ) {
            text = text.dropLast(1)
            glue = true
        }
        sb.append(sep)
        val start = sb.length
        sb.append(text)
        val edges = w.edges?.takeIf { it.size == w.text.length + 1 && w.text.startsWith(text) }
        words.add(PageWord(text, start, sb.length, w.l, w.t, w.r, w.b, sep, edges))
        prev = w
    }
    return PageContent(sb.toString(), words)
}

/** The readable text of a book, page by page. Pages are PDF pages, or ~1,800-character EPUB sections. */
internal interface BookSource {
    val pageCount: Int
    suspend fun content(page: Int): PageContent
    suspend fun text(page: Int): String = content(page).text

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

/** Collects every word PDFBox lays out on a page, with its box. */
private class WordCollector : PDFTextStripper() {
    val raw = ArrayList<RawWord>()

    // PDFBox hands over a run of text (often a whole line); split it into real words ourselves,
    // at spaces and at visible gaps, so each word gets its own box.
    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        if (textPositions.isEmpty()) return
        val pw = textPositions[0].pageWidth
        val ph = textPositions[0].pageHeight
        if (pw <= 0f || ph <= 0f) return
        val sb = StringBuilder()
        var l = Float.MAX_VALUE
        var t = Float.MAX_VALUE
        var r = 0f
        var b = 0f
        var baseline = 0f
        var prevRight = Float.NaN
        val edgeList = ArrayList<Float>()

        fun flush() {
            val word = sb.toString().trim()
            if (word.isNotEmpty() && r > l && b > t) {
                raw.add(
                    RawWord(
                        word,
                        (l / pw).coerceIn(0f, 1f), (t / ph).coerceIn(0f, 1f),
                        (r / pw).coerceIn(0f, 1f), (b / ph).coerceIn(0f, 1f),
                        (baseline / ph).coerceIn(0f, 1f),
                        // One boundary per character; a ligature such as "fi" is split evenly between its letters.
                        if (edgeList.size == word.length + 1) FloatArray(edgeList.size) { (edgeList[it] / pw).coerceIn(0f, 1f) } else null,
                    )
                )
            }
            sb.setLength(0)
            edgeList.clear()
            l = Float.MAX_VALUE
            t = Float.MAX_VALUE
            r = 0f
            b = 0f
            prevRight = Float.NaN
        }

        for (pos in textPositions) {
            val ch = pos.unicode ?: ""
            if (ch.isBlank()) {
                flush()
                continue
            }
            val h = pos.heightDir.takeIf { it > 0f } ?: pos.fontSizeInPt
            val x = pos.xDirAdj
            if (!prevRight.isNaN()) {
                val gapLimit = maxOf(pos.widthOfSpace * 0.5f, pos.fontSizeInPt * 0.22f)
                if (x - prevRight > gapLimit || kotlin.math.abs(pos.yDirAdj - baseline) > h * 0.5f) flush()
            }
            // Drop the end-of-previous-glyph boundary; the next glyph's own start replaces it.
            if (edgeList.isNotEmpty()) edgeList.removeAt(edgeList.size - 1)
            val glyphW = pos.widthDirAdj
            for (k in 0 until ch.length) edgeList.add(x + glyphW * k / ch.length)
            edgeList.add(x + glyphW)
            sb.append(ch)
            l = minOf(l, x)
            r = maxOf(r, x + pos.widthDirAdj)
            t = minOf(t, pos.yDirAdj - h)
            b = maxOf(b, pos.yDirAdj + h * 0.22f)
            baseline = pos.yDirAdj
            prevRight = x + pos.widthDirAdj
        }
        flush()
    }
}

/**
 * PDF text through PDFBox, with a box for every word so the page can be selected and followed
 * while it is read. Pages without any text (scans) are read with ML Kit OCR instead. The file is
 * copied to the cache while open (low memory) and deleted on close.
 */
internal class PdfSource private constructor(
    private val doc: PDDocument,
    private val tmp: File,
) : BookSource {
    private val lock = Mutex()
    override val pageCount: Int = doc.numberOfPages
    private val cache = LruCache<Int, PageContent>(48)

    private var ocrFd: ParcelFileDescriptor? = null
    private var ocrRenderer: PdfRenderer? = null
    private var recognizer: TextRecognizer? = null

    override suspend fun content(page: Int): PageContent {
        cache.get(page)?.let { return it }
        return withContext(Dispatchers.IO) {
            lock.withLock {
                cache.get(page)?.let { return@withLock it }
                var content = extract(page)
                if (content.words.isEmpty()) content = ocr(page) ?: content
                cache.put(page, content)
                content
            }
        }
    }

    private fun extract(page: Int): PageContent = runCatching {
        val stripper = WordCollector()
        stripper.startPage = page + 1
        stripper.endPage = page + 1
        stripper.getText(doc)
        buildPageContent(stripper.raw)
    }.getOrDefault(PageContent.Empty)

    /** Reads a scanned page with ML Kit: the page is drawn to a bitmap and recognised on the device. */
    private suspend fun ocr(page: Int): PageContent? = runCatching {
        if (ocrRenderer == null) {
            ocrFd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
            ocrRenderer = PdfRenderer(ocrFd!!)
        }
        val renderer = ocrRenderer ?: return null
        val bmp = renderer.openPage(page).use { p ->
            val w = OCR_WIDTH
            val h = (w.toFloat() * p.height / p.width).toInt().coerceIn(w / 2, w * 3)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                it.eraseColor(android.graphics.Color.WHITE)
                p.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
        try {
            val rec = recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also { recognizer = it }
            val result = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text> { cont ->
                rec.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            val raw = ArrayList<RawWord>()
            val bw = bmp.width.toFloat()
            val bh = bmp.height.toFloat()
            for (block in result.textBlocks) {
                var firstInBlock = true
                for (line in block.lines) {
                    for (el in line.elements) {
                        val box = el.boundingBox ?: continue
                        // A new block reads as a new paragraph: nudge its first word's baseline down.
                        val baseline = box.bottom / bh + if (firstInBlock) 0.05f else 0f
                        firstInBlock = false
                        raw.add(RawWord(el.text, box.left / bw, box.top / bh, box.right / bw, box.bottom / bh, baseline))
                    }
                }
            }
            buildPageContent(raw)
        } finally {
            bmp.recycle()
        }
    }.getOrNull()

    override fun close() {
        runCatching { recognizer?.close() }
        runCatching { ocrRenderer?.close() }
        runCatching { ocrFd?.close() }
        runCatching { doc.close() }
        runCatching { tmp.delete() }
    }

    companion object {
        private const val OCR_WIDTH = 1500

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
    private val empty = PageContent.Empty
    override suspend fun content(page: Int): PageContent = pages.getOrNull(page)?.let { PageContent(it, emptyList()) } ?: empty
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

    /** [covers]: possible cover images in order of confidence (paths inside the zip). */
    private class Package(val spine: List<String>, val covers: List<String>)

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

    /**
     * The book's cover from its metadata, decoded to about [widthPx] wide, or null. Tries, in order: the
     * manifest's cover-image, the `cover` meta entry, the guide's cover page, images named "cover", the
     * first picture of the first pages, and finally the biggest portrait picture in the file.
     */
    fun cover(context: Context, uri: Uri, widthPx: Int): Bitmap? = withZip(context, uri) { zip ->
        val pkg = readPackage(zip) ?: return@withZip null
        val tried = HashSet<String>()
        for (path in pkg.covers + fallbackImages(zip, pkg)) {
            if (!tried.add(path)) continue
            val bmp = decodeEntry(zip, path, widthPx)
            if (bmp != null && bmp.width >= 80 && bmp.height >= 80) return@withZip bmp
        }
        null
    }

    private fun decodeEntry(zip: ZipFile, path: String, widthPx: Int): Bitmap? = runCatching {
        val entry = zip.getEntry(path) ?: zip.entries().toList().firstOrNull { it.name.equals(path, ignoreCase = true) }
            ?: return null
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= widthPx) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    /** Last resorts: a picture on the first pages, then the biggest jpg/png in the archive. */
    private fun fallbackImages(zip: ZipFile, pkg: Package): List<String> {
        val out = ArrayList<String>()
        for (page in pkg.spine.take(3)) {
            val entry = zip.getEntry(page) ?: continue
            val html = runCatching { zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.UTF_8) }.getOrNull() ?: continue
            out += imagesIn(html, page.substringBeforeLast('/', ""))
        }
        val big = zip.entries().toList()
            .filter { !it.isDirectory && IMAGE_EXT.any { ext -> it.name.endsWith(ext, ignoreCase = true) } }
            .sortedByDescending { it.size }
            .take(4)
            .map { it.name }
        return out + big
    }

    private val IMAGE_EXT = listOf(".jpg", ".jpeg", ".png", ".webp")
    private val IMG_SRC = Regex("""<img[^>]*?\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val SVG_IMAGE = Regex("""<image[^>]*?href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    /** Pictures referenced by an (X)HTML page, as paths inside the zip. */
    private fun imagesIn(html: String, base: String): List<String> =
        (IMG_SRC.findAll(html) + SVG_IMAGE.findAll(html)).map { resolve(base, it.groupValues[1]) }.toList()

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
            .getElementsByTagNameNS("*", "rootfile").item(0)
            ?.let { (it as Element).getAttribute("full-path") }
            ?.takeIf { it.isNotBlank() } ?: return null
        val opf = zip.getEntry(opfPath)?.let { e -> zip.getInputStream(e).use { parse(it) } } ?: return null
        val base = opfPath.substringBeforeLast('/', "")

        class Item(val id: String, val href: String, val type: String, val props: String)
        val items = opf.getElementsByTagNameNS("*", "item").let { list ->
            (0 until list.length).map { i ->
                (list.item(i) as Element).let {
                    Item(it.getAttribute("id"), it.getAttribute("href"), it.getAttribute("media-type"), it.getAttribute("properties"))
                }
            }
        }
        val byId = items.associateBy { it.id }
        val spine = opf.getElementsByTagNameNS("*", "itemref").let { list ->
            (0 until list.length).mapNotNull { i ->
                byId[(list.item(i) as Element).getAttribute("idref")]
            }
        }.filter { it.type.contains("html") || it.type.contains("xml") }
            .map { resolve(base, it.href) }

        fun isImage(i: Item) = i.type.startsWith("image/") || IMAGE_EXT.any { i.href.endsWith(it, ignoreCase = true) }
        val covers = ArrayList<String>()
        // 1. EPUB 3: <item properties="cover-image">
        items.firstOrNull { it.props.split(' ').contains("cover-image") }?.let { covers += resolve(base, it.href) }
        // 2. EPUB 2: <meta name="cover" content="id-of-the-image">
        val coverMetaId = opf.getElementsByTagNameNS("*", "meta").let { list ->
            (0 until list.length).map { list.item(it) as Element }
                .firstOrNull { it.getAttribute("name") == "cover" }?.getAttribute("content")
        }
        coverMetaId?.let { id ->
            byId[id]?.let { item ->
                if (isImage(item)) covers += resolve(base, item.href)
                else {
                    // Some books point the meta at the cover page, not the picture.
                    zip.getEntry(resolve(base, item.href))?.let { e ->
                        val html = runCatching { zip.getInputStream(e).use { it.readBytes() }.toString(Charsets.UTF_8) }.getOrNull().orEmpty()
                        covers += imagesIn(html, resolve(base, item.href).substringBeforeLast('/', ""))
                    }
                }
            }
        }
        // 3. <guide><reference type="cover" href="cover.xhtml">
        opf.getElementsByTagNameNS("*", "reference").let { list ->
            (0 until list.length).map { list.item(it) as Element }
                .firstOrNull { it.getAttribute("type") == "cover" }
                ?.getAttribute("href")?.takeIf { it.isNotBlank() }
                ?.let { href ->
                    val path = resolve(base, href)
                    if (IMAGE_EXT.any { path.endsWith(it, ignoreCase = true) }) covers += path
                    else zip.getEntry(path)?.let { e ->
                        val html = runCatching { zip.getInputStream(e).use { it.readBytes() }.toString(Charsets.UTF_8) }.getOrNull().orEmpty()
                        covers += imagesIn(html, path.substringBeforeLast('/', ""))
                    }
                }
        }
        // 4. Any picture called "cover".
        items.filter { isImage(it) && (it.id.contains("cover", true) || it.href.contains("cover", true)) }
            .forEach { covers += resolve(base, it.href) }
        return Package(spine, covers)
    }

    private fun parse(input: java.io.InputStream) =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
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
