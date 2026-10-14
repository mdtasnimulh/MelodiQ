package com.tasnimulhasan.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity

/**
 * Long-press menu for a song. Shared by Home and Songs (and anything else) so every list
 * offers the same actions. Shown while [song] is non-null; callers set it to null to hide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsSheet(
    song: MusicEntity?,
    onPlayNext: (MusicEntity) -> Unit,
    onPlayLater: (MusicEntity) -> Unit,
    onAddToPlaylist: (MusicEntity) -> Unit,
    onSongInfo: (MusicEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    if (song == null) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = song.songTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            SheetAction(Icons.Filled.QueuePlayNext, "Play next") { onPlayNext(song) }
            SheetAction(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { onPlayLater(song) }
            SheetAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { onAddToPlaylist(song) }
            SheetAction(Icons.Filled.Info, "Song info") { onSongInfo(song) }
        }
    }
}

/**
 * "Add to playlist" sheet. Always offers a "New playlist" row first, so it works even when
 * the user has no playlists yet; tapping it reveals a name field inline. Shown while [song]
 * is non-null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    song: MusicEntity?,
    playlists: List<PlaylistEntity>,
    onPlaylistSelected: (MusicEntity, PlaylistEntity) -> Unit,
    onCreatePlaylist: (MusicEntity, String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (song == null) return
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    fun submit() {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) onCreatePlaylist(song, trimmed)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = "Add to playlist",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            if (creating) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { creating = false; name = "" }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { submit() }, enabled = name.isNotBlank()) {
                            Text("Create & add")
                        }
                    }
                }
            } else {
                SheetAction(Icons.Filled.Add, "New playlist") { creating = true }
            }

            if (playlists.isEmpty()) {
                if (!creating) {
                    Text(
                        text = "You don't have any playlists yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        SheetAction(Icons.AutoMirrored.Filled.QueueMusic, playlist.playlistName) {
                            onPlaylistSelected(song, playlist)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
