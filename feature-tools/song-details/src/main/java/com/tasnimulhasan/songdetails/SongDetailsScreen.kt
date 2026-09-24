package com.tasnimulhasan.songdetails

import android.content.IntentSender
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (uiState.writeSupport == TagWriteSupport.UNSUPPORTED) {
            Text(
                text = "This file's format doesn't support editing tags in-app yet. You can still view its info and manage the file below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        val editable = uiState.writeSupport != TagWriteSupport.UNSUPPORTED
        LabeledField("Title", title, editable) { title = it }
        LabeledField("Artist", artist, editable) { artist = it }
        LabeledField("Album", album, editable) { album = it }
        LabeledField("Album Artist", albumArtist, editable) { albumArtist = it }
        LabeledField("Genre", genre, editable) { genre = it }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField("Year", year, editable, modifier = Modifier.weight(1f)) { year = it }
            LabeledField("Track #", trackNumber, editable, modifier = Modifier.weight(1f)) { trackNumber = it }
            LabeledField("Disc #", discNumber, editable, modifier = Modifier.weight(1f)) { discNumber = it }
        }

        if (editable) {
            Button(
                onClick = {
                    onSave(
                        EditableMetadata(
                            title = title, artist = artist, album = album, albumArtist = albumArtist,
                            genre = genre, year = year, trackNumber = trackNumber, discNumber = discNumber,
                        )
                    )
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save changes")
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

        uiState.fileInfo?.let { FileInfoSection(it) }

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
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
