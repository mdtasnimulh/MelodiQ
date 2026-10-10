package com.tasnimulhasan.songdetails

import android.content.IntentSender
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.metadata.AudioFileInfo
import com.tasnimulhasan.entity.metadata.EditableMetadata
import com.tasnimulhasan.entity.metadata.TagWriteSupport
import kotlinx.coroutines.launch
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.tasnimulhasan.ui.image.AlbumArt
import com.tasnimulhasan.ui.image.AlbumArtVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Decodes the picked image, scales it down (long edge <= 1000px, keeps tags small) and
 * re-encodes as JPEG - the tag writer labels embedded art image/jpeg. Null if undecodable. */
private fun prepareCover(context: android.content.Context, uri: android.net.Uri): Pair<ByteArray, Bitmap>? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1000) sample *= 2
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
    decoded?.let { bmp ->
        val scale = 1000f / maxOf(bmp.width, bmp.height)
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true) else bmp
        val out = ByteArrayOutputStream()
        // JPEG has no alpha; the Bitmap is flattened on white by compress() only if opaque - fine for covers.
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.toByteArray() to scaled
    }
} catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }

@Composable
fun SongDetailsRoute(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: SongDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingConsent by viewModel.pendingConsent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onConsentResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(pendingConsent) {
        val sender: IntentSender = pendingConsent ?: return@LaunchedEffect
        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onDeleted()
    }

    SongDetailsScreen(
        modifier = modifier,
        uiState = uiState,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onRename = viewModel::rename,
        onMove = viewModel::move,
        onShare = {
            coroutineScope.launch {
                val uri = viewModel.shareUri() ?: return@launch
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share song"))
            }
        },
        onMessageShown = viewModel::clearMessage,
    )
}

