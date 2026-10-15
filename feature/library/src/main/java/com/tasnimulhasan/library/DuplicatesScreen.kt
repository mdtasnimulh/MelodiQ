package com.tasnimulhasan.library

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.library.DuplicateFinderUseCases
import com.tasnimulhasan.entity.metadata.FileOpResult
import com.tasnimulhasan.entity.room.library.DuplicateGroup
import com.tasnimulhasan.entity.room.library.DuplicateSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class DuplicatesUiState(
    val loaded: Boolean = false,
    val groups: List<DuplicateGroup> = emptyList(),
    /** Song ids currently ticked for deletion. */
    val marked: Set<Long> = emptySet(),
) {
    val markedSongs: List<DuplicateSong>
        get() = groups.flatMap { it.songs }.filter { it.song.songId in marked }
    val markedBytes: Long get() = markedSongs.sumOf { it.sizeBytes }
}

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val useCases: DuplicateFinderUseCases,
) : ViewModel() {

    /** Songs the user flipped away from the default (default = keep the first/largest, mark the rest). */
    private val toggled = MutableStateFlow<Set<Long>>(emptySet())
    private val _pendingConsent = MutableStateFlow<IntentSender?>(null)
    val pendingConsent: StateFlow<IntentSender?> = _pendingConsent.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val state: StateFlow<DuplicatesUiState> = combine(useCases.groups(), toggled) { groups, flipped ->
        val marked = buildSet {
            groups.forEach { g ->
                g.songs.forEachIndexed { index, s ->
                    val default = index > 0
                    if (default != (s.song.songId in flipped)) add(s.song.songId)
                }
            }
        }
        DuplicatesUiState(loaded = true, groups = groups, marked = marked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DuplicatesUiState())

    private var pendingIds: List<Long> = emptyList()

    /** Ticks / unticks one copy, but never lets a group end up with every copy ticked. */
    fun toggle(group: DuplicateGroup, songId: Long) {
        val current = state.value.marked
        val wouldMark = songId !in current
        if (wouldMark && group.songs.count { it.song.songId !in current } <= 1) {
            _message.value = "Keep at least one copy of each song"
            return
        }
        toggled.update { if (songId in it) it - songId else it + songId }
    }

    fun deleteMarked() {
        val ids = state.value.markedSongs.map { it.song.songId }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            when (val result = useCases.delete(ids)) {
                is FileOpResult.Success -> done(ids.size)
                is FileOpResult.NeedsPermission -> {
                    pendingIds = ids
                    _pendingConsent.value = result.intentSender
                }
                is FileOpResult.Error -> _message.value = result.message
                else -> _message.value = "Couldn't delete those files"
            }
        }
    }

    /** The system performs the delete itself once the user approves, so nothing more to delete here. */
    fun onConsentResult(approved: Boolean) {
        _pendingConsent.value = null
        if (approved) {
            val n = pendingIds.size
            viewModelScope.launch { useCases.notifyChanged() }
            done(n)
        }
        pendingIds = emptyList()
    }

    fun clearMessage() { _message.value = null }

    private fun done(count: Int) {
        toggled.value = emptySet()
        _message.value = if (count == 1) "Deleted 1 duplicate" else "Deleted $count duplicates"
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.2f GB", mb / 1024) else String.format(Locale.US, "%.1f MB", mb)
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

@Composable
internal fun DuplicatesRoute(
    modifier: Modifier = Modifier,
    viewModel: DuplicatesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingConsent by viewModel.pendingConsent.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(pendingConsent) {
        val sender = pendingConsent ?: return@LaunchedEffect
        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, m, android.widget.Toast.LENGTH_SHORT).show()
        viewModel.clearMessage()
    }

    Box(modifier.fillMaxSize()) {
        when {
            !state.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.groups.isEmpty() -> Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No duplicates found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Songs count as duplicates when the title, artist and length all match.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.marked.isEmpty()) 24.dp else 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "${state.groups.size} songs have more than one copy. The largest file in each group is kept by default - tick the copies you want to delete.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.groups, key = { g -> g.songs.first().song.songId }) { group ->
                    DuplicateGroupCard(group, state.marked) { id -> viewModel.toggle(group, id) }
                }
            }
        }

        if (state.marked.isNotEmpty()) {
            Button(
                onClick = { confirm = true },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            ) {
                Text("Delete ${state.marked.size} duplicate${if (state.marked.size == 1) "" else "s"} (${formatSize(state.markedBytes)})")
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete ${state.marked.size} files?") },
            text = { Text("This permanently removes ${formatSize(state.markedBytes)} from your device. Android will ask you to confirm.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; viewModel.deleteMarked() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DuplicateGroupCard(group: DuplicateGroup, marked: Set<Long>, onToggle: (Long) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(
                group.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                group.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(4.dp))
            group.songs.forEachIndexed { index, s ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = s.song.songId in marked, onCheckedChange = { onToggle(s.song.songId) })
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.folderPath.ifBlank { "Unknown folder" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            buildString {
                                append(formatSize(s.sizeBytes)).append(" · ").append(formatDuration(s.durationMs))
                                if (index == 0) append(" · largest")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
