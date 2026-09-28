package com.tasnimulhasan.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasnimulhasan.domain.localusecase.favourite.ObserveFavouriteIdsUseCase
import com.tasnimulhasan.domain.localusecase.favourite.ToggleFavouriteUseCase
import com.tasnimulhasan.domain.localusecase.library.LibraryUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.entity.home.MusicEntity
import com.tasnimulhasan.entity.room.library.FolderSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One level of the folder tree currently being browsed: the songs directly inside it, and
 * the immediate child folders (derived from every folder path that starts with this one but
 * has exactly one more path segment). */
data class FolderLevel(
    val path: String,
    val displayName: String,
    val childFolders: List<FolderSummary>,
    val songs: List<MusicEntity>,
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
) : ViewModel() {

    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    // "" represents the library root. Pushing/popping this stack IS the folder navigation -
    // no extra nav-graph routes needed for an arbitrarily deep, user-defined tree.
    private val pathStack = MutableStateFlow(listOf(""))

    private val _allFolders = MutableStateFlow<List<FolderSummary>>(emptyList())

    private val _currentLevel = MutableStateFlow(FolderLevel("", "Music", emptyList(), emptyList()))
    val currentLevel: StateFlow<FolderLevel> = _currentLevel.asStateFlow()

    val canGoBack: StateFlow<Boolean> = pathStack
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            libraryUseCases.observeFolders().collectLatest { folders ->
                _allFolders.value = folders
                loadCurrentLevel()
            }
        }
    }

    fun open(folderPath: String) {
        pathStack.value = pathStack.value + folderPath
        viewModelScope.launch { loadCurrentLevel() }
    }

    fun back(): Boolean {
        if (pathStack.value.size <= 1) return false
        pathStack.value = pathStack.value.dropLast(1)
        viewModelScope.launch { loadCurrentLevel() }
        return true
    }

    /** Plays every song under the current folder (including subfolders) as its own queue -
     * "play this whole directory", distinct from tapping one song inside it. Uses the same
     * curated-queue path playlists use, so it doesn't disturb the main library queue
     * Home/Songs render from. */
    fun playAllInCurrentFolder() {
        viewModelScope.launch {
            val path = pathStack.value.last()
            val songs = libraryUseCases.getSongsUnderFolder(path)
            if (songs.isNotEmpty()) {
                playback.ensurePlaybackServiceStarted()
                playerUseCases.playCuratedQueue(songs, 0)
            }
        }
    }

    private suspend fun loadCurrentLevel() {
        val current = pathStack.value.last()
        val depth = if (current.isEmpty()) 0 else current.count { it == '/' } + 1

        val childFolders = _allFolders.value.filter { folder ->
            val matchesPrefix = if (current.isEmpty()) true else {
                folder.folderPath == current || folder.folderPath.startsWith("$current/")
            }
            if (!matchesPrefix || folder.folderPath == current) return@filter false
            val folderDepth = folder.folderPath.count { it == '/' } + 1
            folderDepth == depth + 1
        }

        val songs = if (current.isEmpty()) emptyList() else libraryUseCases.getSongsInFolder(current, limit = 500, offset = 0)

        _currentLevel.value = FolderLevel(
            path = current,
            displayName = current.substringAfterLast('/').ifBlank { "Music" },
            childFolders = childFolders,
            songs = songs,
        )
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    playerUseCases: PlayerUseCases,
    toggleFavouriteUseCase: ToggleFavouriteUseCase,
    observeFavouriteIdsUseCase: ObserveFavouriteIdsUseCase,
) : ViewModel() {

    val playback = LibraryPlaybackState(playerUseCases, toggleFavouriteUseCase, observeFavouriteIdsUseCase, viewModelScope)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<MusicEntity>>(emptyList())
    val results: StateFlow<List<MusicEntity>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                // Small debounce so fast typing doesn't fire a query per keystroke - short
                // enough that the search still feels instant against the local database.
                // (No distinctUntilChanged needed: StateFlow already only emits distinct
                // consecutive values on its own.)
                .debounce(150)
                .collectLatest { q ->
                    if (q.isBlank()) {
                        _results.value = emptyList()
                        _isSearching.value = false
                        return@collectLatest
                    }
                    _isSearching.value = true
                    _results.value = libraryUseCases.searchLibrary(q, limit = 100)
                    _isSearching.value = false
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }
}
