package com.tasnimulhasan.featureplayer

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
internal fun PlayerRoute(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    PlayerScreen(
        modifier
    )
}

@Composable
internal fun PlayerScreen(modifier: Modifier) {
    Box (modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Player",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    PlayerScreen(modifier = Modifier)
}