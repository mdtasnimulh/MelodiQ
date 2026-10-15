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
) : ViewModel() {

    val books: StateFlow<List<Book>> = repository.books

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun addBooks(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var added = 0
            var duplicates = 0
            var failed = 0
            uris.forEach {
                when (repository.add(it)) {
                    AddResult.ADDED -> added++
                    AddResult.DUPLICATE -> duplicates++
                    AddResult.FAILED -> failed++
                }
            }
            _messages.send(
                when {
                    added > 0 -> if (added == 1) "Book added" else "$added books added"
                    duplicates > 0 -> "Already in your library"
                    else -> "Couldn't open that PDF"
                } + if (failed > 0 && added > 0) " ($failed couldn't be opened)" else ""
            )
        }
    }

    fun toggleFavourite(id: String) = repository.toggleFavourite(id)

    fun remove(id: String) {
        repository.remove(id)
        viewModelScope.launch { _messages.send("Removed from library") }
    }

    fun comingSoon() {
        viewModelScope.launch { _messages.send("Audio narration is coming soon") }
    }
}