@Composable
internal fun SongDetailsScreen(
    modifier: Modifier = Modifier,
    uiState: SongDetailsUiState,
    onSave: (EditableMetadata) -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onMove: (String) -> Unit,
    onShare: () -> Unit,
    onMessageShown: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    var title by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.title.orEmpty()) }
    var artist by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.artist.orEmpty()) }
    var album by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.album.orEmpty()) }
    var albumArtist by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.albumArtist.orEmpty()) }
    var genre by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.genre.orEmpty()) }
    var year by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.year.orEmpty()) }
    var trackNumber by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.trackNumber.orEmpty()) }
    var discNumber by remember(uiState.metadata) { mutableStateOf(uiState.metadata?.discNumber.orEmpty()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingArt by remember(uiState.metadata) { mutableStateOf<ByteArray?>(null) }
    // Not keyed on metadata: after a successful save the picked image keeps showing even
    // before MediaStore regenerates its thumbnail.
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val artLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val prepared = withContext(Dispatchers.IO) { prepareCover(context, uri) }
            if (prepared != null) {
                pendingArt = prepared.first
                previewBitmap = prepared.second
            } else {
                android.widget.Toast.makeText(context, "Couldn't read that image", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val artVersion = remember(uiState.metadata) { AlbumArtVersion.value }

    val original = uiState.metadata
    val editable = uiState.writeSupport != TagWriteSupport.UNSUPPORTED
    val isDirty = original != null && (
        title != original.title.orEmpty() || artist != original.artist.orEmpty() ||
            album != original.album.orEmpty() || albumArtist != original.albumArtist.orEmpty() ||
            genre != original.genre.orEmpty() || year != original.year.orEmpty() ||
            trackNumber != original.trackNumber.orEmpty() || discNumber != original.discNumber.orEmpty()
        ) || pendingArt != null
    fun resetFields() {
        title = original?.title.orEmpty(); artist = original?.artist.orEmpty()
        album = original?.album.orEmpty(); albumArtist = original?.albumArtist.orEmpty()
        genre = original?.genre.orEmpty(); year = original?.year.orEmpty()
        trackNumber = original?.trackNumber.orEmpty(); discNumber = original?.discNumber.orEmpty()
        pendingArt = null; previewBitmap = null
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (isDirty) 96.dp else 16.dp)
            .animateContentSize()
    ) {
        // Header: shows the title/artist as they are being edited, so it is obvious which
        // song this is and that typing is changing something.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .then(if (editable) Modifier.clickable {
                            artLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title.trim().firstOrNull()?.uppercase() ?: "♪",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    val preview = previewBitmap
                    if (preview != null) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = "Cover art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (uiState.songId > 0) {
                        key(artVersion) {
                            AsyncImage(
                                model = AlbumArt(
                                    songId = uiState.songId,
                                    contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, uiState.songId),
                                    albumId = 0L,
                                ),
                                contentDescription = "Cover art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                if (editable) {
                    TextButton(onClick = {
                        artLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Change cover") }
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(artist, album).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Unknown artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                uiState.fileInfo?.let {
                    Text(
                        text = it.format.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (uiState.writeSupport == TagWriteSupport.UNSUPPORTED) {
            Text(
                text = "This file's format doesn't support editing tags in-app yet. You can still view its info and manage the file below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Basic info", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                LabeledField("Title", title, editable) { title = it }
                LabeledField("Artist", artist, editable) { artist = it }
                LabeledField("Album", album, editable) { album = it }
                LabeledField("Album Artist", albumArtist, editable) { albumArtist = it }
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
        androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Details", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                LabeledField("Genre", genre, editable) { genre = it }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField("Year", year, editable, modifier = Modifier.weight(1f), numeric = true, maxLength = 4) { year = it }
                    LabeledField("Track #", trackNumber, editable, modifier = Modifier.weight(1f), numeric = true, maxLength = 3) { trackNumber = it }
                    LabeledField("Disc #", discNumber, editable, modifier = Modifier.weight(1f), numeric = true, maxLength = 2) { discNumber = it }
                }
            }
        }

        uiState.message?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2500)
                onMessageShown()
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        uiState.fileInfo?.let {
            androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(16.dp)) { FileInfoSection(it) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text(text = "File", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showRenameDialog = true }) { Text("Rename") }
            OutlinedButton(onClick = { showMoveDialog = true }) { Text("Move") }
            OutlinedButton(onClick = onShare) { Text("Share") }
        }
        TextButton(
            onClick = { showDeleteConfirm = true },
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Delete song")
        }
    }

    // Save bar slides up only when something changed, replacing the always-visible buttons.
    androidx.compose.animation.AnimatedVisibility(
        visible = isDirty && editable,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
    ) {
        androidx.compose.material3.Surface(tonalElevation = 6.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { resetFields() }, enabled = !uiState.isSaving) { Text("Discard") }
                Button(
                    onClick = {
                        onSave(
                            EditableMetadata(
                                title = title, artist = artist, album = album, albumArtist = albumArtist,
                                genre = genre, year = year, trackNumber = trackNumber, discNumber = discNumber,
                                newArtwork = pendingArt,
                            )
                        )
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                ) { Text(if (uiState.isSaving) "Saving..." else "Save changes") }
            }
        }
    }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this song?") },
            text = { Text("This permanently removes the file from your device.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename file") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { showRenameDialog = false; onRename(newName) }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMoveDialog) {
        var newFolder by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to folder") },
            text = {
                Column {
                    Text(
                        "Enter the destination folder path (e.g. Music/English/Rock)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(value = newFolder, onValueChange = { newFolder = it }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveDialog = false; onMove(newFolder) }) { Text("Move") }
            },
            dismissButton = { TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    maxLength: Int = Int.MAX_VALUE,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        // Year / track / disc only accept digits (and a sensible length) instead of letting
        // junk get written into the file's tags.
        onValueChange = { new ->
            if (!numeric || (new.all { it.isDigit() } && new.length <= maxLength)) onValueChange(new)
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (numeric) androidx.compose.ui.text.input.KeyboardType.Number
            else androidx.compose.ui.text.input.KeyboardType.Text
        ),
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun FileInfoSection(info: AudioFileInfo) {
    Column {
        Text(text = "File info", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        InfoRow("Format", info.format)
        info.bitrateKbps?.let { InfoRow("Bitrate", "$it kbps") }
        info.sampleRateHz?.let { InfoRow("Sample rate", "${it / 1000.0} kHz") }
        info.channels?.let { InfoRow("Channels", if (it == 1) "Mono" else if (it == 2) "Stereo" else "$it channels") }
        InfoRow("Duration", formatDuration(info.durationMs))
        InfoRow("Size", formatSize(info.sizeBytes))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}
