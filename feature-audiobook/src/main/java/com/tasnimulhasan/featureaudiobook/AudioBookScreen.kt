package com.tasnimulhasan.featureaudiobook

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.designsystem.component.Icon
import com.tasnimulhasan.designsystem.component.LocalFloatingBarPadding
import kotlin.math.roundToInt

/** Soft glass-shelf colours, cycled shelf by shelf. */
private val ShelfColors = listOf(
    Color(0xFFF4A94E), Color(0xFF4DA3FF), Color(0xFFFF9EB5), Color(0xFFC79BFF), Color(0xFF6FD6A3),
)

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
    viewModel: AudioBookViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addBooks(uris)
    }
    AudioBookScreen(
        books = books,
        covers = viewModel.covers,
        onAdd = { picker.launch(arrayOf("application/pdf")) },
        onOpen = navigateToDetails,
        onToggleFavourite = viewModel::toggleFavourite,
        onRemove = viewModel::remove,
    )
}

@Composable
internal fun AudioBookScreen(
    books: List<Book>,
    covers: BookCovers,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val favourites = remember(books) { books.filter { it.favourite } }
    val shown = if (tab == 0) books else favourites

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp,
            bottom = LocalFloatingBarPadding.current + 24.dp,
        ),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "My Books",
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
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, "Add PDF", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }

        item {
            FloatingTabs(
                labels = listOf("All (${books.size})", "Favourites (${favourites.size})"),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(vertical = 18.dp),
            )
        }

        if (shown.isEmpty()) {
            item { EmptyState(favouritesTab = tab == 1, onAdd = onAdd) }
        } else {
            itemsIndexed(shown.chunked(3), key = { _, row -> row.first().id }) { index, row ->
                Shelf(
                    label = if (index == 0) (if (tab == 0) "Recently added" else "Your favourites") else null,
                    books = row,
                    color = ShelfColors[index % ShelfColors.size],
                    covers = covers,
                    onOpen = onOpen,
                    onToggleFavourite = onToggleFavourite,
                    onRemove = onRemove,
                )
            }
        }
    }
}

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
                            fontSize = 15.sp,
                            color = if (i == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Up to three covers standing on a translucent glass shelf. */
@Composable
private fun Shelf(
    label: String?,
    books: List<Book>,
    color: Color,
    covers: BookCovers,
    onOpen: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(Modifier.padding(bottom = 22.dp)) {
        if (label != null) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                books.forEach { book ->
                    BookTile(
                        book = book,
                        covers = covers,
                        onOpen = { onOpen(book.id) },
                        onToggleFavourite = { onToggleFavourite(book.id) },
                        onRemove = { onRemove(book.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - books.size) { Spacer(Modifier.weight(1f)) }
            }
            // The shelf: translucent coloured glass laid over the bottom of the covers.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.85f)))
                    )
                    .border(0.8.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(4.dp)),
            ) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
                Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BookTile(
    book: Book,
    covers: BookCovers,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .aspectRatio(0.68f)
            .shadow(8.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onOpen, onLongClick = { menu = true }),
    ) {
        BookCover(book = book, covers = covers, modifier = Modifier.fillMaxSize())
        if (book.favourite) {
            Icon(
                Icons.Filled.Favorite, "Favourite",
                tint = Color(0xFFE5365B),
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (book.favourite) "Remove from favourites" else "Add to favourites") },
                leadingIcon = {
                    Icon(if (book.favourite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite, null)
                },
                onClick = { menu = false; onToggleFavourite() },
            )
            DropdownMenuItem(
                text = { Text("Remove from library") },
                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = { menu = false; onRemove() },
            )
        }
    }
}

/** First page of the PDF, or a coloured title card while it loads (or if it can't be read). */
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
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(favouritesTab: Boolean, onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (favouritesTab) "No favourite books yet" else "No books yet",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (favouritesTab) "Long-press a book or open it to add it here."
            else "Add a PDF from your phone to start your library. The file stays where it is.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
        )
        if (!favouritesTab) {
            Button(onClick = onAdd) { Text("Add PDF") }
        }
    }
}
