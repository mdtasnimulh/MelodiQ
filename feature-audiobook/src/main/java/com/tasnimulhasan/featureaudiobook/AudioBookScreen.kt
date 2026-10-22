package com.tasnimulhasan.featureaudiobook

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.key
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.designsystem.component.LocalFloatingBarPadding
import com.tasnimulhasan.ui.component.MelodiqConfirmSheet
import com.tasnimulhasan.ui.component.MelodiqSheet
import com.tasnimulhasan.ui.component.SheetHeader
import com.tasnimulhasan.ui.component.SheetPrimaryButton
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

/** Glass-shelf colors a shelf can be given. */
private val ShelfPalette = listOf(
    Color(0xFFB57EDC), Color(0xFFF08A73), Color(0xFF4DB6AC), Color(0xFFF4A94E),
    Color(0xFF4DA3FF), Color(0xFFFF8FB1), Color(0xFF6FD6A3), Color(0xFF8D99AE),
)
private const val MAIN_COLOR_INDEX = 4
private const val MAIN_SHELF = "__main__"
private const val MAX_COVERS = 5

private val CoverFallbacks = listOf(
    listOf(Color(0xFF8E2C2C), Color(0xFF3B1E3F)),
    listOf(Color(0xFF1F5F8B), Color(0xFF12263F)),
    listOf(Color(0xFF2E7D5B), Color(0xFF14352B)),
    listOf(Color(0xFFB5651D), Color(0xFF4A2511)),
    listOf(Color(0xFF6A4C93), Color(0xFF2A1B3D)),
)

@Composable
internal fun AudioBookRouteScreen(
    navigateToDetails: (bookId: String) -> Unit,
    navigateBack: () -> Unit,
    viewModel: AudioBookViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The shelf the next picked books go onto (null = the main shelf).
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addBooks(uris, target)
    }
    AudioBookScreen(
        books = books,
        shelves = shelves,
        progress = progress,
        covers = viewModel.covers,
        onAdd = { shelfId ->
            target = shelfId
            picker.launch(arrayOf("application/pdf", "application/epub+zip", "application/octet-stream"))
        },
        onOpen = navigateToDetails,
        onToggleFavourite = viewModel::toggleFavourite,
        onRemove = viewModel::remove,
        onCreateShelf = viewModel::createShelf,
        onUpdateShelf = viewModel::updateShelf,
        onDeleteShelf = viewModel::deleteShelf,
        onMove = viewModel::moveToShelf,
        onNavigateBack = navigateBack,
    )
}

private sealed interface Sheet {
    data class BookActions(val book: Book) : Sheet
    data class MoveBook(val book: Book) : Sheet
    data class ConfirmRemove(val book: Book) : Sheet

    /** [shelf] null = the main shelf. */
    data class ShelfActions(val shelf: Shelf?) : Sheet

    /** [shelf] null = a new shelf. */
    data class EditShelf(val shelf: Shelf?) : Sheet
    data class DeleteShelf(val shelf: Shelf) : Sheet
}

