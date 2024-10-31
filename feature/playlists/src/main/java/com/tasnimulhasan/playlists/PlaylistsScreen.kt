package com.tasnimulhasan.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun PlaylistsRoute(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    PlaylistsScreen(
        modifier
    )
}

@Composable
internal fun PlaylistsScreen(modifier: Modifier) {
    Box (modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Playlists",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistsScreenPreview() {
    PlaylistsScreen(modifier = Modifier)
}