package com.tasnimulhasan.featureaudiobook

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AudioBookViewModel @Inject constructor(
    private val repository: AudioBookRepository,
    val covers: BookCovers,
    private val speaker: BookSpeaker,
) : ViewModel() {

    val books: StateFlow<List<Book>> = repository.books
    val shelves: StateFlow<List<Shelf>> = repository.shelves
    val progress: StateFlow<Map<String, Progress>> = repository.progress

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun addBooks(uris: List<Uri>, shelfId: String? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var added = 0
            var duplicates = 0
            var failed = 0
            uris.forEach {
                when (repository.add(it, shelfId)) {
                    AddResult.ADDED -> added++
                    AddResult.DUPLICATE -> duplicates++
                    AddResult.FAILED, AddResult.UNSUPPORTED -> failed++
                }
            }
            _messages.send(
                when {
                    added > 0 -> if (added == 1) "Book added" else "$added books added"
                    duplicates > 0 -> "Already in your library"
                    else -> "Couldn't open that file. Choose a PDF or EPUB book"
                } + if (failed > 0 && added > 0) " ($failed couldn't be opened)" else ""
            )
        }
    }

    fun toggleFavourite(id: String) = repository.toggleFavourite(id)

    fun createShelf(name: String, colorIndex: Int) {
        repository.createShelf(name, colorIndex)
    }

    fun updateShelf(id: String, name: String, colorIndex: Int) = repository.updateShelf(id, name, colorIndex)

    fun deleteShelf(id: String) {
        repository.deleteShelf(id)
        viewModelScope.launch { _messages.send("Shelf deleted. Its books are back on My Books") }
    }

    fun moveToShelf(bookId: String, shelfId: String?) = repository.moveToShelf(bookId, shelfId)

    fun remove(id: String) {
        speaker.onBookRemoved(id)
        repository.remove(id)
        viewModelScope.launch { _messages.send("Removed from library") }
    }
}