@Composable
internal fun AudioBookScreen(
    books: List<Book>,
    shelves: List<Shelf>,
    progress: Map<String, Progress>,
    covers: BookCovers,
    onAdd: (shelfId: String?) -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCreateShelf: (name: String, colorIndex: Int) -> Unit,
    onUpdateShelf: (id: String, name: String, colorIndex: Int) -> Unit,
    onDeleteShelf: (String) -> Unit,
    onMove: (bookId: String, shelfId: String?) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openShelf by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }

    val favourites = remember(books) { books.filter { it.favourite } }
    val reading = remember(books, progress) {
        books.filter { b -> progress[b.id]?.let { it.page > 0 || it.permille > 0 } == true }
    }
    // A shelf that was deleted while open goes back to the library.
    LaunchedEffect(shelves, openShelf) {
        val o = openShelf
        if (o != null && o != MAIN_SHELF && shelves.none { it.id == o }) openShelf = null
    }
    BackHandler(enabled = openShelf != null) { openShelf = null }

    val bottomPad = LocalFloatingBarPadding.current + 24.dp
    AnimatedContent(
        targetState = openShelf,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 6 } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 6 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(200)))
            }
        },
        label = "library",
    ) { open ->
    if (open != null) {
        val shelf = shelves.firstOrNull { it.id == open }
        ShelfPage(
            name = shelf?.name ?: "My Books",
            color = ShelfPalette[(shelf?.colorIndex ?: MAIN_COLOR_INDEX) % ShelfPalette.size],
            books = books.filter { (it.shelfId ?: MAIN_SHELF) == open },
            progress = progress,
            covers = covers,
            bottomPad = bottomPad,
            onBack = { openShelf = null },
            onAdd = { onAdd(shelf?.id) },
            onMenu = { sheet = Sheet.ShelfActions(shelf) },
            onOpen = onOpen,
            onBookMenu = { sheet = Sheet.BookActions(it) },
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPad),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        com.tasnimulhasan.designsystem.component.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "My Library",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = if (books.size == 1) "1 book" else "${books.size} books",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                FloatingTabs(
                    labels = listOf("All (${books.size})", "Reading (${reading.size})", "Favourites (${favourites.size})"),
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                )
            }

            item {
                TextButton(onClick = { sheet = Sheet.EditShelf(null) }) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Shelf", fontWeight = FontWeight.SemiBold)
                }
            }

            if (tab == 0) {
                val mainBooks = books.filter { it.shelfId == null || shelves.none { s -> s.id == it.shelfId } }
                if (books.isEmpty() && shelves.isEmpty()) {
                    item { EmptyState(favouritesTab = false, readingTab = false, onAdd = { onAdd(null) }) }
                } else {
                    if (mainBooks.isNotEmpty() || shelves.isEmpty()) {
                        item(key = "shelf-main") {
                            ShelfCard(
                                modifier = Modifier.animateItem().entrance(0),
                                name = "My Books",
                                color = ShelfPalette[MAIN_COLOR_INDEX],
                                books = mainBooks,
                                covers = covers,
                                onOpen = { openShelf = MAIN_SHELF },
                                onAdd = { onAdd(null) },
                                onMenu = { sheet = Sheet.ShelfActions(null) },
                            )
                        }
                    }
                    itemsIndexed(shelves, key = { _, s -> "shelf-${s.id}" }) { index, shelf ->
                        ShelfCard(
                            modifier = Modifier.animateItem().entrance((minOf(index + 1, 3)) * 80),
                            name = shelf.name,
                            color = ShelfPalette[shelf.colorIndex % ShelfPalette.size],
                            books = books.filter { it.shelfId == shelf.id },
                            covers = covers,
                            onOpen = { openShelf = shelf.id },
                            onAdd = { onAdd(shelf.id) },
                            onMenu = { sheet = Sheet.ShelfActions(shelf) },
                        )
                    }
                }
            } else {
                val shown = if (tab == 1) reading else favourites
                if (shown.isEmpty()) {
                    item { EmptyState(favouritesTab = tab == 2, readingTab = tab == 1, onAdd = { onAdd(null) }) }
                } else {
                    items(shown.chunked(3), key = { row -> "row-${row.first().id}" }) { row ->
                        BookRow(row, progress, covers, onOpen, Modifier.animateItem()) { sheet = Sheet.BookActions(it) }
                    }
                }
            }
        }
    }
    }

    // ---- sheets ------------------------------------------------------------------------------
    when (val s = sheet) {
        null -> Unit
        is Sheet.BookActions -> {
            val book = books.firstOrNull { it.id == s.book.id } ?: s.book
            MelodiqSheet(onDismiss = { sheet = null }) { dismiss ->
                SheetHeader(title = book.title, message = "${book.formatLabel} · ${book.pages} pages")
                Spacer(Modifier.height(10.dp))
                SheetActionRow(
                    icon = if (book.favourite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                    title = if (book.favourite) "Remove from favourites" else "Add to favourites",
                ) { onToggleFavourite(book.id); dismiss() }
                SheetActionRow(icon = Icons.Filled.CollectionsBookmark, title = "Move to shelf") { sheet = Sheet.MoveBook(book) }
                SheetActionRow(icon = Icons.Filled.Delete, title = "Remove from library", destructive = true) {
                    sheet = Sheet.ConfirmRemove(book)
                }
            }
        }
        is Sheet.MoveBook -> {
            val book = books.firstOrNull { it.id == s.book.id } ?: s.book
            val current = book.shelfId?.takeIf { id -> shelves.any { it.id == id } }
            MelodiqSheet(onDismiss = { sheet = null }) { dismiss ->
                SheetHeader(title = "Move to shelf", message = book.title)
                Spacer(Modifier.height(10.dp))
                SheetActionRow(
                    icon = Icons.Filled.CollectionsBookmark,
                    title = "My Books",
                    checked = current == null,
                ) { onMove(book.id, null); dismiss() }
                shelves.forEach { shelf ->
                    SheetActionRow(
                        icon = Icons.Filled.CollectionsBookmark,
                        title = shelf.name,
                        checked = current == shelf.id,
                        tint = ShelfPalette[shelf.colorIndex % ShelfPalette.size],
                    ) { onMove(book.id, shelf.id); dismiss() }
                }
            }
        }
        is Sheet.ConfirmRemove -> MelodiqConfirmSheet(
            title = "Remove from library?",
            message = "“${s.book.title}” leaves your library and its reading position is forgotten. The file on your phone is not deleted.",
            confirmLabel = "Remove",
            destructive = true,
            icon = Icons.Filled.Delete,
            onConfirm = { onRemove(s.book.id) },
            onDismiss = { sheet = null },
        )
        is Sheet.ShelfActions -> {
            val shelf = s.shelf
            MelodiqSheet(onDismiss = { sheet = null }) { dismiss ->
                SheetHeader(title = shelf?.name ?: "My Books")
                Spacer(Modifier.height(10.dp))
                SheetActionRow(icon = Icons.Filled.Add, title = "Add books to this shelf") {
                    onAdd(shelf?.id); dismiss()
                }
                if (shelf != null) {
                    SheetActionRow(icon = Icons.Filled.Edit, title = "Edit shelf details", subtitle = "Name and colour") {
                        sheet = Sheet.EditShelf(shelf)
                    }
                    SheetActionRow(icon = Icons.Filled.Delete, title = "Delete shelf", subtitle = "Its books stay in your library", destructive = true) {
                        sheet = Sheet.DeleteShelf(shelf)
                    }
                }
            }
        }
        is Sheet.EditShelf -> ShelfEditorSheet(
            title = if (s.shelf == null) "New shelf" else "Edit shelf",
            confirmLabel = if (s.shelf == null) "Create shelf" else "Save",
            initialName = s.shelf?.name.orEmpty(),
            initialColor = s.shelf?.colorIndex ?: ((shelves.size + 1) % ShelfPalette.size),
            onConfirm = { name, color ->
                if (s.shelf == null) onCreateShelf(name, color) else onUpdateShelf(s.shelf.id, name, color)
            },
            onDismiss = { sheet = null },
        )
        is Sheet.DeleteShelf -> MelodiqConfirmSheet(
            title = "Delete shelf?",
            message = "“${s.shelf.name}” will be removed. Its books are not deleted; they move back to My Books.",
            confirmLabel = "Delete shelf",
            destructive = true,
            icon = Icons.Filled.Delete,
            onConfirm = { onDeleteShelf(s.shelf.id) },
            onDismiss = { sheet = null },
        )
    }
}

