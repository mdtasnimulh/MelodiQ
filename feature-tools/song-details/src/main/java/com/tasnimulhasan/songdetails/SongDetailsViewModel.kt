package com.tasnimulhasan.songdetails

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.metadata.DeleteSongUseCase
import com.tasnimulhasan.domain.localusecase.metadata.GetShareableUriUseCase
import com.tasnimulhasan.domain.localusecase.metadata.GetWriteSupportUseCase
import com.tasnimulhasan.domain.localusecase.metadata.MoveSongUseCase
import com.tasnimulhasan.domain.localusecase.metadata.NotifyLibraryChangedUseCase
import com.tasnimulhasan.domain.localusecase.metadata.ReadCurrentMetadataUseCase
import com.tasnimulhasan.domain.localusecase.metadata.ReadFileInfoUseCase
import com.tasnimulhasan.domain.localusecase.metadata.RenameSongUseCase
import com.tasnimulhasan.domain.localusecase.metadata.SaveMetadataUseCase
import com.tasnimulhasan.entity.metadata.AudioFileInfo
import com.tasnimulhasan.entity.metadata.EditableMetadata
import com.tasnimulhasan.entity.metadata.FileOpResult
import com.tasnimulhasan.entity.metadata.MetadataEditResult
import com.tasnimulhasan.entity.metadata.TagWriteSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What to retry after the user grants a scoped-storage consent request. Kept as an enum of
 * intents rather than a raw closure so state survives process death/recomposition cleanly. */
private sealed interface PendingRetry {
    data class Save(val metadata: EditableMetadata) : PendingRetry
    data object Delete : PendingRetry
    data class Rename(val newName: String) : PendingRetry
    data class Move(val newFolder: String) : PendingRetry
}

data class SongDetailsUiState(
    val isLoading: Boolean = true,
    val metadata: EditableMetadata? = null,
    val fileInfo: AudioFileInfo? = null,
    val writeSupport: TagWriteSupport = TagWriteSupport.UNSUPPORTED,
    val isSaving: Boolean = false,
    val message: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class SongDetailsViewModel @Inject constructor(
    private val readCurrentMetadata: ReadCurrentMetadataUseCase,
    private val readFileInfo: ReadFileInfoUseCase,
    private val getWriteSupport: GetWriteSupportUseCase,
    private val saveMetadata: SaveMetadataUseCase,
    private val deleteSong: DeleteSongUseCase,
    private val renameSong: RenameSongUseCase,
    private val moveSong: MoveSongUseCase,
    private val getShareableUri: GetShareableUriUseCase,
    private val notifyLibraryChanged: NotifyLibraryChangedUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val songId: Long = savedStateHandle.get<Long>("songId") ?: -1L

    private val _uiState = MutableStateFlow(SongDetailsUiState())
    val uiState: StateFlow<SongDetailsUiState> = _uiState.asStateFlow()

    private val _pendingConsent = MutableStateFlow<IntentSender?>(null)
    val pendingConsent: StateFlow<IntentSender?> = _pendingConsent.asStateFlow()

    private var pendingRetry: PendingRetry? = null

    init {
        viewModelScope.launch {
            val metadata = readCurrentMetadata(songId)
            val info = readFileInfo(songId)
            val support = getWriteSupport(songId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                metadata = metadata,
                fileInfo = info,
                writeSupport = support,
            )
        }
    }

    fun save(metadata: EditableMetadata) {
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        viewModelScope.launch {
            when (val result = saveMetadata(songId, metadata)) {
                MetadataEditResult.Success -> {
                    com.tasnimulhasan.ui.image.AlbumArtVersion.bump()
                    _uiState.value = _uiState.value.copy(
                    isSaving = false, metadata = metadata, message = "Saved"
                    )
                }
                is MetadataEditResult.NeedsPermission -> {
                    pendingRetry = PendingRetry.Save(metadata)
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _pendingConsent.value = result.intentSender
                }
                MetadataEditResult.UnsupportedFormat -> _uiState.value = _uiState.value.copy(
                    isSaving = false, message = "This file format can't be edited"
                )
                is MetadataEditResult.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false, message = result.message
                )
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            when (val result = deleteSong(songId)) {
                FileOpResult.Success -> _uiState.value = _uiState.value.copy(deleted = true)
                is FileOpResult.NeedsPermission -> {
                    pendingRetry = PendingRetry.Delete
                    _pendingConsent.value = result.intentSender
                }
                is FileOpResult.Error -> _uiState.value = _uiState.value.copy(message = result.message)
            }
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            when (val result = renameSong(songId, newName)) {
                FileOpResult.Success -> {
                    com.tasnimulhasan.ui.image.AlbumArtVersion.bump()
                    val current = _uiState.value.metadata
                    val baseName = newName.substringBeforeLast('.', newName)
                    if (current != null && _uiState.value.writeSupport != TagWriteSupport.UNSUPPORTED &&
                        current.title != baseName
                    ) {
                        // The list shows the TITLE tag, not the file name - rename both so the
                        // change is actually visible. The tag writer keeps the embedded cover.
                        save(current.copy(title = baseName, newArtwork = null))
                    } else {
                        _uiState.value = _uiState.value.copy(message = "Renamed")
                    }
                }
                is FileOpResult.NeedsPermission -> {
                    pendingRetry = PendingRetry.Rename(newName)
                    _pendingConsent.value = result.intentSender
                }
                is FileOpResult.Error -> _uiState.value = _uiState.value.copy(message = result.message)
            }
        }
    }

    fun move(newFolderRelativePath: String) {
        viewModelScope.launch {
            when (val result = moveSong(songId, newFolderRelativePath)) {
                FileOpResult.Success -> _uiState.value = _uiState.value.copy(message = "Moved")
                is FileOpResult.NeedsPermission -> {
                    pendingRetry = PendingRetry.Move(newFolderRelativePath)
                    _pendingConsent.value = result.intentSender
                }
                is FileOpResult.Error -> _uiState.value = _uiState.value.copy(message = result.message)
            }
        }
    }

    suspend fun shareUri() = getShareableUri(songId)

    /** Called by the screen after the user approves (or denies) the system consent dialog. */
    fun onConsentResult(granted: Boolean) {
        _pendingConsent.value = null
        val retry = pendingRetry
        pendingRetry = null
        if (!granted || retry == null) return

        when (retry) {
            is PendingRetry.Save -> save(retry.metadata)
            PendingRetry.Delete -> {
                // createDeleteRequest makes the SYSTEM perform the deletion once the user
                // approves - calling delete again here would find nothing left to delete,
                // fail, and re-prompt in a loop. Approval IS the success signal.
                viewModelScope.launch {
                    notifyLibraryChanged()
                    _uiState.value = _uiState.value.copy(deleted = true)
                }
            }
            is PendingRetry.Rename -> rename(retry.newName)
            is PendingRetry.Move -> move(retry.newFolder)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
