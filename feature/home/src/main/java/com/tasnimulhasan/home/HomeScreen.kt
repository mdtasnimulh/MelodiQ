package com.tasnimulhasan.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            onClick = { navigateToPlayer.invoke() },
        ) {
            Icon(Icons.Filled.Add, "Floating action button.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(modifier = Modifier) {}
}