// ---- shelf card ------------------------------------------------------------------------------

/**
 * One shelf: up to the five latest covers fanned out (the newest in the middle), a glass bar laid
 * over their lower half with the shelf's "+" (add books here) and "…" (shelf menu) buttons, and the
 * shelf's name and book count underneath.
 */
@Composable
private fun ShelfCard(
    modifier: Modifier = Modifier,
    name: String,
    color: Color,
    books: List<Book>,
    covers: BookCovers,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onMenu: () -> Unit,
) {
    val latest = remember(books) { books.sortedByDescending { it.addedAt }.take(MAX_COVERS) }
    // Newest in the centre, the others alternating to its right and left.
    val arranged = remember(latest) {
        val n = latest.size
        val slots = arrayOfNulls<Book>(n)
        val c = (n - 1) / 2
        latest.forEachIndexed { k, book ->
            val offset = (k + 1) / 2
            slots[if (k == 0) c else if (k % 2 == 1) c + offset else c - offset] = book
        }
        slots.filterNotNull()
    }

    Column(modifier.fillMaxWidth().padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(178.dp)
                .pressable(onClick = onOpen),
        ) {
            val coverH = 140.dp
            val coverW = coverH * 0.68f
            val slotsCount = if (arranged.isEmpty()) 3 else arranged.size
            // Same margin on both sides, so the fan sits exactly in the middle of the card.
            val area = maxWidth - 40.dp
            val step = if (slotsCount > 1) min(coverW.value * 0.62f, (area - coverW).value / (slotsCount - 1)).dp else 0.dp
            val total = coverW + step * (slotsCount - 1)
            val startX = (maxWidth - total) / 2
            val middleX = (maxWidth - coverW) / 2
            val centre = (slotsCount - 1) / 2f

            for (i in 0 until slotsCount) {
                val dist = abs(i - centre)
                val scale = 1f - 0.07f * dist
                val w = coverW * scale
                val h = coverH * scale
                val book = arranged.getOrNull(i)
                key(i) {
                    FanCover(
                        book = book,
                        covers = covers,
                        targetX = startX + step * i + (coverW - w) / 2,
                        middleX = middleX + (coverW - w) / 2,
                        width = w,
                        height = h,
                        tilt = (i - centre) * 3.5f,
                        z = 10f - dist * 2f,
                        order = i,
                    )
                }
            }

            // The shelf: coloured glass over the lower part of the covers.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(20f)
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.50f), color.copy(alpha = 0.78f))))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "•",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = color.copy(alpha = 1f),
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GlassButton(Icons.Filled.Add, "Add books to $name", onAdd)
                    GlassButton(Icons.Filled.MoreHoriz, "Shelf options", onMenu)
                }
            }
        }
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (books.size == 1) "1 book" else "${books.size} books",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One cover of a shelf's fan: springs out from the middle into its place, tilting slightly. */
@Composable
private fun BoxScope.FanCover(
    book: Book?,
    covers: BookCovers,
    targetX: Dp,
    middleX: Dp,
    width: Dp,
    height: Dp,
    tilt: Float,
    z: Float,
    order: Int,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((order * 70L).milliseconds)
        shown = true
    }
    val x by animateDpAsState(
        if (shown) targetX else middleX,
        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "fanX",
    )
    val rotation by animateFloatAsState(
        if (shown) tilt else 0f,
        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "fanTilt",
    )
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(300), label = "fanAlpha")
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = x, y = (-12).dp)
            .zIndex(z)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
            }
            .size(width, height)
            .shadow(10.dp, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp)),
    ) {
        if (book != null) {
            BookCover(book = book, covers = covers, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
            )
        }
    }
}

