package com.tasnimulhasan.melodiq.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable object AudioBookRoute

fun NavController.navigateToAudioBook(navOptions: NavOptions? = null) = navigate(route = AudioBookRoute, navOptions)

/** Placeholder: the audio book feature itself comes later. */
fun NavGraphBuilder.audioBookScreen() {
    composable<AudioBookRoute> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Audio Book Screen", style = MaterialTheme.typography.titleLarge)
        }
    }
}
