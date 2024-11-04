package com.tasnimulhasan.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun HomeRoute(
    modifier: Modifier = Modifier,
    navigateToPlayer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    HomeScreen(
        modifier,
        navigateToPlayer
    )
}

@Composable
internal fun HomeScreen(
    modifier: Modifier,
    navigateToPlayer: () -> Unit
) {
    Box (
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "Home",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(modifier = Modifier) {}
}