/** Fades and rises into place the first time it is shown, after [delayMillis]. */
@Composable
private fun Modifier.entrance(delayMillis: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(420, delayMillis, FastOutSlowInEasing)) }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36.dp.toPx()
    }
}

/** Tap / long-press with a springy shrink while the finger is down. */
@Composable
private fun Modifier.pressable(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { click() },
                onLongPress = { longClick?.invoke() },
            )
        }
}

@Composable
private fun GlassButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = Color(0xFF2B2B2E), modifier = Modifier.size(19.dp))
    }
}

// ---- one shelf opened ------------------------------------------------------------------------

@Composable
private fun ShelfPage(
    name: String,
    color: Color,
    books: List<Book>,
    progress: Map<String, Progress>,
    covers: BookCovers,
    bottomPad: Dp,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onMenu: () -> Unit,
    onOpen: (String) -> Unit,
    onBookMenu: (Book) -> Unit,
) {
    val sorted = remember(books) { books.sortedByDescending { it.addedAt } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomPad),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground) }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        name, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (books.size == 1) "1 book" else "${books.size} books",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                GlassIconOnSurface(Icons.Filled.Add, "Add books", onAdd)
                Spacer(Modifier.width(8.dp))
                GlassIconOnSurface(Icons.Filled.MoreHoriz, "Shelf options", onMenu)
            }
            Spacer(Modifier.height(16.dp))
        }
        if (sorted.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("This shelf is empty", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "Tap + to add books to it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                    )
                    Button(onClick = onAdd) { Text("Add Book") }
                }
            }
        } else {
            items(sorted.chunked(3), key = { row -> "grid-${row.first().id}" }) { row ->
                BookRow(row, progress, covers, onOpen, Modifier.animateItem(), onBookMenu)
            }
        }
    }
}

