package com.tasnimulhasan.featureaudiobook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface ReaderUi {
    data object Loading : ReaderUi
    data class Ready(val book: Book) : ReaderUi
    data object Failed : ReaderUi
}

@HiltViewModel
internal class BookReaderViewModel @Inject constructor(
    private val repository: AudioBookRepository,
    val speaker: BookSpeaker,
) : ViewModel() {

    private val _ui = MutableStateFlow<ReaderUi>(ReaderUi.Loading)
    val ui: StateFlow<ReaderUi> = _ui.asStateFlow()

    private var started = false

    /** Opens the book (restoring its saved position). [autoPlay] starts narration once, e.g. from the Play button. */
    fun load(bookId: String, autoPlay: Boolean) {
        if (started) return
        started = true
        val book = repository.find(bookId)
        if (book == null) {
            _ui.value = ReaderUi.Failed
            return
        }
        viewModelScope.launch {
            if (speaker.load(book)) {
                _ui.value = ReaderUi.Ready(book)
                if (autoPlay) speaker.play()
            } else {
                _ui.value = ReaderUi.Failed
            }
        }
    }

    override fun onCleared() {
        // Leaving the reader while it is quiet ends the session; while it is playing it carries on in the background.
        if (!speaker.state.value.playing) speaker.stop()
    }
}