@Composable
private fun GlassIconOnSurface(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun BookRow(
    row: List<Book>,
    progress: Map<String, Progress>,
    covers: BookCovers,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (Book) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        row.forEachIndexed { slot, book ->
            BookTile(
                book = book,
                fraction = readFraction(book, progress[book.id]),
                covers = covers,
                onOpen = { onOpen(book.id) },
                onMenu = { onMenu(book) },
                modifier = Modifier.weight(1f).entrance(slot * 60),
            )
        }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

private fun readFraction(book: Book, p: Progress?): Float =
    if (p == null || book.pages <= 0) 0f else ((p.page + p.permille / 1000f) / book.pages).coerceIn(0f, 1f)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BookTile(
    book: Book,
    fraction: Float,
    covers: BookCovers,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(
            modifier = Modifier
                .pressable(onClick = onOpen, onLongClick = onMenu)
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
        ) {
            BookCover(book = book, covers = covers, modifier = Modifier.fillMaxSize())
            if (book.favourite) {
                Icon(
                    Icons.Filled.Favorite, "Favourite",
                    tint = Color(0xFFE5365B),
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp),
                )
            }
            if (fraction > 0f) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Black.copy(alpha = 0.35f),
                )
            }
        }
        Text(
            text = book.title,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// ---- sheets' pieces --------------------------------------------------------------------------

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    destructive: Boolean = false,
    checked: Boolean = false,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val content = if (destructive) scheme.error else scheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: if (destructive) scheme.error else scheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = content)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = scheme.onSurfaceVariant)
        }
        if (checked) Icon(Icons.Filled.Check, "Selected", tint = scheme.primary)
    }
}

/** Name + colour of a shelf; used for both creating and editing. */
@Composable
private fun ShelfEditorSheet(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialColor: Int,
    onConfirm: (name: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor) }
    MelodiqSheet(onDismiss = onDismiss) { dismiss ->
        SheetHeader(title = title, message = "Give the shelf a name and a colour.", icon = Icons.Filled.CollectionsBookmark)
        Spacer(Modifier.height(18.dp))
        val submit = {
            if (name.isNotBlank()) {
                onConfirm(name.trim(), color)
                dismiss()
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(32) },
            label = { Text("Shelf name") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Colour", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShelfPalette.forEachIndexed { i, c ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (i == color) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .clickable { color = i },
                    contentAlignment = Alignment.Center,
                ) {
                    if (i == color) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SheetPrimaryButton(text = confirmLabel, enabled = name.isNotBlank(), onClick = { submit() })
        TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

// ---- shared pieces ---------------------------------------------------------------------------

/** The floating tab switch: a raised capsule with a pill that springs between the tabs. */
@Composable
private fun FloatingTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val tabWidth = maxWidth / labels.size
        val offset by animateDpAsState(
            tabWidth * selected,
            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
            label = "tabOffset",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .offset(x = offset)
                    .width(tabWidth)
                    .height(48.dp)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Row(Modifier.fillMaxSize()) {
                labels.forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = if (i == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** The book's cover (from its metadata / first page), or a coloured title card while it loads or if there is none. */
@Composable
internal fun BookCover(book: Book, covers: BookCovers, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(null, book.id) { value = covers.load(book) }
    val colors = CoverFallbacks[(book.id.hashCode() and Int.MAX_VALUE) % CoverFallbacks.size]
    Box(modifier.background(Brush.verticalGradient(colors)), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = book.title,
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(favouritesTab: Boolean, readingTab: Boolean, onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when {
                favouritesTab -> "No favourite books yet"
                readingTab -> "Nothing in progress"
                else -> "No books yet"
            },
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = when {
                favouritesTab -> "Long-press a book or open it to add it here."
                readingTab -> "Books you have started reading or listening to show up here."
                else -> "Add a book (PDF or EPUB) from your phone to start your library. The file stays where it is."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
        )
        if (!favouritesTab && !readingTab) {
            Button(onClick = onAdd) { Text("Add Book") }
        }
    }
}

@Composable
private fun RoundAction(onClick: () -> Unit, tint: Color, content: @Composable (Color) -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content(tint) }
}